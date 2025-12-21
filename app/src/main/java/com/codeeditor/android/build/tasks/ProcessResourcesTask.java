package com.codeeditor.android.build.tasks;

import com.codeeditor.android.build.BuildContext;
import com.codeeditor.android.build.BuildException;
import com.codeeditor.android.build.BuildTask;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

public class ProcessResourcesTask implements BuildTask {
    
    private AtomicBoolean cancelled = new AtomicBoolean(false);
    private int progress = 0;
    private String progressMessage = "Processing resources...";
    private Process currentProcess;
    
    private Map<String, Integer> resourceIds = new HashMap<>();
    private int nextId = 0x7f010001;
    
    @Override
    public String getName() {
        return "Process Resources";
    }
    
    @Override
    public boolean execute(BuildContext context) throws BuildException {
        context.phaseStarted("Resource Processing");
        
        try {
            File resDir = context.getConfig().getResDir();
            File manifestFile = context.getConfig().getManifestFile();
            File outputApk = context.getResourcesApk();
            
            if (!manifestFile.exists()) {
                throw new BuildException("Resources", "AndroidManifest.xml not found: " + manifestFile);
            }
            
            outputApk.getParentFile().mkdirs();
            
            boolean aapt2Success = processWithAapt2(context, resDir, manifestFile, outputApk);
            
            if (!aapt2Success) {
                throw new BuildException("Resources", 
                    "AAPT2 resource processing failed. Please ensure SDK tools are properly installed. " +
                    "The build cannot proceed without AAPT2 as fallback resource processing would produce invalid APKs.");
            }
            
            context.putArtifact("resources.ap_", outputApk);
            context.log("Resources processed: " + outputApk.getName());
            context.phaseCompleted("Resource Processing");
            
            return true;
            
        } catch (Exception e) {
            throw new BuildException("Resources", "Resource processing failed: " + e.getMessage(), e);
        }
    }
    
    private boolean processWithAapt2(BuildContext context, File resDir, File manifestFile, File outputApk) {
        File aapt2 = context.getSdkManager().getAapt2();
        
        if (!aapt2.exists() || !aapt2.canExecute()) {
            context.log("AAPT2 not found at: " + aapt2.getAbsolutePath());
            return false;
        }
        
        try {
            File intermediateDir = new File(context.getBuildDir(), "res-compiled");
            intermediateDir.mkdirs();
            
            if (resDir.exists() && resDir.isDirectory()) {
                progress = 20;
                progressMessage = "Compiling resources with AAPT2...";
                context.progress(progress, progressMessage);
                
                boolean compiled = compileResourcesWithAapt2(context, aapt2, resDir, intermediateDir);
                if (!compiled) {
                    context.log("AAPT2 compile failed, will use fallback");
                    return false;
                }
            }
            
            progress = 60;
            progressMessage = "Linking resources with AAPT2...";
            context.progress(progress, progressMessage);
            
            return linkResources(context, aapt2, intermediateDir, resDir, manifestFile, outputApk);
            
        } catch (Exception e) {
            context.error("AAPT2 processing failed: " + e.getMessage());
            return false;
        }
    }
    
