package com.codeeditor.android.build;

import android.content.Context;

import com.codeeditor.android.build.tasks.CompileJavaTask;
import com.codeeditor.android.build.tasks.DexTask;
import com.codeeditor.android.build.tasks.PackageApkTask;
import com.codeeditor.android.build.tasks.ProcessResourcesTask;
import com.codeeditor.android.build.tasks.SignApkTask;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;

public class BuildService {
    
    private Context context;
    private BuildWorkspace workspace;
    private SdkManager sdkManager;
    private BuildPipeline pipeline;
    private ApkInstaller installer;
    private BuildListener listener;
    
    public interface BuildListener {
        void onBuildStarted();
        void onBuildProgress(int progress, String message);
        void onBuildLog(String message);
        void onBuildCompleted(BuildResult result);
        void onBuildError(String error);
    }
    
    public BuildService(Context context) {
        this.context = context;
        this.workspace = new BuildWorkspace(context);
        this.sdkManager = new SdkManager(context, workspace.getSdkDir());
        this.pipeline = new BuildPipeline(context);
        this.installer = new ApkInstaller(context);
    }
    
    public void setListener(BuildListener listener) {
        this.listener = listener;
    }
    
    public BuildWorkspace getWorkspace() {
        return workspace;
    }
    
    public SdkManager getSdkManager() {
        return sdkManager;
    }
    
    public ApkInstaller getInstaller() {
        return installer;
    }
    
    public boolean isSdkReady() {
        return sdkManager.isToolsInstalled() && 
               sdkManager.isPlatformInstalled(34);
    }
    
    public void setupSdk(SdkManager.DownloadListener downloadListener) {
        sdkManager.setListener(downloadListener);
        sdkManager.downloadAndInstallSdk();
    }
    
    public void buildProject(File projectDir) {
        buildProject(projectDir, null);
    }
    
    public void buildProject(File projectDir, BuildConfig customConfig) {
        try {
            BuildConfig config = customConfig;
            
            if (config == null) {
                config = loadProjectConfig(projectDir);
            }
            
            executeBuild(config);
            
        } catch (Exception e) {
            if (listener != null) {
                listener.onBuildError("Failed to start build: " + e.getMessage());
            }
        }
    }
    
    private BuildConfig loadProjectConfig(File projectDir) throws IOException {
        File configFile = new File(projectDir, "project.json");
        
        if (!configFile.exists()) {
            throw new IOException("project.json not found in project directory");
        }
        
        StringBuilder content = new StringBuilder();
        try (FileReader reader = new FileReader(configFile)) {
            char[] buffer = new char[1024];
            int read;
            while ((read = reader.read(buffer)) != -1) {
                content.append(buffer, 0, read);
            }
        }
        
        String json = content.toString();
        
        String name = extractJsonString(json, "name");
        String packageName = extractJsonString(json, "package");
        int minSdk = extractJsonInt(json, "minSdk", 26);
        int targetSdk = extractJsonInt(json, "targetSdk", 34);
        int versionCode = extractJsonInt(json, "versionCode", 1);
        String versionName = extractJsonString(json, "versionName");
        String mainActivity = extractJsonString(json, "mainActivity");
        
        return new BuildConfig.Builder()
            .setProjectName(name)
            .setPackageName(packageName)
            .setMinSdkVersion(minSdk)
            .setTargetSdkVersion(targetSdk)
            .setVersionCode(versionCode)
            .setVersionName(versionName != null ? versionName : "1.0")
            .setMainActivity(mainActivity)
            .setProjectDir(projectDir)
            .setDebugBuild(true)
            .build();
    }
    
    private String extractJsonString(String json, String key) {
        String pattern = "\"" + key + "\"\\s*:\\s*\"([^\"]+)\"";
        java.util.regex.Pattern p = java.util.regex.Pattern.compile(pattern);
        java.util.regex.Matcher m = p.matcher(json);
        if (m.find()) {
            return m.group(1);
        }
        return null;
    }
    
    private int extractJsonInt(String json, String key, int defaultValue) {
        String pattern = "\"" + key + "\"\\s*:\\s*(\\d+)";
        java.util.regex.Pattern p = java.util.regex.Pattern.compile(pattern);
        java.util.regex.Matcher m = p.matcher(json);
        if (m.find()) {
            try {
                return Integer.parseInt(m.group(1));
            } catch (NumberFormatException e) {
                return defaultValue;
            }
        }
        return defaultValue;
    }
    
    private void executeBuild(BuildConfig config) {
        pipeline.clearTasks();
        
        pipeline.addTask(new CompileJavaTask());
        pipeline.addTask(new ProcessResourcesTask());
        pipeline.addTask(new DexTask());
        pipeline.addTask(new PackageApkTask());
        pipeline.addTask(new SignApkTask());
        
        pipeline.setListener(new BuildPipeline.BuildListener() {
            @Override
            public void onBuildStarted() {
                if (listener != null) {
                    listener.onBuildStarted();
                }
            }
            
            @Override
            public void onTaskStarted(String taskName, int taskIndex, int totalTasks) {
                if (listener != null) {
                    int progress = (taskIndex * 100) / totalTasks;
                    listener.onBuildProgress(progress, "Starting: " + taskName);
                }
            }
            
            @Override
            public void onTaskCompleted(String taskName, int taskIndex, int totalTasks) {
                if (listener != null) {
                    listener.onBuildLog("Completed: " + taskName);
                }
            }
            
            @Override
            public void onTaskFailed(String taskName, String error) {
                if (listener != null) {
                    listener.onBuildError("Task failed: " + taskName + " - " + error);
                }
            }
            
            @Override
            public void onProgress(int overallProgress, String message) {
                if (listener != null) {
                    listener.onBuildProgress(overallProgress, message);
                }
            }
            
            @Override
            public void onBuildCompleted(BuildResult result) {
                if (listener != null) {
                    listener.onBuildCompleted(result);
                }
            }
            
            @Override
            public void onLog(String message) {
                if (listener != null) {
                    listener.onBuildLog(message);
                }
            }
        });
        
        pipeline.execute(config);
    }
    
    public void cancelBuild() {
        pipeline.cancel();
    }
    
    public void installBuiltApk(File apkFile, ApkInstaller.InstallCallback callback) {
        installer.installApk(apkFile, callback);
    }
    
    public void createNewProject(String name, String packageName, ProjectCreationCallback callback) {
        new Thread(() -> {
            try {
                File projectDir = workspace.createProject(name, packageName);
                callback.onSuccess(projectDir);
            } catch (IOException e) {
                callback.onError(e.getMessage());
            }
        }).start();
    }
    
    public interface ProjectCreationCallback {
        void onSuccess(File projectDir);
        void onError(String message);
    }
    
    public void shutdown() {
        pipeline.shutdown();
        sdkManager.shutdown();
    }
}
