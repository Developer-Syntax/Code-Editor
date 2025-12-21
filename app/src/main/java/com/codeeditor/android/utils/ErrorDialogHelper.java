package com.codeeditor.android.utils;

import android.app.Activity;
import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;

import java.io.PrintWriter;
import java.io.StringWriter;

public class ErrorDialogHelper {
    
    private static final String TAG = "ErrorDialogHelper";
    private static final Handler mainHandler = new Handler(Looper.getMainLooper());
    
    public static void showError(Context context, String title, String message) {
        showError(context, title, message, null);
    }
    
    public static void showError(Context context, String title, String message, Throwable throwable) {
        mainHandler.post(() -> {
            try {
                if (context instanceof Activity) {
                    Activity activity = (Activity) context;
                    if (activity.isFinishing() || activity.isDestroyed()) {
                        Log.w(TAG, "Cannot show dialog - activity is finishing or destroyed");
                        return;
                    }
                }
                
                String fullMessage = message;
                if (throwable != null) {
                    fullMessage += "\n\nError details: " + getShortErrorMessage(throwable);
                }
                
                AlertDialog.Builder builder = new AlertDialog.Builder(context);
                builder.setTitle(title);
                builder.setMessage(fullMessage);
                builder.setPositiveButton("OK", null);
                
                if (throwable != null) {
                    final String stackTrace = getStackTraceString(throwable);
                    builder.setNeutralButton("View Details", (dialog, which) -> {
                        showDetailedError(context, stackTrace);
                    });
                }
                
                builder.create().show();
                
            } catch (Exception e) {
                Log.e(TAG, "Failed to show error dialog", e);
                try {
                    Toast.makeText(context, message, Toast.LENGTH_LONG).show();
                } catch (Exception e2) {
                    Log.e(TAG, "Failed to show toast", e2);
                }
            }
        });
    }
    
    public static void showWarning(Context context, String title, String message) {
        mainHandler.post(() -> {
            try {
                if (context instanceof Activity) {
                    Activity activity = (Activity) context;
                    if (activity.isFinishing() || activity.isDestroyed()) {
                        return;
                    }
                }
                
                new AlertDialog.Builder(context)
                    .setTitle(title)
                    .setMessage(message)
                    .setPositiveButton("OK", null)
                    .setIcon(android.R.drawable.ic_dialog_alert)
                    .create()
                    .show();
                    
            } catch (Exception e) {
                Log.e(TAG, "Failed to show warning dialog", e);
            }
        });
    }
    
    public static void showInfo(Context context, String title, String message) {
        mainHandler.post(() -> {
            try {
                if (context instanceof Activity) {
                    Activity activity = (Activity) context;
                    if (activity.isFinishing() || activity.isDestroyed()) {
                        return;
                    }
                }
                
                new AlertDialog.Builder(context)
                    .setTitle(title)
                    .setMessage(message)
                    .setPositiveButton("OK", null)
                    .setIcon(android.R.drawable.ic_dialog_info)
                    .create()
                    .show();
                    
            } catch (Exception e) {
                Log.e(TAG, "Failed to show info dialog", e);
            }
        });
    }
    
    public static void showConfirmation(Context context, String title, String message, 
            Runnable onConfirm, Runnable onCancel) {
        mainHandler.post(() -> {
            try {
                if (context instanceof Activity) {
                    Activity activity = (Activity) context;
                    if (activity.isFinishing() || activity.isDestroyed()) {
                        return;
                    }
                }
                
                new AlertDialog.Builder(context)
                    .setTitle(title)
                    .setMessage(message)
                    .setPositiveButton("Yes", (dialog, which) -> {
                        if (onConfirm != null) onConfirm.run();
                    })
                    .setNegativeButton("No", (dialog, which) -> {
                        if (onCancel != null) onCancel.run();
                    })
                    .create()
                    .show();
                    
            } catch (Exception e) {
                Log.e(TAG, "Failed to show confirmation dialog", e);
            }
        });
    }
    
    private static void showDetailedError(Context context, String stackTrace) {
        try {
            android.widget.ScrollView scrollView = new android.widget.ScrollView(context);
            android.widget.TextView textView = new android.widget.TextView(context);
            textView.setText(stackTrace);
            textView.setPadding(32, 32, 32, 32);
            textView.setTextIsSelectable(true);
            textView.setTypeface(android.graphics.Typeface.MONOSPACE);
            textView.setTextSize(10);
            scrollView.addView(textView);
            
            new AlertDialog.Builder(context)
                .setTitle("Error Details")
                .setView(scrollView)
                .setPositiveButton("Close", null)
                .setNeutralButton("Copy", (dialog, which) -> {
                    copyToClipboard(context, stackTrace);
                })
                .create()
                .show();
                
        } catch (Exception e) {
            Log.e(TAG, "Failed to show detailed error", e);
        }
    }
    
    private static void copyToClipboard(Context context, String text) {
        try {
            android.content.ClipboardManager clipboard = 
                    (android.content.ClipboardManager) context.getSystemService(Context.CLIPBOARD_SERVICE);
            if (clipboard != null) {
                android.content.ClipData clip = android.content.ClipData.newPlainText("Error", text);
                clipboard.setPrimaryClip(clip);
                Toast.makeText(context, "Copied to clipboard", Toast.LENGTH_SHORT).show();
            }
        } catch (Exception e) {
            Log.e(TAG, "Failed to copy to clipboard", e);
        }
    }
    
    private static String getShortErrorMessage(Throwable throwable) {
        String className = throwable.getClass().getSimpleName();
        String message = throwable.getMessage();
        
        if (message != null && !message.isEmpty()) {
            return className + ": " + message;
        }
        return className;
    }
    
    private static String getStackTraceString(Throwable throwable) {
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        throwable.printStackTrace(pw);
        return sw.toString();
    }
    
    public static void safeExecute(Context context, Runnable action, String errorTitle) {
        try {
            action.run();
        } catch (Exception e) {
            Log.e(TAG, "Error in safe execute", e);
            showError(context, errorTitle, "An error occurred: " + getShortErrorMessage(e), e);
        }
    }
    
    public static <T> T safeExecuteWithResult(Context context, java.util.concurrent.Callable<T> action, 
            String errorTitle, T defaultValue) {
        try {
            return action.call();
        } catch (Exception e) {
            Log.e(TAG, "Error in safe execute with result", e);
            showError(context, errorTitle, "An error occurred: " + getShortErrorMessage(e), e);
            return defaultValue;
        }
    }
}
