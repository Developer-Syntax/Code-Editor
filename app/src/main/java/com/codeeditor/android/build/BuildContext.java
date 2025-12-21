package com.codeeditor.android.build;

import android.content.Context;

import com.codeeditor.android.build.DependencyResolver.Dependency;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class BuildContext {
    
    private Context appContext;
    private BuildConfig config;
    private File sdkDir;
    private File androidJar;
    private SdkManager sdkManager;
    private Map<String, Object> artifacts;
    private List<String> logs;
    private List<String> errors;
    private List<String> warnings;
    private boolean cancelled;
    private BuildListener listener;
    private List<Dependency> dependencies;
    private IncrementalBuildCache incrementalCache;
    
    public BuildContext(Context appContext, BuildConfig config) {
        this.appContext = appContext;
        this.config = config;
        this.artifacts = new HashMap<>();
        this.logs = new ArrayList<>();
        this.errors = new ArrayList<>();
        this.warnings = new ArrayList<>();
        this.cancelled = false;
    }
    
    public void setSdkManager(SdkManager sdkManager) {
        this.sdkManager = sdkManager;
    }
    
    public SdkManager getSdkManager() {
        return sdkManager;
    }
    
    public File getBuildDir() {
        return config.getIntermediatesDir().getParentFile();
    }
    
    public interface BuildListener {
        void onPhaseStarted(String phase);
        void onPhaseCompleted(String phase);
        void onProgress(int progress, String message);
        void onLog(String message);
        void onError(String error);
        void onWarning(String warning);
    }
    
    public Context getAppContext() { return appContext; }
    public BuildConfig getConfig() { return config; }
    public File getSdkDir() { return sdkDir; }
    public File getAndroidJar() { return androidJar; }
    
    public void setSdkDir(File sdkDir) { 
        this.sdkDir = sdkDir;
        this.androidJar = new File(sdkDir, "platforms/android-" + config.getTargetSdkVersion() + "/android.jar");
    }
    
    public void setListener(BuildListener listener) {
        this.listener = listener;
    }
    
    public void putArtifact(String key, Object artifact) {
        artifacts.put(key, artifact);
    }
    
    @SuppressWarnings("unchecked")
    public <T> T getArtifact(String key, Class<T> type) {
        Object artifact = artifacts.get(key);
        if (artifact != null && type.isInstance(artifact)) {
            return (T) artifact;
        }
        return null;
    }
    
    public void log(String message) {
        logs.add(message);
        if (listener != null) {
            listener.onLog(message);
        }
    }
    
    public void error(String error) {
        errors.add(error);
        if (listener != null) {
            listener.onError(error);
        }
    }
    
    public void warning(String warning) {
        warnings.add(warning);
        if (listener != null) {
            listener.onWarning(warning);
        }
    }
    
    public void phaseStarted(String phase) {
        log("Starting: " + phase);
        if (listener != null) {
            listener.onPhaseStarted(phase);
        }
    }
    
    public void phaseCompleted(String phase) {
        log("Completed: " + phase);
        if (listener != null) {
            listener.onPhaseCompleted(phase);
        }
    }
    
    public void progress(int progress, String message) {
        if (listener != null) {
            listener.onProgress(progress, message);
        }
    }
    
    public void cancel() {
        this.cancelled = true;
    }
    
    public boolean isCancelled() {
        return cancelled;
    }
    
    public List<String> getLogs() { return new ArrayList<>(logs); }
    public List<String> getErrors() { return new ArrayList<>(errors); }
    public List<String> getWarnings() { return new ArrayList<>(warnings); }
    
    public File getClassesDir() {
        return new File(config.getIntermediatesDir(), "classes");
    }
    
    public File getDexDir() {
        return new File(config.getIntermediatesDir(), "dex");
    }
    
    public File getResourcesApk() {
        return new File(config.getIntermediatesDir(), "resources.ap_");
    }
    
    public File getMergedManifest() {
        return new File(config.getIntermediatesDir(), "merged_manifest/AndroidManifest.xml");
    }
    
    public File getUnsignedApk() {
        return new File(config.getIntermediatesDir(), "unsigned.apk");
    }
    
    public File getAlignedApk() {
        return new File(config.getIntermediatesDir(), "aligned.apk");
    }
    
    public File getNativeLibsDir() {
        File nativeLibs = getArtifact("native_libs_dir", File.class);
        if (nativeLibs != null && nativeLibs.exists()) {
            return nativeLibs;
        }
        return config.getNativeLibsDir();
    }
    
    public boolean hasNativeLibs() {
        File nativeLibsDir = getNativeLibsDir();
        if (!nativeLibsDir.exists()) return false;
        
        String[] abis = {"arm64-v8a", "armeabi-v7a", "x86_64", "x86"};
        for (String abi : abis) {
            File abiDir = new File(nativeLibsDir, abi);
            if (abiDir.exists()) {
                File[] soFiles = abiDir.listFiles((dir, name) -> name.endsWith(".so"));
                if (soFiles != null && soFiles.length > 0) {
                    return true;
                }
            }
        }
        return false;
    }
    
    public void setDependencies(List<Dependency> dependencies) {
        this.dependencies = dependencies;
    }
    
    public List<Dependency> getDependencies() {
        return dependencies != null ? dependencies : new ArrayList<>();
    }
    
    public void addDependency(String groupId, String artifactId, String version) {
        if (dependencies == null) {
            dependencies = new ArrayList<>();
        }
        dependencies.add(new Dependency(groupId, artifactId, version));
    }
    
    public void setIncrementalCache(IncrementalBuildCache cache) {
        this.incrementalCache = cache;
    }
    
    public IncrementalBuildCache getIncrementalCache() {
        return incrementalCache;
    }
    
    public String getDependencyClasspath() {
        String classpath = getArtifact("dependency_classpath", String.class);
        return classpath != null ? classpath : "";
    }
    
    @SuppressWarnings("unchecked")
    public List<File> getDependencyJars() {
        List<File> jars = getArtifact("dependency_jars", List.class);
        return jars != null ? jars : new ArrayList<>();
    }
}
