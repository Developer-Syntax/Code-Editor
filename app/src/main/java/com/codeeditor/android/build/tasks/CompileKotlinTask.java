package com.codeeditor.android.build.tasks;

import com.codeeditor.android.build.BuildContext;
import com.codeeditor.android.build.BuildException;
import com.codeeditor.android.build.BuildTask;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

public class CompileKotlinTask implements BuildTask {
    
    private static final String KOTLIN_COMPILER_VERSION = "1.9.22";
    private static final String KOTLIN_COMPILER_URL = 
        "https://github.com/JetBrains/kotlin/releases/download/v" + KOTLIN_COMPILER_VERSION + 
        "/kotlin-compiler-" + KOTLIN_COMPILER_VERSION + ".zip";
    
    private AtomicBoolean cancelled = new AtomicBoolean(false);
    private int progress = 0;
    private String progressMessage = "Compiling Kotlin sources...";
    private Process currentProcess;
    
    @Override
    public String getName() {
        return "Compile Kotlin";
    }
    
    @Override
    public boolean execute(BuildContext context) throws BuildException {
        context.phaseStarted("Kotlin Compilation");
        
        try {
            File sourceDir = context.getConfig().getSourceDir();
            File classesDir = context.getClassesDir();
            File androidJar = context.getAndroidJar();
            
            List<File> kotlinFiles = collectKotlinFiles(sourceDir);
            
            if (kotlinFiles.isEmpty()) {
                context.log("No Kotlin source files found, skipping Kotlin compilation");
                context.phaseCompleted("Kotlin Compilation");
                return true;
            }
            
            context.log("Found " + kotlinFiles.size() + " Kotlin source files");
            
            classesDir.mkdirs();
            
            File kotlincDir = ensureKotlinCompiler(context);
            
            if (kotlincDir == null) {
                context.warning("Kotlin compiler not available, attempting embedded compilation...");
                return compileWithEmbeddedKotlin(context, kotlinFiles, classesDir, androidJar);
            }
            
            boolean success = compileWithKotlinc(context, kotlinFiles, classesDir, androidJar, kotlincDir);
            
            if (success) {
                context.phaseCompleted("Kotlin Compilation");
            }
            
            return success;
            
        } catch (Exception e) {
            throw new BuildException("Compile", "Kotlin compilation failed: " + e.getMessage(), e);
        }
    }
    
    private List<File> collectKotlinFiles(File dir) {
        List<File> files = new ArrayList<>();
        collectKotlinFilesRecursive(dir, files);
        return files;
    }
    
    private void collectKotlinFilesRecursive(File dir, List<File> files) {
        if (!dir.exists()) return;
        
        File[] children = dir.listFiles();
        if (children != null) {
            for (File child : children) {
                if (cancelled.get()) return;
                
                if (child.isDirectory()) {
                    collectKotlinFilesRecursive(child, files);
                } else if (child.getName().endsWith(".kt") || child.getName().endsWith(".kts")) {
                    files.add(child);
                }
            }
        }
    }
    
    private File ensureKotlinCompiler(BuildContext context) {
        File kotlinHome = new File(context.getSdkManager().getSdkDir(), "kotlin");
        File kotlinc = new File(kotlinHome, "bin/kotlinc");
        
        if (kotlinc.exists() && kotlinc.canExecute()) {
            context.log("Kotlin compiler found at: " + kotlinc.getAbsolutePath());
            return kotlinHome;
        }
        
        if (new File(kotlinHome, "kotlinc/bin/kotlinc").exists()) {
            return new File(kotlinHome, "kotlinc");
        }
        
        context.log("Kotlin compiler not found, attempting to download...");
        
        try {
            if (downloadKotlinCompiler(context, kotlinHome)) {
                File extracted = new File(kotlinHome, "kotlinc");
                if (extracted.exists()) {
                    return extracted;
                }
            }
        } catch (Exception e) {
            context.error("Failed to download Kotlin compiler: " + e.getMessage());
        }
        
        return null;
    }
    
