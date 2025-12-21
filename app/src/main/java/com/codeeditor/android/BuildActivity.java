package com.codeeditor.android;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.codeeditor.android.build.ApkInstaller;
import com.codeeditor.android.build.BuildConfig;
import com.codeeditor.android.build.BuildResult;
import com.codeeditor.android.build.BuildService;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.progressindicator.LinearProgressIndicator;

import java.io.File;

public class BuildActivity extends AppCompatActivity {
    
    private BuildService buildService;
    private File projectDir;
    private File builtApk;
    private boolean isBuilding = false;
    private Handler mainHandler;
    private StringBuilder logBuilder;
    
    private MaterialToolbar toolbar;
    private TextView projectNameText;
    private TextView packageNameText;
    private TextView sdkVersionText;
    private LinearProgressIndicator buildProgress;
    private TextView buildStatus;
    private TextView buildLog;
    private ScrollView logScrollView;
    private MaterialButton clearLogButton;
    private MaterialCardView outputCard;
    private TextView apkName;
    private TextView apkSize;
    private MaterialButton installButton;
    private MaterialButton cancelButton;
    private MaterialButton buildButton;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        try {
            setContentView(R.layout.activity_build);
            
            mainHandler = new Handler(Looper.getMainLooper());
            logBuilder = new StringBuilder();
            
            initViews();
            initBuildService();
            loadProjectInfo();
        } catch (Exception e) {
            android.util.Log.e("BuildActivity", "Error in onCreate", e);
            showErrorDialog("Build Error", "Failed to initialize build activity: " + e.getMessage(), e);
        }
    }
    
    private void showErrorDialog(String title, String message, Throwable error) {
        try {
            String errorDetails = message;
            if (error != null && error.getMessage() != null) {
                errorDetails += "\n\nDetails: " + error.getClass().getSimpleName() + ": " + error.getMessage();
            }
            
            new androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle(title)
                .setMessage(errorDetails)
                .setPositiveButton("OK", null)
                .setNeutralButton("View Details", (dialog, which) -> {
                    if (error != null) {
                        showFullErrorDetails(error);
                    }
                })
                .show();
        } catch (Exception e) {
            Toast.makeText(this, message, Toast.LENGTH_LONG).show();
        }
    }
    
    private void showFullErrorDetails(Throwable error) {
        try {
            java.io.StringWriter sw = new java.io.StringWriter();
            java.io.PrintWriter pw = new java.io.PrintWriter(sw);
            error.printStackTrace(pw);
            String stackTrace = sw.toString();
            
            android.widget.ScrollView scrollView = new android.widget.ScrollView(this);
            android.widget.TextView textView = new android.widget.TextView(this);
            textView.setText(stackTrace);
            textView.setPadding(32, 32, 32, 32);
            textView.setTextIsSelectable(true);
            textView.setTypeface(android.graphics.Typeface.MONOSPACE);
            textView.setTextSize(10);
            scrollView.addView(textView);
            
            new androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("Error Details")
                .setView(scrollView)
                .setPositiveButton("Close", null)
                .setNeutralButton("Copy", (dialog, which) -> {
                    android.content.ClipboardManager clipboard = 
                            (android.content.ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
                    if (clipboard != null) {
                        android.content.ClipData clip = android.content.ClipData.newPlainText("Error", stackTrace);
                        clipboard.setPrimaryClip(clip);
                        Toast.makeText(this, "Copied to clipboard", Toast.LENGTH_SHORT).show();
                    }
                })
                .show();
        } catch (Exception e) {
            Toast.makeText(this, "Error: " + error.getMessage(), Toast.LENGTH_LONG).show();
        }
    }
    
    private void initViews() {
        toolbar = findViewById(R.id.toolbar);
        projectNameText = findViewById(R.id.projectName);
        packageNameText = findViewById(R.id.packageName);
        sdkVersionText = findViewById(R.id.sdkVersion);
        buildProgress = findViewById(R.id.buildProgress);
        buildStatus = findViewById(R.id.buildStatus);
        buildLog = findViewById(R.id.buildLog);
        logScrollView = findViewById(R.id.logScrollView);
        clearLogButton = findViewById(R.id.clearLogButton);
        outputCard = findViewById(R.id.outputCard);
        apkName = findViewById(R.id.apkName);
        apkSize = findViewById(R.id.apkSize);
        installButton = findViewById(R.id.installButton);
        cancelButton = findViewById(R.id.cancelButton);
        buildButton = findViewById(R.id.buildButton);
        
        toolbar.setNavigationOnClickListener(v -> finish());
        
        clearLogButton.setOnClickListener(v -> {
            logBuilder.setLength(0);
            buildLog.setText("Build log cleared.");
        });
        
        buildButton.setOnClickListener(v -> startBuild());
        cancelButton.setOnClickListener(v -> cancelBuild());
        installButton.setOnClickListener(v -> installApk());
    }
    
    private void initBuildService() {
        buildService = new BuildService(this);
        
        buildService.setListener(new BuildService.BuildListener() {
            @Override
            public void onBuildStarted() {
                runOnUiThread(() -> {
                    isBuilding = true;
                    updateBuildUI();
                    appendLog("Build started...\n");
                });
            }
            
            @Override
            public void onBuildProgress(int progress, String message) {
                runOnUiThread(() -> {
                    buildProgress.setProgress(progress);
                    buildStatus.setText(message);
                });
            }
            
            @Override
            public void onBuildLog(String message) {
                runOnUiThread(() -> appendLog(message + "\n"));
            }
            
            @Override
            public void onBuildCompleted(BuildResult result) {
                runOnUiThread(() -> {
                    isBuilding = false;
                    updateBuildUI();
                    handleBuildResult(result);
                });
            }
            
            @Override
            public void onBuildError(String error) {
                runOnUiThread(() -> appendLog("ERROR: " + error + "\n"));
            }
        });
    }
    
    private void loadProjectInfo() {
        String projectPath = getIntent().getStringExtra("project_path");
        
        if (projectPath != null) {
            projectDir = new File(projectPath);
            if (projectDir.exists()) {
                loadProjectConfig();
                return;
            }
        }
        
        projectNameText.setText("No project selected");
        packageNameText.setText("-");
        buildButton.setEnabled(false);
    }
    
    private void loadProjectConfig() {
        try {
            File configFile = new File(projectDir, "project.json");
            if (configFile.exists()) {
                String content = readFile(configFile);
                
                String name = extractJsonValue(content, "name");
                String pkg = extractJsonValue(content, "package");
                String minSdk = extractJsonValue(content, "minSdk");
                String targetSdk = extractJsonValue(content, "targetSdk");
                
                projectNameText.setText(name != null ? name : projectDir.getName());
                packageNameText.setText(pkg != null ? pkg : "-");
                sdkVersionText.setText("Min: " + (minSdk != null ? minSdk : "26") + 
                    ", Target: " + (targetSdk != null ? targetSdk : "34"));
                
                buildButton.setEnabled(true);
            } else {
                projectNameText.setText(projectDir.getName());
                packageNameText.setText("-");
                buildButton.setEnabled(true);
            }
        } catch (Exception e) {
            projectNameText.setText(projectDir.getName());
            appendLog("Warning: Could not load project config: " + e.getMessage() + "\n");
        }
    }
    
    private String readFile(File file) throws Exception {
        StringBuilder content = new StringBuilder();
        try (java.io.FileReader reader = new java.io.FileReader(file)) {
            char[] buffer = new char[1024];
            int read;
            while ((read = reader.read(buffer)) != -1) {
                content.append(buffer, 0, read);
            }
        }
        return content.toString();
    }
    
    private String extractJsonValue(String json, String key) {
        String pattern = "\"" + key + "\"\\s*:\\s*\"?([^,\"\\}]+)\"?";
        java.util.regex.Pattern p = java.util.regex.Pattern.compile(pattern);
        java.util.regex.Matcher m = p.matcher(json);
        if (m.find()) {
            return m.group(1).trim();
        }
        return null;
    }
    
    private void startBuild() {
        if (projectDir == null) {
            Toast.makeText(this, "No project selected", Toast.LENGTH_SHORT).show();
            return;
        }
        
        logBuilder.setLength(0);
        buildLog.setText("");
        outputCard.setVisibility(View.GONE);
        buildProgress.setProgress(0);
        
        buildService.buildProject(projectDir);
    }
    
    private void cancelBuild() {
        buildService.cancelBuild();
        appendLog("Build cancelled by user.\n");
    }
    
    private void updateBuildUI() {
        buildButton.setEnabled(!isBuilding);
        cancelButton.setEnabled(isBuilding);
        
        if (isBuilding) {
            buildProgress.setIndeterminate(false);
        }
    }
    
    private void handleBuildResult(BuildResult result) {
        if (result.isSuccess()) {
            buildProgress.setProgress(100);
            buildStatus.setText("Build successful!");
            
            builtApk = result.getOutputApk();
            if (builtApk != null && builtApk.exists()) {
                outputCard.setVisibility(View.VISIBLE);
                apkName.setText(builtApk.getName());
                apkSize.setText(formatFileSize(builtApk.length()));
            }
            
            appendLog("\n" + result.getSummary() + "\n");
            
        } else if (result.isCancelled()) {
            buildStatus.setText("Build cancelled");
            appendLog("\nBuild was cancelled.\n");
            
        } else {
            buildStatus.setText("Build failed");
            buildProgress.setProgress(0);
            appendLog("\n" + result.getSummary() + "\n");
        }
    }
    
    private void appendLog(String message) {
        logBuilder.append(message);
        buildLog.setText(logBuilder.toString());
        
        logScrollView.post(() -> logScrollView.fullScroll(View.FOCUS_DOWN));
    }
    
    private void installApk() {
        if (builtApk == null || !builtApk.exists()) {
            Toast.makeText(this, "APK file not found", Toast.LENGTH_SHORT).show();
            return;
        }
        
        buildService.installBuiltApk(builtApk, new ApkInstaller.InstallCallback() {
            @Override
            public void onSuccess() {
                runOnUiThread(() -> 
                    Toast.makeText(BuildActivity.this, "Installation started", Toast.LENGTH_SHORT).show());
            }
            
            @Override
            public void onError(String message) {
                runOnUiThread(() -> 
                    Toast.makeText(BuildActivity.this, "Install error: " + message, Toast.LENGTH_LONG).show());
            }
            
            @Override
            public void onPermissionRequired() {
                runOnUiThread(() -> {
                    Toast.makeText(BuildActivity.this, 
                        "Please enable 'Install from unknown sources' permission", Toast.LENGTH_LONG).show();
                    buildService.getInstaller().requestInstallPermission();
                });
            }
        });
    }
    
    private String formatFileSize(long bytes) {
        if (bytes < 1024) return bytes + " B";
        if (bytes < 1024 * 1024) return String.format("%.1f KB", bytes / 1024.0);
        return String.format("%.2f MB", bytes / (1024.0 * 1024.0));
    }
    
    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (buildService != null) {
            buildService.shutdown();
        }
    }
}
