package com.codeeditor.android.build.tasks;

import com.codeeditor.android.build.BuildContext;
import com.codeeditor.android.build.BuildException;
import com.codeeditor.android.build.BuildTask;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

public class CompileJavaTask implements BuildTask {
    
    private AtomicBoolean cancelled = new AtomicBoolean(false);
    private int progress = 0;
    private String progressMessage = "Compiling Java sources...";
    private Process currentProcess;
    
    @Override
    public String getName() {
        return "Compile Java";
    }
    
    @Override
    public boolean execute(BuildContext context) throws BuildException {
        context.phaseStarted("Java Compilation");
        
        try {
            File sourceDir = context.getConfig().getSourceDir();
            File classesDir = context.getClassesDir();
            File androidJar = context.getAndroidJar();
            
            if (!sourceDir.exists()) {
                throw new BuildException("Compile", "Source directory not found: " + sourceDir);
            }
            
            classesDir.mkdirs();
            
            List<File> javaFiles = collectJavaFiles(sourceDir);
            
            File generatedDir = new File(context.getBuildDir(), "generated");
            if (generatedDir.exists()) {
                List<File> generatedFiles = collectJavaFiles(generatedDir);
                javaFiles.addAll(generatedFiles);
            }
            
            if (javaFiles.isEmpty()) {
                context.warning("No Java source files found");
                context.phaseCompleted("Java Compilation");
                return true;
            }
            
            context.log("Found " + javaFiles.size() + " Java source files");
            
            boolean success = false;
            
            context.log("Trying ECJ compiler...");
            success = compileWithECJ(context, javaFiles, classesDir, androidJar);
            
            if (!success) {
                context.log("ECJ not available, trying javac command...");
                success = compileWithJavacCommand(context, javaFiles, classesDir, androidJar);
            }
            
            if (success) {
                context.phaseCompleted("Java Compilation");
            }
            
            return success;
            
        } catch (Exception e) {
            throw new BuildException("Compile", "Java compilation failed: " + e.getMessage(), e);
        }
    }
    
    private List<File> collectJavaFiles(File dir) {
        List<File> files = new ArrayList<>();
        collectJavaFilesRecursive(dir, files);
        return files;
    }
    
    private void collectJavaFilesRecursive(File dir, List<File> files) {
        File[] children = dir.listFiles();
        if (children != null) {
            for (File child : children) {
                if (cancelled.get()) return;
                
                if (child.isDirectory()) {
                    collectJavaFilesRecursive(child, files);
                } else if (child.getName().endsWith(".java")) {
                    files.add(child);
                }
            }
        }
    }
    
    private boolean compileWithECJ(BuildContext context, List<File> javaFiles,
                                   File classesDir, File androidJar) {
        context.log("Attempting ECJ compilation...");
        
        try {
            Class<?> ecjMain = Class.forName("org.eclipse.jdt.internal.compiler.batch.Main");
            
            List<String> args = new ArrayList<>();
            args.add("-d");
            args.add(classesDir.getAbsolutePath());
            args.add("-source");
            args.add("11");
            args.add("-target");
            args.add("11");
            args.add("-encoding");
            args.add("UTF-8");
            args.add("-nowarn");
            args.add("-proceedOnError");
            
            if (androidJar != null && androidJar.exists() && androidJar.length() > 0) {
                args.add("-bootclasspath");
                args.add(androidJar.getAbsolutePath());
            }
            
            String depClasspath = context.getDependencyClasspath();
            if (depClasspath != null && !depClasspath.isEmpty()) {
                args.add("-classpath");
                args.add(depClasspath);
            }
            
            for (File javaFile : javaFiles) {
                args.add(javaFile.getAbsolutePath());
            }
            
            StringWriter outputWriter = new StringWriter();
            StringWriter errorWriter = new StringWriter();
            PrintWriter outPw = new PrintWriter(outputWriter);
            PrintWriter errPw = new PrintWriter(errorWriter);
            
            Object mainInstance = ecjMain.getConstructor(
                PrintWriter.class, PrintWriter.class, boolean.class)
                .newInstance(outPw, errPw, false);
            
            Method compileMethod = ecjMain.getMethod("compile", String[].class);
            boolean success = (Boolean) compileMethod.invoke(mainInstance, 
                (Object) args.toArray(new String[0]));
            
            String output = outputWriter.toString();
            String errors = errorWriter.toString();
            
            if (!output.isEmpty()) {
                context.log("ECJ output: " + output);
            }
            if (!errors.isEmpty()) {
                context.error("ECJ errors: " + errors);
            }
            
            int compiledCount = countClassFiles(classesDir);
            context.log("ECJ compiled " + compiledCount + " class files");
            
            return success || compiledCount > 0;
            
        } catch (ClassNotFoundException e) {
            context.log("ECJ not found in classpath");
            return false;
        } catch (Exception e) {
            context.error("ECJ compilation error: " + e.getMessage());
            return false;
        }
    }
    
    private boolean compileWithJavacCommand(BuildContext context, List<File> javaFiles,
                                            File classesDir, File androidJar) {
        context.log("Attempting javac command compilation...");
        
        try {
            File sourceList = new File(classesDir.getParentFile(), "sources.txt");
            try (FileOutputStream fos = new FileOutputStream(sourceList)) {
                for (File javaFile : javaFiles) {
                    fos.write((javaFile.getAbsolutePath() + "\n").getBytes());
                }
            }
            
            List<String> command = new ArrayList<>();
            command.add("javac");
            command.add("-d");
            command.add(classesDir.getAbsolutePath());
            command.add("-source");
            command.add("11");
            command.add("-target");
            command.add("11");
            command.add("-encoding");
            command.add("UTF-8");
            
            if (androidJar != null && androidJar.exists() && androidJar.length() > 0) {
                command.add("-bootclasspath");
                command.add(androidJar.getAbsolutePath());
            }
            
            command.add("@" + sourceList.getAbsolutePath());
            
            context.log("Executing: " + String.join(" ", command));
            
            ProcessBuilder pb = new ProcessBuilder(command);
            pb.redirectErrorStream(true);
            
            currentProcess = pb.start();
            
            StringBuilder output = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(currentProcess.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    output.append(line).append("\n");
                    context.log("javac: " + line);
                }
            }
            
            boolean finished = currentProcess.waitFor(300, TimeUnit.SECONDS);
            
            if (!finished) {
                currentProcess.destroyForcibly();
                context.error("javac timed out");
                return false;
            }
            
            int exitCode = currentProcess.exitValue();
            
            if (exitCode != 0) {
                context.error("javac failed with exit code " + exitCode + ": " + output);
                return false;
            }
            
            int compiledCount = countClassFiles(classesDir);
            context.log("javac compiled " + compiledCount + " class files");
            
            return compiledCount > 0;
            
        } catch (IOException | InterruptedException e) {
            context.error("javac command failed: " + e.getMessage());
            return false;
        }
    }
    
    private int countClassFiles(File dir) {
        int count = 0;
        File[] files = dir.listFiles();
        if (files != null) {
            for (File file : files) {
                if (file.isDirectory()) {
                    count += countClassFiles(file);
                } else if (file.getName().endsWith(".class")) {
                    count++;
                }
            }
        }
        return count;
    }
    
    @Override
    public void cancel() {
        cancelled.set(true);
        if (currentProcess != null) {
            currentProcess.destroyForcibly();
        }
    }
    
    @Override
    public int getProgress() {
        return progress;
    }
    
    @Override
    public String getProgressMessage() {
        return progressMessage;
    }
}
