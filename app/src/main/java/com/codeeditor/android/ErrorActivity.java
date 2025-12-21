package com.codeeditor.android;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.graphics.Typeface;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

public class ErrorActivity extends AppCompatActivity {
    
    private String shortMessage;
    private String fullMessage;
    private boolean showingDetails = false;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        shortMessage = getIntent().getStringExtra("short_message");
        fullMessage = getIntent().getStringExtra("full_message");
        
        if (shortMessage == null) shortMessage = "Unknown error";
        if (fullMessage == null) fullMessage = shortMessage;
        
        setupLayout();
    }
    
    private void setupLayout() {
        ScrollView scrollView = new ScrollView(this);
        scrollView.setFillViewport(true);
        
        android.widget.LinearLayout layout = new android.widget.LinearLayout(this);
        layout.setOrientation(android.widget.LinearLayout.VERTICAL);
        layout.setPadding(48, 48, 48, 48);
        layout.setBackgroundColor(0xFFF5F5F5);
        
        TextView titleView = new TextView(this);
        titleView.setText("Application Error");
        titleView.setTextSize(24);
        titleView.setTextColor(0xFFD32F2F);
        titleView.setTypeface(null, Typeface.BOLD);
        titleView.setPadding(0, 0, 0, 32);
        layout.addView(titleView);
        
        TextView iconView = new TextView(this);
        iconView.setText("⚠");
        iconView.setTextSize(64);
        iconView.setTextColor(0xFFD32F2F);
        iconView.setGravity(android.view.Gravity.CENTER);
        iconView.setPadding(0, 0, 0, 32);
        layout.addView(iconView);
        
        TextView descView = new TextView(this);
        descView.setText("An unexpected error occurred and the application needs to be restarted.");
        descView.setTextSize(16);
        descView.setTextColor(0xFF616161);
        descView.setPadding(0, 0, 0, 32);
        layout.addView(descView);
        
        final TextView errorView = new TextView(this);
        errorView.setText(shortMessage);
        errorView.setTextSize(14);
        errorView.setTextColor(0xFF424242);
        errorView.setTypeface(Typeface.MONOSPACE);
        errorView.setBackgroundColor(0xFFFFFFFF);
        errorView.setPadding(32, 32, 32, 32);
        errorView.setTextIsSelectable(true);
        layout.addView(errorView);
        
        android.widget.LinearLayout buttonLayout = new android.widget.LinearLayout(this);
        buttonLayout.setOrientation(android.widget.LinearLayout.VERTICAL);
        buttonLayout.setPadding(0, 32, 0, 0);
        
        Button restartButton = new Button(this);
        restartButton.setText("Restart Application");
        restartButton.setBackgroundColor(0xFF4CAF50);
        restartButton.setTextColor(0xFFFFFFFF);
        restartButton.setOnClickListener(v -> restartApp());
        buttonLayout.addView(restartButton);
        
        addButtonMargin(restartButton);
        
        final Button detailsButton = new Button(this);
        detailsButton.setText("Show Details");
        detailsButton.setBackgroundColor(0xFF2196F3);
        detailsButton.setTextColor(0xFFFFFFFF);
        detailsButton.setOnClickListener(v -> {
            showingDetails = !showingDetails;
            if (showingDetails) {
                errorView.setText(fullMessage);
                detailsButton.setText("Hide Details");
            } else {
                errorView.setText(shortMessage);
                detailsButton.setText("Show Details");
            }
        });
        buttonLayout.addView(detailsButton);
        
        addButtonMargin(detailsButton);
        
        Button copyButton = new Button(this);
        copyButton.setText("Copy Error Log");
        copyButton.setBackgroundColor(0xFF9E9E9E);
        copyButton.setTextColor(0xFFFFFFFF);
        copyButton.setOnClickListener(v -> copyToClipboard());
        buttonLayout.addView(copyButton);
        
        addButtonMargin(copyButton);
        
        Button closeButton = new Button(this);
        closeButton.setText("Close Application");
        closeButton.setBackgroundColor(0xFFF44336);
        closeButton.setTextColor(0xFFFFFFFF);
        closeButton.setOnClickListener(v -> {
            finishAffinity();
            android.os.Process.killProcess(android.os.Process.myPid());
            System.exit(1);
        });
        buttonLayout.addView(closeButton);
        
        layout.addView(buttonLayout);
        
        scrollView.addView(layout);
        setContentView(scrollView);
    }
    
    private void addButtonMargin(Button button) {
        android.widget.LinearLayout.LayoutParams params = 
                (android.widget.LinearLayout.LayoutParams) button.getLayoutParams();
        if (params == null) {
            params = new android.widget.LinearLayout.LayoutParams(
                    android.widget.LinearLayout.LayoutParams.MATCH_PARENT,
                    android.widget.LinearLayout.LayoutParams.WRAP_CONTENT);
        }
        params.setMargins(0, 0, 0, 16);
        button.setLayoutParams(params);
    }
    
    private void restartApp() {
        try {
            Intent intent = getPackageManager().getLaunchIntentForPackage(getPackageName());
            if (intent != null) {
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(intent);
            }
        } catch (Exception e) {
            Toast.makeText(this, "Failed to restart app", Toast.LENGTH_SHORT).show();
        }
        finishAffinity();
        android.os.Process.killProcess(android.os.Process.myPid());
        System.exit(0);
    }
    
    private void copyToClipboard() {
        try {
            ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
            if (clipboard != null) {
                ClipData clip = ClipData.newPlainText("Error Log", fullMessage);
                clipboard.setPrimaryClip(clip);
                Toast.makeText(this, "Error log copied to clipboard", Toast.LENGTH_SHORT).show();
            }
        } catch (Exception e) {
            Toast.makeText(this, "Failed to copy to clipboard", Toast.LENGTH_SHORT).show();
        }
    }
    
    @Override
    public void onBackPressed() {
        finishAffinity();
        android.os.Process.killProcess(android.os.Process.myPid());
        System.exit(1);
    }
}