    private boolean downloadKotlinCompiler(BuildContext context, File kotlinHome) {
        try {
            kotlinHome.mkdirs();
            
            File kotlincBin = new File(kotlinHome, "kotlinc/bin/kotlinc");
            if (kotlincBin.exists()) {
                kotlincBin.setExecutable(true);
                return true;
            }
            
            progress = 10;
            progressMessage = "Downloading Kotlin compiler...";
            context.progress(progress, progressMessage);
            
            File zipFile = new File(kotlinHome, "kotlin-compiler.zip");
            
            if (zipFile.exists() && zipFile.length() > 50 * 1024 * 1024) {
                context.log("Using cached Kotlin compiler zip");
            } else {
                URL url = new URL(KOTLIN_COMPILER_URL);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestProperty("User-Agent", "CodeEditor-Android/2.0");
                conn.setConnectTimeout(30000);
                conn.setReadTimeout(120000);
                conn.setInstanceFollowRedirects(true);
                
                int responseCode = conn.getResponseCode();
                
                if (responseCode == 302 || responseCode == 301) {
                    String redirectUrl = conn.getHeaderField("Location");
                    if (redirectUrl != null) {
                        conn.disconnect();
                        conn = (HttpURLConnection) new URL(redirectUrl).openConnection();
                        conn.setRequestProperty("User-Agent", "CodeEditor-Android/2.0");
                        responseCode = conn.getResponseCode();
                    }
                }
                
                if (responseCode != 200) {
                    context.error("Failed to download Kotlin: HTTP " + responseCode);
                    return false;
                }
                
                long totalSize = conn.getContentLengthLong();
                long downloaded = 0;
                
                try (InputStream is = conn.getInputStream();
                     FileOutputStream fos = new FileOutputStream(zipFile)) {
                    
                    byte[] buffer = new byte[16384];
                    int bytesRead;
                    
                    while ((bytesRead = is.read(buffer)) != -1) {
                        if (cancelled.get()) {
                            conn.disconnect();
                            zipFile.delete();
                            return false;
                        }
                        
                        fos.write(buffer, 0, bytesRead);
                        downloaded += bytesRead;
                        
                        if (totalSize > 0) {
                            progress = 10 + (int) ((downloaded * 40) / totalSize);
                            progressMessage = "Downloading Kotlin: " + (downloaded / 1024 / 1024) + " MB / " + 
                                (totalSize / 1024 / 1024) + " MB";
                            context.progress(progress, progressMessage);
                        }
                    }
                }
                conn.disconnect();
            }
            
            if (!zipFile.exists() || zipFile.length() < 10 * 1024 * 1024) {
                context.error("Kotlin download incomplete");
                return false;
            }
            
            progress = 50;
            progressMessage = "Extracting Kotlin compiler...";
            context.progress(progress, progressMessage);
            
            boolean extracted = extractZip(zipFile, kotlinHome);
            
            if (extracted) {
                kotlincBin = new File(kotlinHome, "kotlinc/bin/kotlinc");
                if (kotlincBin.exists()) {
                    kotlincBin.setExecutable(true);
                    new File(kotlinHome, "kotlinc/bin/kotlin").setExecutable(true);
                    context.log("Kotlin compiler installed successfully");
                } else {
                    context.error("Kotlin extraction failed - kotlinc not found");
                    return false;
                }
            }
            
            progress = 60;
            progressMessage = "Kotlin compiler ready";
            context.progress(progress, progressMessage);
            
            return extracted;
            
        } catch (Exception e) {
            context.error("Kotlin download failed: " + e.getMessage());
            return false;
        }
    }
    
    private boolean extractZip(File zipFile, File destDir) {
        try {
            ProcessBuilder pb = new ProcessBuilder("unzip", "-o", "-q", 
                zipFile.getAbsolutePath(), "-d", destDir.getAbsolutePath());
            Process process = pb.start();
            boolean finished = process.waitFor(120, TimeUnit.SECONDS);
            
            if (!finished) {
                process.destroyForcibly();
                return false;
            }
            
            return process.exitValue() == 0;
            
        } catch (Exception e) {
            return false;
        }
    }
    
