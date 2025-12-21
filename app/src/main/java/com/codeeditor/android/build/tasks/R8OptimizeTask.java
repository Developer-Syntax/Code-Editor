package com.codeeditor.android.build.tasks;

import com.codeeditor.android.build.BuildContext;
import com.codeeditor.android.build.BuildException;
import com.codeeditor.android.build.BuildTask;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

public class R8OptimizeTask implements BuildTask {
    
    private AtomicBoolean cancelled = new AtomicBoolean(false);
    private int progress = 0;
    private String progressMessage = "Optimizing with R8...";
    private Process currentProcess;
    
    @Override
    public String getName() {
        return "R8 Optimization";
    }
    
    @Override
    public boolean execute(BuildContext context) throws BuildException {
        if (context.getConfig().isDebugBuild()) {
            context.log("Skipping R8 optimization for debug build");
            return true;
        }
        
        context.phaseStarted("R8 Optimization");
        
        try {
            File dexDir = context.getDexDir();
            File classesJar = new File(dexDir, "classes.jar");
            
            if (!classesJar.exists()) {
                context.warning("classes.jar not found, skipping R8");
                return true;
            }
            
            File r8Jar = context.getSdkManager().getR8Jar();
            
            if (r8Jar == null || !r8Jar.exists()) {
                context.log("R8 not available, using D8 instead");
                return true;
            }
            
            File outputDir = new File(dexDir, "optimized");
            outputDir.mkdirs();
            
            File proguardRules = createProguardRules(context);
            
            boolean success = runR8(context, classesJar, outputDir, proguardRules);
            
            if (success) {
                File optimizedDex = new File(outputDir, "classes.dex");
                if (optimizedDex.exists()) {
                    File originalDex = new File(dexDir, "classes.dex");
                    
                    long originalSize = originalDex.exists() ? originalDex.length() : 0;
                    long optimizedSize = optimizedDex.length();
                    
                    if (optimizedSize > 0 && optimizedSize < originalSize) {
                        if (originalDex.exists()) {
                            originalDex.renameTo(new File(dexDir, "classes.dex.backup"));
                        }
                        optimizedDex.renameTo(originalDex);
                        
                        long savedKb = (originalSize - optimizedSize) / 1024;
                        int savedPercent = (int) ((originalSize - optimizedSize) * 100 / originalSize);
                        
                        context.log("R8 reduced DEX size by " + savedKb + " KB (" + savedPercent + "%)");
                    }
                }
                
                context.phaseCompleted("R8 Optimization");
            }
            
            return success;
            
        } catch (Exception e) {
            context.warning("R8 optimization failed: " + e.getMessage());
            return true;
        }
    }
    
    private File createProguardRules(BuildContext context) throws IOException {
        File rulesFile = new File(context.getBuildDir(), "proguard-rules.pro");
        
        File projectRules = new File(context.getConfig().getProjectDir(), "proguard-rules.pro");
        if (projectRules.exists()) {
            return projectRules;
        }
        
        try (FileWriter writer = new FileWriter(rulesFile)) {
            writer.write("# Default ProGuard rules for R8\n\n");
            
            writer.write("-keepclassmembers class * {\n");
            writer.write("    public static void main(java.lang.String[]);\n");
            writer.write("}\n\n");
            
            writer.write("-keepclassmembers class * extends android.app.Activity {\n");
            writer.write("    public void *(android.view.View);\n");
            writer.write("}\n\n");
            
            writer.write("-keep public class * extends android.app.Activity\n");
            writer.write("-keep public class * extends android.app.Application\n");
            writer.write("-keep public class * extends android.app.Service\n");
            writer.write("-keep public class * extends android.content.BroadcastReceiver\n");
            writer.write("-keep public class * extends android.content.ContentProvider\n\n");
            
            writer.write("-keepclassmembers class * implements java.io.Serializable {\n");
            writer.write("    static final long serialVersionUID;\n");
            writer.write("    private static final java.io.ObjectStreamField[] serialPersistentFields;\n");
            writer.write("    private void writeObject(java.io.ObjectOutputStream);\n");
            writer.write("    private void readObject(java.io.ObjectInputStream);\n");
            writer.write("    java.lang.Object writeReplace();\n");
            writer.write("    java.lang.Object readResolve();\n");
            writer.write("}\n\n");
            
            writer.write("-dontwarn android.**\n");
            writer.write("-dontwarn androidx.**\n");
            writer.write("-dontwarn com.google.**\n");
            writer.write("-dontwarn kotlin.**\n");
            writer.write("-dontwarn kotlinx.**\n\n");
            
            writer.write("-optimizationpasses 3\n");
            writer.write("-allowaccessmodification\n");
            writer.write("-repackageclasses ''\n");
        }
        
        return rulesFile;
    }
    
    private boolean runR8(BuildContext context, File inputJar, File outputDir, File proguardRules) {
        try {
            File r8Jar = context.getSdkManager().getR8Jar();
            File androidJar = context.getAndroidJar();
            
            progress = 20;
            progressMessage = "Running R8 optimizer...";
            context.progress(progress, progressMessage);
            
            List<String> command = new ArrayList<>();
            command.add("dalvikvm");
            command.add("-Xmx1024m");
            command.add("-cp");
            command.add(r8Jar.getAbsolutePath());
            command.add("com.android.tools.r8.R8");
            command.add("--output");
            command.add(outputDir.getAbsolutePath());
            command.add("--min-api");
            command.add(String.valueOf(context.getConfig().getMinSdkVersion()));
            
            if (androidJar != null && androidJar.exists()) {
                command.add("--lib");
                command.add(androidJar.getAbsolutePath());
            }
            
            if (proguardRules != null && proguardRules.exists()) {
                command.add("--pg-conf");
                command.add(proguardRules.getAbsolutePath());
            }
            
            command.add(inputJar.getAbsolutePath());
            
            context.log("Executing R8: " + command.get(0) + " ...");
            
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
                    if (line.contains("Error") || line.contains("error")) {
                        context.error("R8: " + line);
                    } else {
                        context.log("R8: " + line);
                    }
                }
            }
            
            boolean finished = currentProcess.waitFor(600, TimeUnit.SECONDS);
            
            if (!finished) {
                currentProcess.destroyForcibly();
                context.error("R8 timed out");
                return false;
            }
            
            int exitCode = currentProcess.exitValue();
            
            if (exitCode != 0) {
                context.warning("R8 exited with code " + exitCode);
                return false;
            }
            
            progress = 100;
            progressMessage = "R8 optimization complete";
            context.progress(progress, progressMessage);
            
            return true;
            
        } catch (IOException | InterruptedException e) {
            context.error("R8 execution failed: " + e.getMessage());
            return false;
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
