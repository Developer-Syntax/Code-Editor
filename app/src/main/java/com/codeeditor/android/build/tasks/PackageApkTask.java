package com.codeeditor.android.build.tasks;

import com.codeeditor.android.build.BuildContext;
import com.codeeditor.android.build.BuildException;
import com.codeeditor.android.build.BuildTask;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

public class PackageApkTask implements BuildTask {
    
    private AtomicBoolean cancelled = new AtomicBoolean(false);
    private int progress = 0;
    private String progressMessage = "Packaging APK...";
    
    @Override
    public String getName() {
        return "Package APK";
    }
    
    @Override
    public boolean execute(BuildContext context) throws BuildException {
        context.phaseStarted("APK Packaging");
        
        try {
            File resourcesApk = context.getResourcesApk();
            File dexDir = context.getDexDir();
            File unsignedApk = context.getUnsignedApk();
            
            if (!resourcesApk.exists()) {
                throw new BuildException("Package", "Resources APK not found: " + resourcesApk);
            }
            
            unsignedApk.getParentFile().mkdirs();
            
            context.log("Creating unsigned APK...");
            createApk(context, resourcesApk, dexDir, unsignedApk);
            
            context.putArtifact("unsigned.apk", unsignedApk);
            context.log("Unsigned APK created: " + unsignedApk.getName() + 
                " (" + unsignedApk.length() / 1024 + " KB)");
            context.phaseCompleted("APK Packaging");
            
            return true;
            
        } catch (Exception e) {
            throw new BuildException("Package", "APK packaging failed: " + e.getMessage(), e);
        }
    }
    
    private void createApk(BuildContext context, File resourcesApk, File dexDir, File outputApk) 
            throws IOException {
        
        progress = 10;
        progressMessage = "Merging resources and DEX...";
        context.progress(progress, progressMessage);
        
        try (ZipOutputStream zos = new ZipOutputStream(new FileOutputStream(outputApk))) {
            try (ZipInputStream zis = new ZipInputStream(new FileInputStream(resourcesApk))) {
                ZipEntry entry;
                while ((entry = zis.getNextEntry()) != null) {
                    if (cancelled.get()) return;
                    
                    zos.putNextEntry(new ZipEntry(entry.getName()));
                    copyStream(zis, zos);
                    zos.closeEntry();
                    zis.closeEntry();
                }
            }
            
            progress = 50;
            progressMessage = "Adding DEX files...";
            context.progress(progress, progressMessage);
            
            addDexFiles(dexDir, zos);
            
            progress = 80;
            progressMessage = "Adding native libraries...";
            context.progress(progress, progressMessage);
            
            addNativeLibraries(context, zos);
        }
        
        progress = 100;
        progressMessage = "APK created";
        context.progress(progress, progressMessage);
    }
    
    private void addDexFiles(File dexDir, ZipOutputStream zos) throws IOException {
        if (!dexDir.exists()) return;
        
        File[] dexFiles = dexDir.listFiles((dir, name) -> name.endsWith(".dex"));
        if (dexFiles == null) return;
        
        for (int i = 0; i < dexFiles.length; i++) {
            File dexFile = dexFiles[i];
            String entryName = i == 0 ? "classes.dex" : "classes" + (i + 1) + ".dex";
            
            zos.putNextEntry(new ZipEntry(entryName));
            try (FileInputStream fis = new FileInputStream(dexFile)) {
                copyStream(fis, zos);
            }
            zos.closeEntry();
        }
    }
    
    private void addNativeLibraries(BuildContext context, ZipOutputStream zos) throws IOException {
        String[] abis = {"armeabi-v7a", "arm64-v8a", "x86", "x86_64"};
        
        File nativeLibsDir = context.getNativeLibsDir();
        if (nativeLibsDir.exists()) {
            context.log("Adding compiled native libraries from: " + nativeLibsDir.getAbsolutePath());
            addNativeLibsFromDir(nativeLibsDir, abis, zos);
        }
        
        File libsDir = new File(context.getConfig().getProjectDir(), "libs");
        if (libsDir.exists()) {
            context.log("Adding prebuilt native libraries from: " + libsDir.getAbsolutePath());
            addNativeLibsFromDir(libsDir, abis, zos);
        }
        
        File jniLibsDir = new File(context.getConfig().getProjectDir(), "src/main/jniLibs");
        if (jniLibsDir.exists()) {
            context.log("Adding jniLibs native libraries from: " + jniLibsDir.getAbsolutePath());
            addNativeLibsFromDir(jniLibsDir, abis, zos);
        }
    }
    
    private void addNativeLibsFromDir(File baseDir, String[] abis, ZipOutputStream zos) throws IOException {
        for (String abi : abis) {
            File abiDir = new File(baseDir, abi);
            if (abiDir.exists() && abiDir.isDirectory()) {
                File[] soFiles = abiDir.listFiles((dir, name) -> name.endsWith(".so"));
                if (soFiles != null) {
                    for (File soFile : soFiles) {
                        String entryName = "lib/" + abi + "/" + soFile.getName();
                        zos.putNextEntry(new ZipEntry(entryName));
                        try (FileInputStream fis = new FileInputStream(soFile)) {
                            copyStream(fis, zos);
                        }
                        zos.closeEntry();
                    }
                }
            }
        }
    }
    
    private void copyStream(java.io.InputStream in, java.io.OutputStream out) throws IOException {
        byte[] buffer = new byte[8192];
        int bytesRead;
        while ((bytesRead = in.read(buffer)) != -1) {
            out.write(buffer, 0, bytesRead);
        }
    }
    
    @Override
    public void cancel() {
        cancelled.set(true);
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
