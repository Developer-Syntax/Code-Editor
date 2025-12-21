package com.codeeditor.android.build;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInstaller;
import android.util.Log;
import android.widget.Toast;

public class InstallReceiver extends BroadcastReceiver {
    
    private static final String TAG = "InstallReceiver";
    
    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent == null) return;
        
        int status = intent.getIntExtra(PackageInstaller.EXTRA_STATUS, PackageInstaller.STATUS_FAILURE);
        String message = intent.getStringExtra(PackageInstaller.EXTRA_STATUS_MESSAGE);
        String packageName = intent.getStringExtra(PackageInstaller.EXTRA_PACKAGE_NAME);
        
        switch (status) {
            case PackageInstaller.STATUS_PENDING_USER_ACTION:
                Intent confirmIntent = intent.getParcelableExtra(Intent.EXTRA_INTENT);
                if (confirmIntent != null) {
                    confirmIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    context.startActivity(confirmIntent);
                }
                break;
                
            case PackageInstaller.STATUS_SUCCESS:
                Log.i(TAG, "Package installed successfully: " + packageName);
                Toast.makeText(context, "App installed successfully!", Toast.LENGTH_SHORT).show();
                break;
                
            case PackageInstaller.STATUS_FAILURE:
            case PackageInstaller.STATUS_FAILURE_ABORTED:
            case PackageInstaller.STATUS_FAILURE_BLOCKED:
            case PackageInstaller.STATUS_FAILURE_CONFLICT:
            case PackageInstaller.STATUS_FAILURE_INCOMPATIBLE:
            case PackageInstaller.STATUS_FAILURE_INVALID:
            case PackageInstaller.STATUS_FAILURE_STORAGE:
                Log.e(TAG, "Installation failed: " + message);
                Toast.makeText(context, "Installation failed: " + message, Toast.LENGTH_LONG).show();
                break;
        }
    }
}
