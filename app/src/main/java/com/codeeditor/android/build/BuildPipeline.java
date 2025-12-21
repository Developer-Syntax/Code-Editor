package com.codeeditor.android.build;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;

import com.codeeditor.android.build.tasks.CompileJavaTask;
import com.codeeditor.android.build.tasks.CompileKotlinTask;
import com.codeeditor.android.build.tasks.CompileNativeTask;
import com.codeeditor.android.build.tasks.DexTask;
import com.codeeditor.android.build.tasks.PackageApkTask;
import com.codeeditor.android.build.tasks.ProcessResourcesTask;
import com.codeeditor.android.build.tasks.R8OptimizeTask;
import com.codeeditor.android.build.tasks.ResolveDependenciesTask;
import com.codeeditor.android.build.tasks.SignApkTask;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

public class BuildPipeline {
    
    private Context context;
    private List<BuildTask> tasks;
    private ExecutorService executor;
    private Handler mainHandler;
    private AtomicBoolean cancelled;
    private BuildListener listener;
    
    public interface BuildListener {
        void onBuildStarted();
        void onTaskStarted(String taskName, int taskIndex, int totalTasks);
        void onTaskCompleted(String taskName, int taskIndex, int totalTasks);
        void onTaskFailed(String taskName, String error);
        void onProgress(int overallProgress, String message);
        void onBuildCompleted(BuildResult result);
        void onLog(String message);
    }
    
    public BuildPipeline(Context context) {
        this.context = context;
        this.tasks = new ArrayList<>();
        this.executor = Executors.newSingleThreadExecutor();
        this.mainHandler = new Handler(Looper.getMainLooper());
        this.cancelled = new AtomicBoolean(false);
    }
    
    public void addTask(BuildTask task) {
        tasks.add(task);
    }
    
    public void clearTasks() {
        tasks.clear();
    }
    
    public void setListener(BuildListener listener) {
        this.listener = listener;
    }
    
    public void execute(BuildConfig config) {
        if (tasks.isEmpty()) {
            notifyCompleted(BuildResult.failed("No build tasks configured"));
            return;
        }
        
        cancelled.set(false);
        
        executor.execute(() -> {
            long startTime = System.currentTimeMillis();
            
            notifyBuildStarted();
            
            BuildContext buildContext = new BuildContext(context, config);
            
            File sdkRoot = new File(context.getFilesDir(), "android-sdk");
            SdkManager sdkManager = new SdkManager(context, sdkRoot);
            buildContext.setSdkManager(sdkManager);
            buildContext.setSdkDir(sdkRoot);
            
            IncrementalBuildCache incrementalCache = new IncrementalBuildCache(
                context, config.getProjectDir().getAbsolutePath());
            buildContext.setIncrementalCache(incrementalCache);
            
            if (!sdkManager.isToolsInstalled()) {
                notifyLog("SDK tools not found, installing...");
                final Object lock = new Object();
                final boolean[] installComplete = {false};
                final boolean[] installSuccess = {false};
                final String[] installError = {null};
                
                sdkManager.setListener(new SdkManager.DownloadListener() {
                    @Override
                    public void onProgress(int progress, String message) {
                        notifyProgress(progress / 10, "SDK: " + message);
                    }
                    
                    @Override
                    public void onCompleted(boolean success, String message) {
                        synchronized (lock) {
                            installComplete[0] = true;
                            installSuccess[0] = success;
                            lock.notifyAll();
                        }
                    }
                    
                    @Override
                    public void onError(String error) {
                        synchronized (lock) {
                            installComplete[0] = true;
                            installError[0] = error;
                            lock.notifyAll();
                        }
                    }
                });
                
                sdkManager.downloadAndInstallSdk();
                
                synchronized (lock) {
                    long timeout = System.currentTimeMillis() + 600000;
                    while (!installComplete[0] && installError[0] == null) {
                        long remaining = timeout - System.currentTimeMillis();
                        if (remaining <= 0) {
                            notifyCompleted(BuildResult.failed("SDK installation timed out"));
                            return;
                        }
                        try {
                            lock.wait(remaining);
                        } catch (InterruptedException e) {
                            notifyCompleted(BuildResult.failed("SDK installation interrupted"));
                            return;
                        }
                    }
                }
                
                if (installError[0] != null) {
                    notifyCompleted(BuildResult.failed("SDK installation failed: " + installError[0]));
                    return;
                }
                
                if (!sdkManager.isToolsInstalled()) {
                    notifyCompleted(BuildResult.failed("SDK installation completed but required tools were not found. Please check network connection and try again."));
                    return;
                }
            }
            
            buildContext.setListener(new BuildContext.BuildListener() {
                @Override
                public void onPhaseStarted(String phase) {
                    notifyLog("Phase started: " + phase);
                }
                
                @Override
                public void onPhaseCompleted(String phase) {
                    notifyLog("Phase completed: " + phase);
                }
                
                @Override
                public void onProgress(int progress, String message) {
                    notifyProgress(progress, message);
                }
                
                @Override
                public void onLog(String message) {
                    notifyLog(message);
                }
                
                @Override
                public void onError(String error) {
                    notifyLog("ERROR: " + error);
                }
                
                @Override
                public void onWarning(String warning) {
                    notifyLog("WARNING: " + warning);
                }
            });
            
            BuildResult result = null;
            int taskIndex = 0;
            
            for (BuildTask task : tasks) {
                if (cancelled.get()) {
                    result = BuildResult.cancelled();
                    break;
                }
                
                notifyTaskStarted(task.getName(), taskIndex, tasks.size());
                
                try {
                    boolean success = task.execute(buildContext);
                    
                    if (!success) {
                        result = BuildResult.failed("Task failed: " + task.getName(), buildContext.getErrors());
                        notifyTaskFailed(task.getName(), "Task execution returned false");
                        break;
                    }
                    
                    notifyTaskCompleted(task.getName(), taskIndex, tasks.size());
                    
                } catch (BuildException e) {
                    result = BuildResult.failed(e.getMessage(), buildContext.getErrors());
                    notifyTaskFailed(task.getName(), e.getMessage());
                    break;
                } catch (Exception e) {
                    result = BuildResult.failed("Unexpected error: " + e.getMessage(), buildContext.getErrors());
                    notifyTaskFailed(task.getName(), e.getMessage());
                    break;
                }
                
                taskIndex++;
                int progress = (int) ((taskIndex * 100.0) / tasks.size());
                notifyProgress(progress, "Completed " + taskIndex + "/" + tasks.size() + " tasks");
            }
            
            if (result == null) {
                long buildTime = System.currentTimeMillis() - startTime;
                File outputApk = config.getOutputApk();
                
                if (outputApk.exists()) {
                    result = BuildResult.success(outputApk, buildTime);
                } else {
                    result = BuildResult.failed("Build completed but APK not found");
                }
            }
            
            for (String warning : buildContext.getWarnings()) {
                result.addWarning(warning);
            }
            
            notifyCompleted(result);
        });
    }
    
