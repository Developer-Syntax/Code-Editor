package com.codeeditor.android;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.ref.WeakReference;

public class GlobalExceptionHandler implements Thread.UncaughtExceptionHandler {
    
    private static final String TAG = "GlobalExceptionHandler";
    
    private final Context applicationContext;
    private final Thread.UncaughtExceptionHandler defaultHandler;
    private WeakReference<Activity> currentActivityRef;
    
    public GlobalExceptionHandler(Context context) {
        this.applicationContext = context.getApplicationContext();
        this.defaultHandler = Thread.getDefaultUncaughtExceptionHandler();
    }
    
    public void setCurrentActivity(Activity activity) {
        this.currentActivityRef = new WeakReference<>(activity);
    }
    
    @Override
    public void uncaughtException(Thread thread, Throwable throwable) {
        try {
            Log.e(TAG, "Uncaught exception on thread: " + thread.getName(), throwable);
            
            final String errorMessage = getStackTraceString(throwable);
            final String shortMessage = getShortErrorMessage(throwable);
            
            saveErrorLog(errorMessage);
            
            launchErrorActivity(shortMessage, errorMessage);
            
        } catch (Exception e) {
            Log.e(TAG, "Error in exception handler", e);
        } finally {
            try {
                Thread.sleep(2000);
            } catch (InterruptedException ignored) {}
        }
    }
    
    private void launchErrorActivity(String shortMessage, String fullMessage) {
        try {
            Activity currentActivity = currentActivityRef != null ? currentActivityRef.get() : null;
            if (currentActivity != null && !currentActivity.isFinishing() && !currentActivity.isDestroyed()) {
                try {
                    currentActivity.finishAffinity();
                } catch (Exception e) {
                    Log.e(TAG, "Error finishing activity", e);
                }
            }
            
            Intent intent = new Intent(applicationContext, ErrorActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | 
                           Intent.FLAG_ACTIVITY_CLEAR_TASK | 
                           Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS);
            intent.putExtra("short_message", shortMessage);
            intent.putExtra("full_message", fullMessage);
            applicationContext.startActivity(intent);
            
        } catch (Exception e) {
            Log.e(TAG, "Failed to launch error activity", e);
        }
    }
    
    private String getStackTraceString(Throwable throwable) {
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        throwable.printStackTrace(pw);
        return sw.toString();
    }
    
    private String getShortErrorMessage(Throwable throwable) {
        StringBuilder message = new StringBuilder();
        
        Throwable cause = throwable;
        int depth = 0;
        while (cause != null && depth < 5) {
            String className = cause.getClass().getSimpleName();
            String msg = cause.getMessage();
            
            if (message.length() > 0) {
                message.append("\n\nCaused by: ");
            }
            
            message.append(className);
            if (msg != null && !msg.isEmpty()) {
                message.append(": ").append(msg);
            }
            
            StackTraceElement[] stack = cause.getStackTrace();
            if (stack != null && stack.length > 0) {
                StackTraceElement element = stack[0];
                message.append("\n  at ").append(element.getClassName())
                       .append(".").append(element.getMethodName())
                       .append("(").append(element.getFileName())
                       .append(":").append(element.getLineNumber()).append(")");
            }
            
            Throwable nextCause = cause.getCause();
            if (nextCause == cause) break;
            cause = nextCause;
            depth++;
        }
        
        return message.toString();
    }
    
    private void saveErrorLog(String errorMessage) {
        try {
            java.io.File logDir = new java.io.File(applicationContext.getFilesDir(), "crash_logs");
            if (!logDir.exists()) {
                logDir.mkdirs();
            }
            
            cleanOldLogs(logDir);
            
            String timestamp = new java.text.SimpleDateFormat("yyyy-MM-dd_HH-mm-ss", java.util.Locale.US)
                    .format(new java.util.Date());
            java.io.File logFile = new java.io.File(logDir, "crash_" + timestamp + ".log");
            
            java.io.FileWriter writer = new java.io.FileWriter(logFile);
            writer.write("=== Crash Report ===\n");
            writer.write("Timestamp: " + timestamp + "\n");
            writer.write("Android Version: " + Build.VERSION.SDK_INT + " (" + Build.VERSION.RELEASE + ")\n");
            writer.write("Device: " + Build.MANUFACTURER + " " + Build.MODEL + "\n");
            writer.write("App Version: " + getAppVersion() + "\n");
            writer.write("\n=== Stack Trace ===\n");
            writer.write(errorMessage);
            writer.close();
            
            Log.i(TAG, "Crash log saved to: " + logFile.getAbsolutePath());
        } catch (Exception e) {
            Log.e(TAG, "Failed to save error log", e);
        }
    }
    
    private void cleanOldLogs(java.io.File logDir) {
        try {
            java.io.File[] files = logDir.listFiles();
            if (files != null && files.length > 10) {
                java.util.Arrays.sort(files, (a, b) -> Long.compare(a.lastModified(), b.lastModified()));
                for (int i = 0; i < files.length - 10; i++) {
                    files[i].delete();
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Failed to clean old logs", e);
        }
    }
    
    private String getAppVersion() {
        try {
            return applicationContext.getPackageManager()
                    .getPackageInfo(applicationContext.getPackageName(), 0).versionName;
        } catch (Exception e) {
            return "Unknown";
        }
    }
}
