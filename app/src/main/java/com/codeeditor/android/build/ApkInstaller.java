package com.codeeditor.android.build;

import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInstaller;
import android.net.Uri;
import android.os.Build;
import android.provider.Settings;

import androidx.core.content.FileProvider;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class ApkInstaller {
    
    private Context context;
    
    public interface InstallCallback {
        void onSuccess();
        void onError(String message);
        void onPermissionRequired();
    }
    
    public ApkInstaller(Context context) {
        this.context = context;
    }
    
    public boolean canInstallFromUnknownSources() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            return context.getPackageManager().canRequestPackageInstalls();
        }
        return true;
    }
    
    public void requestInstallPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Intent intent = new Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES);
            intent.setData(Uri.parse("package:" + context.getPackageName()));
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(intent);
        }
    }
    
    public void installApk(File apkFile, InstallCallback callback) {
        if (!apkFile.exists()) {
            callback.onError("APK file not found: " + apkFile.getAbsolutePath());
            return;
        }
        
        if (!canInstallFromUnknownSources()) {
            callback.onPermissionRequired();
            return;
        }
        
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                installWithPackageInstaller(apkFile, callback);
            } else {
                installWithIntent(apkFile, callback);
            }
        } catch (Exception e) {
            callback.onError("Installation failed: " + e.getMessage());
        }
    }
    
    private void installWithPackageInstaller(File apkFile, InstallCallback callback) throws IOException {
        PackageInstaller packageInstaller = context.getPackageManager().getPackageInstaller();
        
        PackageInstaller.SessionParams params = new PackageInstaller.SessionParams(
            PackageInstaller.SessionParams.MODE_FULL_INSTALL);
        params.setAppPackageName(null);
        
        int sessionId;
        try {
            sessionId = packageInstaller.createSession(params);
        } catch (IOException e) {
            callback.onError("Failed to create install session: " + e.getMessage());
            return;
        }
        
        PackageInstaller.Session session = null;
        try {
            session = packageInstaller.openSession(sessionId);
            
            try (InputStream in = new FileInputStream(apkFile);
                 OutputStream out = session.openWrite("base.apk", 0, apkFile.length())) {
                
                byte[] buffer = new byte[65536];
                int length;
                while ((length = in.read(buffer)) != -1) {
                    out.write(buffer, 0, length);
                }
                session.fsync(out);
            }
            
            Intent intent = new Intent(context, InstallReceiver.class);
            intent.setAction("com.codeeditor.android.INSTALL_COMPLETE");
            
            PendingIntent pendingIntent = PendingIntent.getBroadcast(
                context,
                sessionId,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_MUTABLE
            );
            
            session.commit(pendingIntent.getIntentSender());
            callback.onSuccess();
            
        } catch (IOException e) {
            if (session != null) {
                session.abandon();
            }
            callback.onError("Failed to write APK: " + e.getMessage());
        }
    }
    
    private void installWithIntent(File apkFile, InstallCallback callback) {
        try {
            Uri apkUri;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                apkUri = FileProvider.getUriForFile(
                    context,
                    context.getPackageName() + ".fileprovider",
                    apkFile
                );
            } else {
                apkUri = Uri.fromFile(apkFile);
            }
            
            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setDataAndType(apkUri, "application/vnd.android.package-archive");
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            
            context.startActivity(intent);
            callback.onSuccess();
            
        } catch (Exception e) {
            callback.onError("Failed to launch installer: " + e.getMessage());
        }
    }
    
    public void uninstallApp(String packageName, InstallCallback callback) {
        try {
            Intent intent = new Intent(Intent.ACTION_DELETE);
            intent.setData(Uri.parse("package:" + packageName));
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(intent);
            callback.onSuccess();
        } catch (Exception e) {
            callback.onError("Failed to launch uninstaller: " + e.getMessage());
        }
    }
}