    private boolean compileWithKotlinc(BuildContext context, List<File> kotlinFiles,
                                        File classesDir, File androidJar, File kotlinHome) {
        
        try {
            progress = 60;
            progressMessage = "Running Kotlin compiler...";
            context.progress(progress, progressMessage);
            
            File kotlinc = new File(kotlinHome, "bin/kotlinc");
            if (!kotlinc.exists()) {
                kotlinc = new File(kotlinHome, "kotlinc/bin/kotlinc");
            }
            
            if (!kotlinc.exists()) {
                context.error("kotlinc binary not found");
                return false;
            }
            
            kotlinc.setExecutable(true);
            
            List<String> command = new ArrayList<>();
            command.add(kotlinc.getAbsolutePath());
            command.add("-d");
            command.add(classesDir.getAbsolutePath());
            command.add("-jvm-target");
            command.add("11");
            command.add("-no-stdlib");
            command.add("-no-reflect");
            
            if (androidJar != null && androidJar.exists()) {
                command.add("-classpath");
                command.add(androidJar.getAbsolutePath() + File.pathSeparator + classesDir.getAbsolutePath());
            }
            
            for (File ktFile : kotlinFiles) {
                command.add(ktFile.getAbsolutePath());
            }
            
            context.log("Executing: " + command.get(0) + " ... (" + kotlinFiles.size() + " files)");
            
            ProcessBuilder pb = new ProcessBuilder(command);
            pb.redirectErrorStream(true);
            
            currentProcess = pb.start();
            
            StringBuilder output = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(currentProcess.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    output.append(line).append("\n");
                    if (line.contains("error:") || line.contains("Error:")) {
                        context.error("kotlinc: " + line);
                    } else if (line.contains("warning:") || line.contains("Warning:")) {
                        context.warning("kotlinc: " + line);
                    } else {
                        context.log("kotlinc: " + line);
                    }
                }
            }
            
            boolean finished = currentProcess.waitFor(600, TimeUnit.SECONDS);
            
            if (!finished) {
                currentProcess.destroyForcibly();
                context.error("Kotlin compilation timed out");
                return false;
            }
            
            int exitCode = currentProcess.exitValue();
            
            if (exitCode != 0) {
                context.error("kotlinc failed with exit code " + exitCode);
                return false;
            }
            
            int compiledCount = countClassFiles(classesDir);
            context.log("Kotlin compiled " + compiledCount + " class files");
            
            progress = 100;
            progressMessage = "Kotlin compilation complete";
            context.progress(progress, progressMessage);
            
            return true;
            
        } catch (IOException | InterruptedException e) {
            context.error("Kotlin compilation error: " + e.getMessage());
            return false;
        }
    }
    
    private boolean compileWithEmbeddedKotlin(BuildContext context, List<File> kotlinFiles,
                                               File classesDir, File androidJar) {
        try {
            Class<?> k2JvmCompiler = Class.forName("org.jetbrains.kotlin.cli.jvm.K2JVMCompiler");
            
            List<String> args = new ArrayList<>();
            args.add("-d");
            args.add(classesDir.getAbsolutePath());
            args.add("-jvm-target");
            args.add("11");
            
            if (androidJar != null && androidJar.exists()) {
                args.add("-classpath");
                args.add(androidJar.getAbsolutePath());
            }
            
            for (File ktFile : kotlinFiles) {
                args.add(ktFile.getAbsolutePath());
            }
            
            Object compiler = k2JvmCompiler.getDeclaredConstructor().newInstance();
            java.lang.reflect.Method execMethod = k2JvmCompiler.getMethod("exec", 
                java.io.PrintStream.class, String[].class);
            
            Object result = execMethod.invoke(compiler, System.err, args.toArray(new String[0]));
            
            int exitCode = (Integer) result.getClass().getMethod("getCode").invoke(result);
            
            return exitCode == 0;
            
        } catch (ClassNotFoundException e) {
            context.log("Embedded Kotlin compiler not available");
            return false;
        } catch (Exception e) {
            context.error("Embedded Kotlin compilation failed: " + e.getMessage());
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