    public void cancel() {
        cancelled.set(true);
        for (BuildTask task : tasks) {
            task.cancel();
        }
    }
    
    private void notifyBuildStarted() {
        if (listener != null) {
            mainHandler.post(() -> listener.onBuildStarted());
        }
    }
    
    private void notifyTaskStarted(String taskName, int index, int total) {
        if (listener != null) {
            mainHandler.post(() -> listener.onTaskStarted(taskName, index, total));
        }
    }
    
    private void notifyTaskCompleted(String taskName, int index, int total) {
        if (listener != null) {
            mainHandler.post(() -> listener.onTaskCompleted(taskName, index, total));
        }
    }
    
    private void notifyTaskFailed(String taskName, String error) {
        if (listener != null) {
            mainHandler.post(() -> listener.onTaskFailed(taskName, error));
        }
    }
    
    private void notifyProgress(int progress, String message) {
        if (listener != null) {
            mainHandler.post(() -> listener.onProgress(progress, message));
        }
    }
    
    private void notifyCompleted(BuildResult result) {
        if (listener != null) {
            mainHandler.post(() -> listener.onBuildCompleted(result));
        }
    }
    
    private void notifyLog(String message) {
        if (listener != null) {
            mainHandler.post(() -> listener.onLog(message));
        }
    }
    
    public void shutdown() {
        executor.shutdown();
    }
    
    public static BuildPipeline createStandardPipeline(Context context) {
        BuildPipeline pipeline = new BuildPipeline(context);
        
        pipeline.addTask(new ResolveDependenciesTask());
        pipeline.addTask(new CompileNativeTask());
        pipeline.addTask(new CompileKotlinTask());
        pipeline.addTask(new CompileJavaTask());
        pipeline.addTask(new ProcessResourcesTask());
        pipeline.addTask(new DexTask());
        pipeline.addTask(new R8OptimizeTask());
        pipeline.addTask(new PackageApkTask());
        pipeline.addTask(new SignApkTask());
        
        return pipeline;
    }
    
    public static BuildPipeline createDebugPipeline(Context context) {
        BuildPipeline pipeline = new BuildPipeline(context);
        
        pipeline.addTask(new ResolveDependenciesTask());
        pipeline.addTask(new CompileNativeTask());
        pipeline.addTask(new CompileKotlinTask());
        pipeline.addTask(new CompileJavaTask());
        pipeline.addTask(new ProcessResourcesTask());
        pipeline.addTask(new DexTask());
        pipeline.addTask(new PackageApkTask());
        pipeline.addTask(new SignApkTask());
        
        return pipeline;
    }
    
    public static BuildPipeline createJavaOnlyPipeline(Context context) {
        BuildPipeline pipeline = new BuildPipeline(context);
        
        pipeline.addTask(new ResolveDependenciesTask());
        pipeline.addTask(new CompileJavaTask());
        pipeline.addTask(new ProcessResourcesTask());
        pipeline.addTask(new DexTask());
        pipeline.addTask(new PackageApkTask());
        pipeline.addTask(new SignApkTask());
        
        return pipeline;
    }
    
    public static BuildPipeline createNativePipeline(Context context) {
        BuildPipeline pipeline = new BuildPipeline(context);
        
        pipeline.addTask(new ResolveDependenciesTask());
        pipeline.addTask(new CompileNativeTask());
        pipeline.addTask(new CompileKotlinTask());
        pipeline.addTask(new CompileJavaTask());
        pipeline.addTask(new ProcessResourcesTask());
        pipeline.addTask(new DexTask());
        pipeline.addTask(new PackageApkTask());
        pipeline.addTask(new SignApkTask());
        
        return pipeline;
    }
}
