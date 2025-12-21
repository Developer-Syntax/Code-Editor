package com.codeeditor.android;

import android.app.Activity;
import android.app.Application;
import android.os.Bundle;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

public class CodeEditorApplication extends Application implements Application.ActivityLifecycleCallbacks {
    
    private static final String TAG = "CodeEditorApp";
    
    private GlobalExceptionHandler exceptionHandler;
    private Activity currentActivity;
    
    @Override
    public void onCreate() {
        super.onCreate();
        
        setupExceptionHandler();
        
        registerActivityLifecycleCallbacks(this);
        
        Log.i(TAG, "Application initialized");
    }
    
    private void setupExceptionHandler() {
        exceptionHandler = new GlobalExceptionHandler(this);
        Thread.setDefaultUncaughtExceptionHandler(exceptionHandler);
        Log.i(TAG, "Global exception handler installed");
    }
    
    public Activity getCurrentActivity() {
        return currentActivity;
    }
    
    @Override
    public void onActivityCreated(@NonNull Activity activity, @Nullable Bundle savedInstanceState) {
    }
    
    @Override
    public void onActivityStarted(@NonNull Activity activity) {
    }
    
    @Override
    public void onActivityResumed(@NonNull Activity activity) {
        currentActivity = activity;
        if (exceptionHandler != null) {
            exceptionHandler.setCurrentActivity(activity);
        }
    }
    
    @Override
    public void onActivityPaused(@NonNull Activity activity) {
        if (currentActivity == activity) {
            currentActivity = null;
        }
    }
    
    @Override
    public void onActivityStopped(@NonNull Activity activity) {
    }
    
    @Override
    public void onActivitySaveInstanceState(@NonNull Activity activity, @NonNull Bundle outState) {
    }
    
    @Override
    public void onActivityDestroyed(@NonNull Activity activity) {
        if (currentActivity == activity) {
            currentActivity = null;
        }
    }
}
