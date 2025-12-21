package com.codeeditor.android;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Typeface;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.os.PowerManager;
import android.util.Log;
import android.view.KeyEvent;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.core.content.ContextCompat;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.termux.terminal.TerminalEmulator;
import com.termux.terminal.TerminalSession;
import com.termux.terminal.TerminalSessionClient;
import com.termux.view.TerminalView;
import com.termux.view.TerminalViewClient;

import com.codeeditor.android.utils.TermuxBootstrap;

import java.io.File;

public class TerminalActivity extends AppCompatActivity implements TerminalViewClient, TerminalSessionClient {

    private static final String TAG = "TerminalActivity";
    private static final String WAKELOCK_TAG = "CodeEditor:TerminalWakeLock";
    
    private TerminalView terminalView;
    private TerminalSession terminalSession;
    private ProgressBar progressBar;
    private TextView progressText;
    private Handler mainHandler;
    private String workingDirectory;
    private int currentTextSize = 14;
    private boolean isSessionRunning = false;
    private String currentShellPath;
    private PowerManager.WakeLock wakeLock;
    
    private TermuxBootstrap termuxBootstrap;
    private boolean useTermuxEnvironment = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        try {
            setContentView(R.layout.activity_terminal);
            
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
            
            acquireWakeLock();

            mainHandler = new Handler(Looper.getMainLooper());

            Toolbar toolbar = findViewById(R.id.toolbar);
            setSupportActionBar(toolbar);
            if (getSupportActionBar() != null) {
                getSupportActionBar().setDisplayHomeAsUpEnabled(true);
                getSupportActionBar().setTitle("Terminal");
            }

            terminalView = findViewById(R.id.terminal_view);
            progressBar = findViewById(R.id.progress_bar);
            progressText = findViewById(R.id.progress_text);

            FloatingActionButton fabKeyboard = findViewById(R.id.fab_keyboard);
            if (fabKeyboard != null) {
                fabKeyboard.setOnClickListener(v -> toggleKeyboard());
            }

            workingDirectory = getIntent().getStringExtra("working_directory");
            if (workingDirectory == null || workingDirectory.isEmpty()) {
                workingDirectory = getFilesDir().getAbsolutePath();
            }

            if (!hasStoragePermission()) {
                showStorageWarning();
            }
            
            termuxBootstrap = new TermuxBootstrap(this);
            
            checkAndSetupBootstrap();
        } catch (Exception e) {
            Log.e(TAG, "Error in onCreate", e);
            showErrorDialog("Terminal Error", "Failed to initialize terminal: " + e.getMessage(), e);
        }
    }
    
    private void checkAndSetupBootstrap() {
        if (termuxBootstrap.isBootstrapInstalled()) {
            useTermuxEnvironment = true;
            setupTerminal();
        } else {
            showBootstrapInstallDialog();
        }
    }
    
    private void showBootstrapInstallDialog() {
        new AlertDialog.Builder(this)
            .setTitle("Terminal Setup")
            .setMessage("Untuk menggunakan package manager (pkg/apt), terminal perlu mengunduh dan menginstal Termux bootstrap (~50MB).\n\nDengan bootstrap, Anda dapat:\n• Install packages: pkg install python nodejs git\n• Kompilasi kode\n• Gunakan tools development\n\nInstal sekarang?")
            .setPositiveButton("Install", (dialog, which) -> {
                installBootstrap();
            })
            .setNegativeButton("Skip", (dialog, which) -> {
                useTermuxEnvironment = false;
                setupTerminal();
            })
            .setCancelable(false)
            .show();
    }
    
    private void installBootstrap() {
        progressBar.setVisibility(View.VISIBLE);
        if (progressText != null) {
            progressText.setVisibility(View.VISIBLE);
        }
        terminalView.setVisibility(View.GONE);
        
        termuxBootstrap.setListener(new TermuxBootstrap.BootstrapListener() {
            @Override
            public void onProgress(String message, int progress) {
                progressBar.setProgress(progress);
                if (progressText != null) {
                    progressText.setText(message);
                }
            }
            
            @Override
            public void onSuccess() {
                progressBar.setVisibility(View.GONE);
                if (progressText != null) {
                    progressText.setVisibility(View.GONE);
                }
                terminalView.setVisibility(View.VISIBLE);
                useTermuxEnvironment = true;
                setupTerminal();
                Toast.makeText(TerminalActivity.this, 
                    "Bootstrap installed! Gunakan 'pkg install <package>' untuk install packages.", 
                    Toast.LENGTH_LONG).show();
            }
            
            @Override
            public void onError(String error) {
                progressBar.setVisibility(View.GONE);
                if (progressText != null) {
                    progressText.setVisibility(View.GONE);
                }
                terminalView.setVisibility(View.VISIBLE);
                
                new AlertDialog.Builder(TerminalActivity.this)
                    .setTitle("Installation Error")
                    .setMessage("Gagal menginstal bootstrap:\n" + error + "\n\nMelanjutkan dengan shell dasar...")
                    .setPositiveButton("OK", null)
                    .show();
                    
                useTermuxEnvironment = false;
                setupTerminal();
            }
        });
        
        termuxBootstrap.installBootstrap();
    }
    
    private void showErrorDialog(String title, String message, Throwable error) {
        try {
            String errorDetails = message;
            if (error != null && error.getMessage() != null) {
                errorDetails += "\n\nDetails: " + error.getClass().getSimpleName() + ": " + error.getMessage();
            }
            
            new AlertDialog.Builder(this)
                .setTitle(title)
                .setMessage(errorDetails)
                .setPositiveButton("OK", (dialog, which) -> finish())
                .setNeutralButton("View Details", (dialog, which) -> {
                    if (error != null) {
                        showFullErrorDetails(error);
                    }
                })
                .setCancelable(false)
                .show();
        } catch (Exception e) {
            Log.e(TAG, "Failed to show error dialog", e);
            Toast.makeText(this, message, Toast.LENGTH_LONG).show();
            finish();
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
            
            new AlertDialog.Builder(this)
                .setTitle("Error Details")
                .setView(scrollView)
                .setPositiveButton("Close", (dialog, which) -> finish())
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
            finish();
        }
    }
    
    private void acquireWakeLock() {
        try {
            PowerManager powerManager = (PowerManager) getSystemService(Context.POWER_SERVICE);
            if (powerManager != null && wakeLock == null) {
                wakeLock = powerManager.newWakeLock(
                    PowerManager.PARTIAL_WAKE_LOCK, 
                    WAKELOCK_TAG
                );
                wakeLock.acquire(30 * 60 * 1000L);
                Log.d(TAG, "WakeLock acquired");
            }
        } catch (Exception e) {
            Log.e(TAG, "Error acquiring WakeLock", e);
        }
    }
    
    private void releaseWakeLock() {
        try {
            if (wakeLock != null && wakeLock.isHeld()) {
                wakeLock.release();
                wakeLock = null;
                Log.d(TAG, "WakeLock released");
            }
        } catch (Exception e) {
            Log.e(TAG, "Error releasing WakeLock", e);
        }
    }

    private void setupTerminal() {
        progressBar.setVisibility(View.VISIBLE);
        if (progressText != null) {
            progressText.setText("Memulai terminal...");
        }

        new Thread(() -> {
            try {
                String shellPath = findShell();
                
                mainHandler.post(() -> {
                    try {
                        initTerminalSession(shellPath);
                        progressBar.setVisibility(View.GONE);
                        if (progressText != null) {
                            progressText.setVisibility(View.GONE);
                        }
                    } catch (Exception e) {
                        Toast.makeText(this, "Terminal error: " + e.getMessage(), Toast.LENGTH_LONG).show();
                        progressBar.setVisibility(View.GONE);
                        if (progressText != null) {
                            progressText.setVisibility(View.GONE);
                        }
                    }
                });
            } catch (Exception e) {
                mainHandler.post(() -> {
                    Toast.makeText(this, "Failed to start terminal: " + e.getMessage(), Toast.LENGTH_LONG).show();
                    progressBar.setVisibility(View.GONE);
                    if (progressText != null) {
                        progressText.setVisibility(View.GONE);
                    }
                });
            }
        }).start();
    }

    private String findShell() {
        if (useTermuxEnvironment && termuxBootstrap != null) {
            String bashPath = termuxBootstrap.getBashPath();
            File bashFile = new File(bashPath);
            if (bashFile.exists() && bashFile.canExecute()) {
                return bashPath;
            }
        }
        
        String[] possibleShells = {
            getFilesDir().getAbsolutePath() + "/usr/bin/bash",
            getFilesDir().getAbsolutePath() + "/usr/bin/sh",
            "/system/bin/sh",
            "/system/bin/bash",
            "/system/xbin/bash"
        };

        for (String shell : possibleShells) {
            File shellFile = new File(shell);
            if (shellFile.exists() && shellFile.canExecute()) {
                return shell;
            }
        }

        return "/system/bin/sh";
    }

    private void initTerminalSession(String shellPath) {
        try {
            if (terminalView == null) {
                showErrorDialog("Terminal Error", "Terminal view not initialized", null);
                return;
            }
            
            currentShellPath = shellPath;
            String[] env = buildEnvironment();
            String[] args = new String[]{"-l"};
            
            Log.d(TAG, "Initializing terminal with shell: " + shellPath);
            Log.d(TAG, "Using Termux environment: " + useTermuxEnvironment);

            terminalView.setTerminalViewClient(this);
            
            try {
                Typeface typeface = Typeface.MONOSPACE;
                terminalView.setTypeface(typeface);
            } catch (Exception e) {
                Log.e(TAG, "Error setting typeface", e);
            }
            
            terminalView.setTextSize(currentTextSize);

            terminalSession = new TerminalSession(
                shellPath,
                useTermuxEnvironment ? termuxBootstrap.getHomePath() : workingDirectory,
                args,
                env,
                TerminalEmulator.DEFAULT_TERMINAL_TRANSCRIPT_ROWS,
                this
            );

            terminalView.attachSession(terminalSession);
            isSessionRunning = true;
            
            terminalView.requestFocus();
            
            Log.d(TAG, "Terminal session started successfully");
        } catch (Exception e) {
            Log.e(TAG, "Error creating terminal session", e);
            showErrorDialog("Terminal Session Error", "Failed to create terminal session", e);
        }
    }
    
    private void restartTerminalSession() {
        if (terminalSession != null) {
            try {
                terminalSession.finishIfRunning();
            } catch (Exception e) {
                Log.e(TAG, "Error finishing old session", e);
            }
        }
        
        isSessionRunning = false;
        progressBar.setVisibility(View.VISIBLE);
        
        mainHandler.postDelayed(() -> {
            try {
                initTerminalSession(currentShellPath != null ? currentShellPath : findShell());
                progressBar.setVisibility(View.GONE);
            } catch (Exception e) {
                Log.e(TAG, "Error restarting terminal", e);
                Toast.makeText(this, "Failed to restart terminal: " + e.getMessage(), Toast.LENGTH_LONG).show();
                progressBar.setVisibility(View.GONE);
            }
        }, 300);
    }

    private String[] buildEnvironment() {
        if (useTermuxEnvironment && termuxBootstrap != null) {
            return termuxBootstrap.buildTermuxEnvironment();
        }
        
        String homeDir = getFilesDir().getAbsolutePath() + "/home";
        String tmpDir = getCacheDir().getAbsolutePath();
        String path = "/system/bin:/system/xbin";
        
        File usrBin = new File(getFilesDir(), "usr/bin");
        if (usrBin.exists()) {
            path = usrBin.getAbsolutePath() + ":" + path;
        }

        new File(homeDir).mkdirs();
        
        String externalStorage = Environment.getExternalStorageDirectory().getAbsolutePath();
        
        File profileFile = new File(homeDir, ".profile");
        if (!profileFile.exists()) {
            try {
                java.io.FileWriter writer = new java.io.FileWriter(profileFile);
                writer.write("# CodeEditor Terminal Profile\n");
                writer.write("export PS1='$ '\n");
                writer.write("# Storage shortcuts\n");
                writer.write("alias sdcard='cd " + externalStorage + "'\n");
                writer.write("alias storage='cd " + externalStorage + "'\n");
                writer.close();
            } catch (Exception e) {
                Log.e(TAG, "Error creating .profile", e);
            }
        }

        java.util.List<String> envList = new java.util.ArrayList<>();
        envList.add("HOME=" + homeDir);
        envList.add("PATH=" + path);
        envList.add("TERM=xterm-256color");
        envList.add("TMPDIR=" + tmpDir);
        envList.add("LANG=en_US.UTF-8");
        envList.add("COLORTERM=truecolor");
        envList.add("SHELL=/system/bin/sh");
        envList.add("PS1=$ ");
        envList.add("USER=shell");
        envList.add("HOSTNAME=android");
        envList.add("EXTERNAL_STORAGE=" + externalStorage);
        envList.add("SECONDARY_STORAGE=" + externalStorage);
        
        if (hasStoragePermission()) {
            envList.add("SDCARD=" + externalStorage);
        }
        
        return envList.toArray(new String[0]);
    }
    
    private boolean hasStoragePermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            return Environment.isExternalStorageManager();
        } else {
            return ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) 
                    == PackageManager.PERMISSION_GRANTED &&
                   ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) 
                    == PackageManager.PERMISSION_GRANTED;
        }
    }
    
    private void showStorageWarning() {
        Toast.makeText(this, 
            "Storage permission not granted. Access to /sdcard may be limited.", 
            Toast.LENGTH_LONG).show();
    }

    private void toggleKeyboard() {
        InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        if (imm != null) {
            imm.toggleSoftInput(InputMethodManager.SHOW_IMPLICIT, 0);
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        isSessionRunning = false;
        
        releaseWakeLock();
        
        getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        
        if (terminalSession != null) {
            try {
                terminalSession.finishIfRunning();
            } catch (Exception e) {
                Log.e(TAG, "Error finishing terminal session", e);
            }
            terminalSession = null;
        }
    }
    
    @Override
    protected void onResume() {
        super.onResume();
        if (terminalView != null && isSessionRunning) {
            terminalView.requestFocus();
        }
        if (wakeLock == null || !wakeLock.isHeld()) {
            acquireWakeLock();
        }
    }
    
    @Override
    protected void onPause() {
        super.onPause();
    }
    
    @Override
    protected void onStop() {
        super.onStop();
        releaseWakeLock();
    }

    @Override
    public float onScale(float scale) {
        if (terminalView == null) return scale;
        
        if (scale < 0.9f || scale > 1.1f) {
            int newSize = (int) (currentTextSize * scale);
            if (newSize >= 8 && newSize <= 72) {
                currentTextSize = newSize;
                try {
                    terminalView.setTextSize(newSize);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
        return scale;
    }

    @Override
    public void onSingleTapUp(MotionEvent e) {
        if (terminalView != null) {
            toggleKeyboard();
        }
    }

    @Override
    public boolean shouldBackButtonBeMappedToEscape() {
        return false;
    }

    @Override
    public boolean shouldEnforceCharBasedInput() {
        return true;
    }

    @Override
    public boolean shouldUseCtrlSpaceWorkaround() {
        return false;
    }

    @Override
    public boolean isTerminalViewSelected() {
        return true;
    }

    @Override
    public void copyModeChanged(boolean copyMode) {
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent e, TerminalSession session) {
        return false;
    }

    @Override
    public boolean onKeyUp(int keyCode, KeyEvent e) {
        return false;
    }

    @Override
    public boolean onLongPress(MotionEvent event) {
        return false;
    }

    @Override
    public boolean readControlKey() {
        return false;
    }

    @Override
    public boolean readAltKey() {
        return false;
    }

    @Override
    public boolean readFnKey() {
        return false;
    }

    @Override
    public boolean readShiftKey() {
        return false;
    }

    @Override
    public boolean onCodePoint(int codePoint, boolean ctrlDown, TerminalSession session) {
        return false;
    }

    @Override
    public void onEmulatorSet() {
    }

    @Override
    public void onTextChanged(@NonNull TerminalSession changedSession) {
        if (terminalView != null) {
            try {
                terminalView.onScreenUpdated();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public void onTitleChanged(@NonNull TerminalSession changedSession) {
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle(changedSession.getTitle());
        }
    }

    @Override
    public void onSessionFinished(@NonNull TerminalSession finishedSession) {
        isSessionRunning = false;
        Log.d(TAG, "Terminal session finished");
        
        mainHandler.post(() -> {
            try {
                new AlertDialog.Builder(this)
                    .setTitle("Terminal Session Ended")
                    .setMessage("The shell process has exited. Would you like to restart the terminal or go back?")
                    .setPositiveButton("Restart", (dialog, which) -> {
                        restartTerminalSession();
                    })
                    .setNegativeButton("Exit", (dialog, which) -> {
                        finish();
                    })
                    .setCancelable(false)
                    .show();
            } catch (Exception e) {
                Log.e(TAG, "Error showing dialog", e);
                finish();
            }
        });
    }

    @Override
    public void onCopyTextToClipboard(@NonNull TerminalSession session, String text) {
        android.content.ClipboardManager clipboard = (android.content.ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
        if (clipboard != null) {
            android.content.ClipData clip = android.content.ClipData.newPlainText("Terminal", text);
            clipboard.setPrimaryClip(clip);
        }
    }

    @Override
    public void onPasteTextFromClipboard(@Nullable TerminalSession session) {
        android.content.ClipboardManager clipboard = (android.content.ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
        if (clipboard != null && clipboard.hasPrimaryClip() && session != null) {
            android.content.ClipData.Item item = clipboard.getPrimaryClip().getItemAt(0);
            if (item != null && item.getText() != null) {
                session.getEmulator().paste(item.getText().toString());
            }
        }
    }

    @Override
    public void onBell(@NonNull TerminalSession session) {
    }

    @Override
    public void onColorsChanged(@NonNull TerminalSession session) {
    }

    @Override
    public void onTerminalCursorStateChange(boolean state) {
    }

    @Override
    public Integer getTerminalCursorStyle() {
        return TerminalEmulator.TERMINAL_CURSOR_STYLE_BLOCK;
    }

    @Override
    public void logError(String tag, String message) {
        android.util.Log.e(tag, message);
    }

    @Override
    public void logWarn(String tag, String message) {
        android.util.Log.w(tag, message);
    }

    @Override
    public void logInfo(String tag, String message) {
        android.util.Log.i(tag, message);
    }

    @Override
    public void logDebug(String tag, String message) {
        android.util.Log.d(tag, message);
    }

    @Override
    public void logVerbose(String tag, String message) {
        android.util.Log.v(tag, message);
    }

    @Override
    public void logStackTraceWithMessage(String tag, String message, Exception e) {
        android.util.Log.e(tag, message, e);
    }

    @Override
    public void logStackTrace(String tag, Exception e) {
        android.util.Log.e(tag, "Exception", e);
    }
}
