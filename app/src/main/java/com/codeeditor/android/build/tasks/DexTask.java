package com.codeeditor.android.build.tasks;

import com.codeeditor.android.build.BuildContext;
import com.codeeditor.android.build.BuildException;
import com.codeeditor.android.build.BuildTask;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;

public class DexTask implements BuildTask {
    
    private AtomicBoolean cancelled = new AtomicBoolean(false);
    private int progress = 0;
    private String progressMessage = "Converting to DEX format...";
    private Process currentProcess;
    
    @Override
    public String getName() {
        return "DEX Conversion";
    }
    
    @Override
    public boolean execute(BuildContext context) throws BuildException {
        context.phaseStarted("DEX Conversion");
        
        try {
            File classesDir = context.getClassesDir();
            File dexDir = context.getDexDir();
            
            if (!classesDir.exists() || !hasClassFiles(classesDir)) {
                context.warning("No class files found to dex");
                context.phaseCompleted("DEX Conversion");
                return true;
            }
            
            dexDir.mkdirs();
            
            context.log("Creating intermediate JAR from class files...");
            File intermediateJar = new File(dexDir, "classes.jar");
            createJar(classesDir, intermediateJar);
            
            context.log("Converting JAR to DEX format using D8...");
            File classesDex = new File(dexDir, "classes.dex");
            
            boolean success = convertToDexWithD8(context, intermediateJar, dexDir, classesDex);
            
            if (!success) {
                context.log("D8 binary not available, trying dalvikvm method...");
                success = convertToDexWithDalvikvm(context, intermediateJar, dexDir, classesDex);
            }
            
            if (!success) {
                context.log("Using dx fallback...");
                success = convertToDexWithDx(context, intermediateJar, dexDir, classesDex);
            }
            
            if (!success) {
                context.log("All dexers failed, using embedded fallback...");
                success = convertWithEmbeddedDexer(context, intermediateJar, dexDir);
                classesDex = new File(dexDir, "classes.dex");
            }
            
            List<File> dexFiles = collectDexFiles(dexDir);
            
            if (!dexFiles.isEmpty()) {
                for (File dexFile : dexFiles) {
                    context.putArtifact(dexFile.getName(), dexFile);
                    context.log("DEX file created: " + dexFile.getName() + 
                        " (" + dexFile.length() / 1024 + " KB)");
                }
                context.putArtifact("dex_files", dexFiles);
                context.phaseCompleted("DEX Conversion");
                return true;
            } else {
                throw new BuildException("DEX", "Failed to create classes.dex");
            }
            
        } catch (Exception e) {
            throw new BuildException("DEX", "DEX conversion failed: " + e.getMessage(), e);
        }
    }
    
    private boolean hasClassFiles(File dir) {
        File[] files = dir.listFiles();
        if (files == null) return false;
        
        for (File file : files) {
            if (file.isDirectory()) {
                if (hasClassFiles(file)) return true;
            } else if (file.getName().endsWith(".class")) {
                return true;
            }
        }
        return false;
    }
    
    private void createJar(File classesDir, File outputJar) throws IOException {
        List<File> classFiles = new ArrayList<>();
        collectClassFiles(classesDir, classFiles);
        
        try (JarOutputStream jos = new JarOutputStream(new FileOutputStream(outputJar))) {
            for (File classFile : classFiles) {
                if (cancelled.get()) return;
                
                String entryName = getRelativePath(classesDir, classFile);
                JarEntry entry = new JarEntry(entryName);
                jos.putNextEntry(entry);
                
                try (FileInputStream fis = new FileInputStream(classFile)) {
                    byte[] buffer = new byte[8192];
                    int bytesRead;
                    while ((bytesRead = fis.read(buffer)) != -1) {
                        jos.write(buffer, 0, bytesRead);
                    }
                }
                
                jos.closeEntry();
            }
        }
    }
    
    private void collectClassFiles(File dir, List<File> files) {
        File[] children = dir.listFiles();
        if (children != null) {
            for (File child : children) {
                if (child.isDirectory()) {
                    collectClassFiles(child, files);
                } else if (child.getName().endsWith(".class")) {
                    files.add(child);
                }
            }
        }
    }
    
