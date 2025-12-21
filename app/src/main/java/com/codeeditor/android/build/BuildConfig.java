package com.codeeditor.android.build;

import java.io.File;

public class BuildConfig {
    private String projectName;
    private String packageName;
    private int minSdkVersion;
    private int targetSdkVersion;
    private int versionCode;
    private String versionName;
    private File projectDir;
    private File outputDir;
    private boolean debugBuild;
    private String mainActivity;
    private boolean enableNdk;
    private String[] ndkAbiFilters;
    
    public BuildConfig() {
        this.minSdkVersion = 26;
        this.targetSdkVersion = 34;
        this.versionCode = 1;
        this.versionName = "1.0";
        this.debugBuild = true;
        this.enableNdk = true;
        this.ndkAbiFilters = new String[]{"arm64-v8a", "armeabi-v7a"};
    }
    
    public static class Builder {
        private BuildConfig config;
        
        public Builder() {
            config = new BuildConfig();
        }
        
        public Builder setProjectName(String name) {
            config.projectName = name;
            return this;
        }
        
        public Builder setPackageName(String packageName) {
            config.packageName = packageName;
            return this;
        }
        
        public Builder setMinSdkVersion(int version) {
            config.minSdkVersion = version;
            return this;
        }
        
        public Builder setTargetSdkVersion(int version) {
            config.targetSdkVersion = version;
            return this;
        }
        
        public Builder setVersionCode(int code) {
            config.versionCode = code;
            return this;
        }
        
        public Builder setVersionName(String name) {
            config.versionName = name;
            return this;
        }
        
        public Builder setProjectDir(File dir) {
            config.projectDir = dir;
            return this;
        }
        
        public Builder setOutputDir(File dir) {
            config.outputDir = dir;
            return this;
        }
        
        public Builder setDebugBuild(boolean debug) {
            config.debugBuild = debug;
            return this;
        }
        
        public Builder setMainActivity(String activity) {
            config.mainActivity = activity;
            return this;
        }
        
        public Builder setEnableNdk(boolean enable) {
            config.enableNdk = enable;
            return this;
        }
        
        public Builder setNdkAbiFilters(String[] abiFilters) {
            config.ndkAbiFilters = abiFilters;
            return this;
        }
        
        public BuildConfig build() {
            if (config.projectName == null || config.projectName.isEmpty()) {
                throw new IllegalStateException("Project name is required");
            }
            if (config.packageName == null || config.packageName.isEmpty()) {
                throw new IllegalStateException("Package name is required");
            }
            if (config.projectDir == null) {
                throw new IllegalStateException("Project directory is required");
            }
            return config;
        }
    }
    
    public String getProjectName() { return projectName; }
    public String getPackageName() { return packageName; }
    public int getMinSdkVersion() { return minSdkVersion; }
    public int getTargetSdkVersion() { return targetSdkVersion; }
    public int getVersionCode() { return versionCode; }
    public String getVersionName() { return versionName; }
    public File getProjectDir() { return projectDir; }
    public File getOutputDir() { return outputDir; }
    public boolean isDebugBuild() { return debugBuild; }
    public String getMainActivity() { return mainActivity; }
    public boolean isNdkEnabled() { return enableNdk; }
    public String[] getNdkAbiFilters() { return ndkAbiFilters; }
    
    public File getSourceDir() {
        return new File(projectDir, "src/main/java");
    }
    
    public File getResDir() {
        return new File(projectDir, "src/main/res");
    }
    
    public File getManifestFile() {
        return new File(projectDir, "src/main/AndroidManifest.xml");
    }
    
    public File getBuildDir() {
        return new File(projectDir, "build");
    }
    
    public File getIntermediatesDir() {
        return new File(getBuildDir(), "intermediates");
    }
    
    public File getJniDir() {
        return new File(projectDir, "src/main/jni");
    }
    
    public File getCppDir() {
        return new File(projectDir, "src/main/cpp");
    }
    
    public File getNativeLibsDir() {
        return new File(getIntermediatesDir(), "native_libs");
    }
    
    public File getOutputApk() {
        String apkName = projectName + (debugBuild ? "-debug" : "-release") + ".apk";
        return new File(getOutputDir() != null ? getOutputDir() : new File(getBuildDir(), "outputs"), apkName);
    }
}