    private boolean compileResourcesWithAapt2(BuildContext context, File aapt2, 
                                              File resDir, File outputDir) throws IOException, InterruptedException {
        
        File resourcesZip = new File(outputDir, "resources.zip");
        
        List<String> command = new ArrayList<>();
        command.add(aapt2.getAbsolutePath());
        command.add("compile");
        command.add("--dir");
        command.add(resDir.getAbsolutePath());
        command.add("-o");
        command.add(resourcesZip.getAbsolutePath());
        
        context.log("Executing AAPT2 compile: " + String.join(" ", command));
        
        ProcessBuilder pb = new ProcessBuilder(command);
        pb.redirectErrorStream(true);
        
        currentProcess = pb.start();
        
        StringBuilder output = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(currentProcess.getInputStream()))) {
            String line;
            while ((line = reader.readLine()) != null) {
                output.append(line).append("\n");
                context.log("AAPT2: " + line);
            }
        }
        
        boolean finished = currentProcess.waitFor(180, TimeUnit.SECONDS);
        if (!finished) {
            currentProcess.destroyForcibly();
            context.error("AAPT2 compile timed out");
            return false;
        }
        
        int exitCode = currentProcess.exitValue();
        if (exitCode != 0) {
            context.error("AAPT2 compile failed with exit code " + exitCode + ": " + output);
            return false;
        }
        
        if (!resourcesZip.exists() || resourcesZip.length() == 0) {
            context.error("AAPT2 compile did not produce resources.zip");
            return false;
        }
        
        context.log("Compiled resources to " + resourcesZip.getName() + " (" + resourcesZip.length() / 1024 + " KB)");
        return true;
    }
    
    private boolean linkResources(BuildContext context, File aapt2, File compiledResDir,
                                  File resDir, File manifestFile, File outputApk) throws IOException, InterruptedException {
        
        List<String> command = new ArrayList<>();
        command.add(aapt2.getAbsolutePath());
        command.add("link");
        command.add("-o");
        command.add(outputApk.getAbsolutePath());
        command.add("--manifest");
        command.add(manifestFile.getAbsolutePath());
        
        File androidJar = context.getAndroidJar();
        if (androidJar != null && androidJar.exists() && androidJar.length() > 0) {
            command.add("-I");
            command.add(androidJar.getAbsolutePath());
        }
        
        command.add("--auto-add-overlay");
        
        command.add("--min-sdk-version");
        command.add(String.valueOf(context.getConfig().getMinSdkVersion()));
        
        command.add("--target-sdk-version");
        command.add(String.valueOf(context.getConfig().getTargetSdkVersion()));
        
        File rJavaDir = new File(context.getBuildDir(), "generated");
        rJavaDir.mkdirs();
        command.add("--java");
        command.add(rJavaDir.getAbsolutePath());
        
        File resourcesZip = new File(compiledResDir, "resources.zip");
        if (resourcesZip.exists()) {
            command.add("-R");
            command.add(resourcesZip.getAbsolutePath());
            context.log("Using compiled resources archive: " + resourcesZip.getName());
        } else {
            List<File> flatFiles = new ArrayList<>();
            collectFlatFilesRecursive(compiledResDir, flatFiles);
            
            context.log("Found " + flatFiles.size() + " compiled .flat files for linking");
            
            if (flatFiles.isEmpty()) {
                context.error("No compiled .flat resource files found in " + compiledResDir);
                return false;
            }
            
            for (File file : flatFiles) {
                command.add("-R");
                command.add(file.getAbsolutePath());
            }
        }
        
        context.log("Executing AAPT2 link: " + String.join(" ", command));
        
        ProcessBuilder pb = new ProcessBuilder(command);
        pb.redirectErrorStream(true);
        
        currentProcess = pb.start();
        
        StringBuilder output = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(currentProcess.getInputStream()))) {
            String line;
            while ((line = reader.readLine()) != null) {
                output.append(line).append("\n");
                context.log("AAPT2: " + line);
            }
        }
        
        boolean finished = currentProcess.waitFor(120, TimeUnit.SECONDS);
        
        if (!finished) {
            currentProcess.destroyForcibly();
            context.error("AAPT2 link timed out");
            return false;
        }
        
        int exitCode = currentProcess.exitValue();
        if (exitCode != 0) {
            context.error("AAPT2 link failed with exit code " + exitCode + ": " + output);
            return false;
        }
        
        progress = 100;
        progressMessage = "Resources linked successfully";
        context.progress(progress, progressMessage);
        
        return outputApk.exists();
    }
    
    private void generateRJava(BuildContext context, File resDir) throws IOException {
        resourceIds.clear();
        nextId = 0x7f010001;
        
        Map<String, List<String>> resources = collectResources(resDir);
        
        File rJavaDir = new File(context.getClassesDir().getParentFile(), 
            "generated/" + context.getConfig().getPackageName().replace('.', '/'));
        rJavaDir.mkdirs();
        File rJava = new File(rJavaDir, "R.java");
        
        StringBuilder sb = new StringBuilder();
        sb.append("package ").append(context.getConfig().getPackageName()).append(";\n\n");
        sb.append("public final class R {\n");
        
        for (Map.Entry<String, List<String>> entry : resources.entrySet()) {
            String type = entry.getKey();
            List<String> names = entry.getValue();
            
            sb.append("    public static final class ").append(type).append(" {\n");
            
            for (String name : names) {
                int id = nextId++;
                String fullName = type + "." + name;
                resourceIds.put(fullName, id);
                sb.append("        public static final int ").append(name)
                  .append(" = 0x").append(Integer.toHexString(id)).append(";\n");
            }
            
            sb.append("    }\n");
        }
        
        sb.append("}\n");
        
        try (FileWriter writer = new FileWriter(rJava)) {
            writer.write(sb.toString());
        }
        
        context.log("Generated R.java with " + resourceIds.size() + " resources");
        context.putArtifact("R.java", rJava);
    }
    
    private Map<String, List<String>> collectResources(File resDir) {
        Map<String, List<String>> resources = new HashMap<>();
        
        if (!resDir.exists()) {
            return resources;
        }
        
        File[] resDirs = resDir.listFiles();
        if (resDirs == null) return resources;
        
        for (File dir : resDirs) {
            if (!dir.isDirectory()) continue;
            
            String dirName = dir.getName();
            String type = getResourceType(dirName);
            
            if (type != null) {
                List<String> names = resources.computeIfAbsent(type, k -> new ArrayList<>());
                
                File[] files = dir.listFiles();
                if (files != null) {
                    for (File file : files) {
                        String name = getResourceName(file.getName(), type);
                        if (name != null && !names.contains(name)) {
                            names.add(name);
                        }
                    }
                }
            }
            
            if (type != null && type.equals("values")) {
                parseValuesResources(dir, resources);
            }
        }
        
        return resources;
    }
    
    private String getResourceType(String dirName) {
        if (dirName.startsWith("layout")) return "layout";
        if (dirName.startsWith("drawable")) return "drawable";
        if (dirName.startsWith("mipmap")) return "mipmap";
        if (dirName.startsWith("values")) return "values";
        if (dirName.startsWith("menu")) return "menu";
        if (dirName.startsWith("anim")) return "anim";
        if (dirName.startsWith("raw")) return "raw";
        if (dirName.startsWith("xml")) return "xml";
        if (dirName.startsWith("color")) return "color";
        return null;
    }
    
    private String getResourceName(String fileName, String type) {
        if (type.equals("values")) return null;
        
        int dotIndex = fileName.lastIndexOf('.');
        if (dotIndex > 0) {
            return fileName.substring(0, dotIndex).replaceAll("[^a-zA-Z0-9_]", "_");
        }
        return fileName.replaceAll("[^a-zA-Z0-9_]", "_");
    }
    
    private void parseValuesResources(File valuesDir, Map<String, List<String>> resources) {
        File[] files = valuesDir.listFiles();
        if (files == null) return;
        
        Pattern stringPattern = Pattern.compile("<string\\s+name=\"([^\"]+)\"");
        Pattern colorPattern = Pattern.compile("<color\\s+name=\"([^\"]+)\"");
        Pattern dimenPattern = Pattern.compile("<dimen\\s+name=\"([^\"]+)\"");
        Pattern stylePattern = Pattern.compile("<style\\s+name=\"([^\"]+)\"");
        Pattern idPattern = Pattern.compile("<item\\s+.*name=\"([^\"]+)\"\\s+type=\"id\"");
        
        for (File file : files) {
            if (!file.getName().endsWith(".xml")) continue;
            
            try {
                String content = readFile(file);
                
                parseResourcePattern(content, stringPattern, "string", resources);
                parseResourcePattern(content, colorPattern, "color", resources);
                parseResourcePattern(content, dimenPattern, "dimen", resources);
                parseResourcePattern(content, stylePattern, "style", resources);
                parseResourcePattern(content, idPattern, "id", resources);
                
            } catch (IOException ignored) {}
        }
    }
    
    private void parseResourcePattern(String content, Pattern pattern, String type, 
                                      Map<String, List<String>> resources) {
        Matcher matcher = pattern.matcher(content);
        List<String> names = resources.computeIfAbsent(type, k -> new ArrayList<>());
        
        while (matcher.find()) {
            String name = matcher.group(1).replaceAll("[^a-zA-Z0-9_]", "_");
            if (!names.contains(name)) {
                names.add(name);
            }
        }
    }
    
    private String readFile(File file) throws IOException {
        StringBuilder content = new StringBuilder();
        try (FileInputStream fis = new FileInputStream(file)) {
            byte[] buffer = new byte[8192];
            int bytesRead;
            while ((bytesRead = fis.read(buffer)) != -1) {
                content.append(new String(buffer, 0, bytesRead));
            }
        }
        return content.toString();
    }
    
    private void compileResourcesFallback(BuildContext context, File resDir, File manifest, File output) 
            throws IOException {
        
        progress = 50;
        progressMessage = "Packaging resources (fallback)...";
        context.progress(progress, progressMessage);
        
        try (ZipOutputStream zos = new ZipOutputStream(new FileOutputStream(output))) {
            zos.putNextEntry(new ZipEntry("AndroidManifest.xml"));
            copyFileToZip(manifest, zos);
            zos.closeEntry();
            
            if (resDir.exists()) {
                addResourceDirectory(resDir, "res", zos);
            }
            
            zos.putNextEntry(new ZipEntry("resources.arsc"));
            zos.write(createResourcesArsc());
            zos.closeEntry();
        }
        
        progress = 100;
        progressMessage = "Resources packaged (fallback)";
        context.progress(progress, progressMessage);
    }
    
    private void addResourceDirectory(File dir, String basePath, ZipOutputStream zos) throws IOException {
        File[] files = dir.listFiles();
        if (files == null) return;
        
        for (File file : files) {
            if (cancelled.get()) return;
            
            String entryPath = basePath + "/" + file.getName();
            
            if (file.isDirectory()) {
                addResourceDirectory(file, entryPath, zos);
            } else {
                zos.putNextEntry(new ZipEntry(entryPath));
                copyFileToZip(file, zos);
                zos.closeEntry();
            }
        }
    }
    
    private void copyFileToZip(File file, ZipOutputStream zos) throws IOException {
        try (FileInputStream fis = new FileInputStream(file)) {
            byte[] buffer = new byte[8192];
            int bytesRead;
            while ((bytesRead = fis.read(buffer)) != -1) {
                zos.write(buffer, 0, bytesRead);
            }
        }
    }
    
    private byte[] createResourcesArsc() {
        return new byte[8];
    }
    
    private void collectFlatFilesRecursive(File dir, List<File> result) {
        if (dir == null || !dir.exists()) return;
        
        File[] files = dir.listFiles();
        if (files != null) {
            for (File file : files) {
                if (file.isDirectory()) {
                    collectFlatFilesRecursive(file, result);
                } else if (file.getName().endsWith(".flat")) {
                    result.add(file);
                }
            }
        }
    }
    
    private void extractZip(File zipFile, File destDir) throws IOException {
        try (java.util.zip.ZipInputStream zis = new java.util.zip.ZipInputStream(
                new java.io.BufferedInputStream(new FileInputStream(zipFile)))) {
            java.util.zip.ZipEntry entry;
            byte[] buffer = new byte[8192];
            
            while ((entry = zis.getNextEntry()) != null) {
                String name = entry.getName();
                
                if (name.contains("..")) {
                    continue;
                }
                
                File file = new File(destDir, name);
                
                if (entry.isDirectory()) {
                    file.mkdirs();
                } else {
                    file.getParentFile().mkdirs();
                    try (FileOutputStream fos = new FileOutputStream(file)) {
                        int count;
                        while ((count = zis.read(buffer)) != -1) {
                            fos.write(buffer, 0, count);
                        }
                    }
                }
                
                zis.closeEntry();
            }
        }
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