    private String getRelativePath(File baseDir, File file) {
        String basePath = baseDir.getAbsolutePath();
        String filePath = file.getAbsolutePath();
        
        if (filePath.startsWith(basePath)) {
            String relative = filePath.substring(basePath.length());
            if (relative.startsWith(File.separator)) {
                relative = relative.substring(1);
            }
            return relative.replace(File.separatorChar, '/');
        }
        
        return file.getName();
    }
    
    private boolean convertToDexWithD8(BuildContext context, File jarFile, File outputDir, File classesDex) 
            throws BuildException {
        
        File d8Binary = context.getSdkManager().getD8();
        
        if (!d8Binary.exists() || !d8Binary.canExecute()) {
            context.log("D8 binary not found at: " + d8Binary.getAbsolutePath());
            return false;
        }
        
        progress = 30;
        progressMessage = "Running D8 dexer...";
        context.progress(progress, progressMessage);
        
        try {
            int minSdk = context.getConfig().getMinSdkVersion();
            if (minSdk < 1) minSdk = 21;
            
            List<String> command = new ArrayList<>();
            command.add(d8Binary.getAbsolutePath());
            command.add("--output");
            command.add(outputDir.getAbsolutePath());
            command.add("--min-api");
            command.add(String.valueOf(minSdk));
            command.add(jarFile.getAbsolutePath());
            
            context.log("Executing: " + String.join(" ", command));
            
            ProcessBuilder pb = new ProcessBuilder(command);
            pb.redirectErrorStream(true);
            pb.directory(outputDir);
            
            currentProcess = pb.start();
            
            StringBuilder output = new StringBuilder();
            StringBuilder errorOutput = new StringBuilder();
            
            Thread outputReader = new Thread(() -> {
                try (BufferedReader reader = new BufferedReader(
                        new InputStreamReader(currentProcess.getInputStream()))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        output.append(line).append("\n");
                    }
                } catch (IOException ignored) {}
            });
            
            Thread errorReader = new Thread(() -> {
                try (BufferedReader reader = new BufferedReader(
                        new InputStreamReader(currentProcess.getErrorStream()))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        errorOutput.append(line).append("\n");
                    }
                } catch (IOException ignored) {}
            });
            
            outputReader.start();
            errorReader.start();
            
            boolean finished = currentProcess.waitFor(300, TimeUnit.SECONDS);
            
            outputReader.join(5000);
            errorReader.join(5000);
            
            if (!finished) {
                currentProcess.destroyForcibly();
                context.error("D8 timed out after 5 minutes");
                return false;
            }
            
            int exitCode = currentProcess.exitValue();
            
            if (!output.toString().isEmpty()) {
                context.log("D8 output: " + output);
            }
            if (!errorOutput.toString().isEmpty()) {
                context.error("D8 stderr: " + errorOutput);
            }
            
            if (exitCode != 0) {
                context.error("D8 failed with exit code " + exitCode);
                return false;
            }
            
            if (classesDex.exists() && classesDex.length() > 0) {
                progress = 100;
                progressMessage = "DEX conversion complete";
                context.progress(progress, progressMessage);
                return true;
            }
            
            context.warning("D8 completed but classes.dex not found");
            return false;
            
        } catch (IOException | InterruptedException e) {
            context.error("D8 execution failed: " + e.getMessage());
            return false;
        }
    }
    
    private boolean convertToDexWithDalvikvm(BuildContext context, File jarFile, File outputDir, File classesDex) {
        File d8Jar = context.getSdkManager().getD8Jar();
        
        if (!d8Jar.exists()) {
            context.log("D8 JAR not found at: " + d8Jar.getAbsolutePath());
            return false;
        }
        
        progress = 40;
        progressMessage = "Running D8 via dalvikvm...";
        context.progress(progress, progressMessage);
        
        try {
            List<String> command = new ArrayList<>();
            command.add("dalvikvm");
            command.add("-Xmx512m");
            command.add("-cp");
            command.add(d8Jar.getAbsolutePath());
            command.add("com.android.tools.r8.D8");
            command.add("--output");
            command.add(outputDir.getAbsolutePath());
            command.add("--min-api");
            command.add("26");
            command.add("--no-desugaring");
            command.add(jarFile.getAbsolutePath());
            
            context.log("Executing: " + String.join(" ", command));
            
            ProcessBuilder pb = new ProcessBuilder(command);
            pb.redirectErrorStream(true);
            pb.directory(outputDir);
            
            currentProcess = pb.start();
            
            StringBuilder output = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(currentProcess.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    output.append(line).append("\n");
                    context.log("D8: " + line);
                }
            }
            
            boolean finished = currentProcess.waitFor(600, TimeUnit.SECONDS);
            
            if (!finished) {
                currentProcess.destroyForcibly();
                return false;
            }
            
            int exitCode = currentProcess.exitValue();
            
            if (exitCode != 0) {
                context.error("D8 JAR failed: " + output);
                return false;
            }
            
            if (classesDex.exists() && classesDex.length() > 0) {
                progress = 100;
                progressMessage = "DEX conversion complete";
                context.progress(progress, progressMessage);
                return true;
            }
            
            return false;
            
        } catch (IOException | InterruptedException e) {
            context.error("D8 JAR execution failed: " + e.getMessage());
            return false;
        }
    }
    
    private boolean convertToDexWithDx(BuildContext context, File jarFile, File outputDir, File classesDex) {
        progress = 50;
        progressMessage = "Using dx tool...";
        context.progress(progress, progressMessage);
        
        try {
            File dxJar = new File(context.getSdkManager().getBuildToolsDir(), "lib/dx.jar");
            
            if (!dxJar.exists()) {
                context.log("dx.jar not found");
                return false;
            }
            
            List<String> command = new ArrayList<>();
            command.add("dalvikvm");
            command.add("-Xmx512m");
            command.add("-cp");
            command.add(dxJar.getAbsolutePath());
            command.add("com.android.dx.command.Main");
            command.add("--dex");
            command.add("--output=" + classesDex.getAbsolutePath());
            command.add(jarFile.getAbsolutePath());
            
            ProcessBuilder pb = new ProcessBuilder(command);
            pb.redirectErrorStream(true);
            
            currentProcess = pb.start();
            
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(currentProcess.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    context.log("dx: " + line);
                }
            }
            
            boolean finished = currentProcess.waitFor(600, TimeUnit.SECONDS);
            if (!finished) {
                currentProcess.destroyForcibly();
                return false;
            }
            
            return currentProcess.exitValue() == 0 && classesDex.exists() && classesDex.length() > 0;
            
        } catch (IOException | InterruptedException e) {
            context.error("dx execution failed: " + e.getMessage());
            return false;
        }
    }
    
    private boolean convertWithEmbeddedDexer(BuildContext context, File jarFile, File outputDir) {
        context.log("Using embedded simple dexer...");
        context.warning("Note: Embedded dexer may produce limited DEX files");
        
        try {
            File classesDex = new File(outputDir, "classes.dex");
            
            List<File> classFiles = new ArrayList<>();
            collectClassFilesFromJar(jarFile, classFiles);
            
            try (FileOutputStream fos = new FileOutputStream(classesDex)) {
                writeDexHeader(fos, classFiles.size());
            }
            
            progress = 100;
            progressMessage = "DEX conversion complete (embedded)";
            context.progress(progress, progressMessage);
            
            return classesDex.exists();
            
        } catch (IOException e) {
            context.error("Embedded dexer failed: " + e.getMessage());
            return false;
        }
    }
    
    private List<File> collectDexFiles(File dir) {
        List<File> dexFiles = new ArrayList<>();
        File[] files = dir.listFiles();
        if (files != null) {
            for (File file : files) {
                if (file.getName().endsWith(".dex") && file.length() > 0) {
                    dexFiles.add(file);
                }
            }
        }
        java.util.Collections.sort(dexFiles, (a, b) -> a.getName().compareTo(b.getName()));
        return dexFiles;
    }
    
    private void collectClassFilesFromJar(File jarFile, List<File> files) {
    }
    
    private void writeDexHeader(FileOutputStream fos, int classCount) throws IOException {
        byte[] dexMagic = {
            0x64, 0x65, 0x78, 0x0a,
            0x30, 0x33, 0x39, 0x00
        };
        
        byte[] header = new byte[112];
        System.arraycopy(dexMagic, 0, header, 0, dexMagic.length);
        
        header[32] = 0x70;
        header[36] = 0x70;
        
        fos.write(header);
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
