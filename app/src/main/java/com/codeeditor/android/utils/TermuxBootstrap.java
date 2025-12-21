package com.codeeditor.android.utils;

import android.content.Context;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.system.Os;
import android.system.ErrnoException;
import android.util.Log;
import android.util.Pair;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.GZIPInputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;

public class TermuxBootstrap {
    private static final String TAG = "TermuxBootstrap";
    
    /**
     * Bootstrap Manager for CodeEditor Android App
     * 
     * Supports multiple bootstrap types:
     * - Custom Debian aarch64 (built via GitHub Actions)
     * - Standard Termux bootstrap
     * - Other compatible Linux distributions
     * 
     * This class handles:
     * - Downloading bootstrap archives (TAR.GZ or ZIP)
     * - Auto-detection of archive format
     * - Extraction and symlink restoration
     * - Environment setup and configuration
     * 
     * To use custom Debian bootstrap:
     * 1. Create a git tag: git tag v1.0.0
     * 2. Push: git push origin v1.0.0
     * 3. Wait ~15-20 min for GitHub Actions to build
     * 4. Download tar.gz from GitHub Release
     * 5. Update BOOTSTRAP_URL below with release download URL
     * 
     * Supported formats:
     * - TAR.GZ (recommended) - faster, more reliable
     * - ZIP - Android ZipInputStream compatible
     * 
     * Bootstrap requirements:
     * - bin/bash (shell)
     * - bin/apt or bin/pkg (package manager, optional)
     * - Standard Linux directory structure (/bin, /usr, /lib, etc)
     */
    private static final String BOOTSTRAP_URL = 
        "https://github.com/Developer-Syntax/Code-Editor/releases/download/bootstrap-20251221-201955-637b4ba/debian-aarch64-bootstrap.tar.gz";
    
    // TODO: Update BOOTSTRAP_URL with your GitHub release URL after first build
    
    private static final String BOOTSTRAP_VERSION = "bookworm-debian-2025-01";
    
    private final Context context;
    private final File filesDir;
    private final File prefixDir;
    private final File stagingPrefixDir;
    private final File homeDir;
    private final File tmpDir;
    private BootstrapListener listener;
    
    public interface BootstrapListener {
        void onProgress(String message, int progress);
        void onSuccess();
        void onError(String error);
    }
    
    public TermuxBootstrap(Context context) {
        this.context = context;
        this.filesDir = context.getFilesDir();
        this.prefixDir = new File(filesDir, "usr");
        this.stagingPrefixDir = new File(filesDir, "usr-staging");
        this.homeDir = new File(filesDir, "home");
        this.tmpDir = context.getCacheDir();
    }
    
    public void setListener(BootstrapListener listener) {
        this.listener = listener;
    }
    
    public boolean isBootstrapInstalled() {
        File bashFile = new File(prefixDir, "bin/bash");
        File pkgFile = new File(prefixDir, "bin/pkg");
        File aptFile = new File(prefixDir, "bin/apt");
        File versionFile = new File(prefixDir, ".bootstrap_version");
        
        boolean bashExists = bashFile.exists() && bashFile.canExecute();
        boolean pkgExists = pkgFile.exists();
        boolean aptExists = aptFile.exists();
        
        if (bashExists && (pkgExists || aptExists)) {
            if (versionFile.exists()) {
                try {
                    String installedVersion = readFile(versionFile);
                    return BOOTSTRAP_VERSION.equals(installedVersion.trim());
                } catch (Exception e) {
                    return true;
                }
            }
            return true;
        }
        return false;
    }
    
    public String getPrefixPath() {
        return prefixDir.getAbsolutePath();
    }
    
    public String getHomePath() {
        return homeDir.getAbsolutePath();
    }
    
    public String getBashPath() {
        return new File(prefixDir, "bin/bash").getAbsolutePath();
    }
    
    public void installBootstrap() {
        new Thread(() -> {
            try {
                notifyProgress("Mempersiapkan instalasi...", 0);
                
                deleteDirectoryRecursive(stagingPrefixDir);
                deleteDirectoryRecursive(prefixDir);
                
                ensureDirectoryExists(stagingPrefixDir);
                ensurePrivateDirectoryExists(homeDir);
                
                notifyProgress("Mengunduh Termux bootstrap...", 5);
                byte[] archiveBytes = downloadBootstrap(BOOTSTRAP_URL);
                
                notifyProgress("Mengekstrak bootstrap...", 40);
                extractBootstrap(archiveBytes, BOOTSTRAP_URL);
                
                notifyProgress("Memindahkan ke direktori prefix...", 85);
                deleteDirectoryRecursive(prefixDir);
                if (!stagingPrefixDir.renameTo(prefixDir)) {
                    throw new RuntimeException("Gagal memindahkan staging ke prefix");
                }
                
                notifyProgress("Mengatur permission file...", 87);
                fixExecutablePermissions();
                
                notifyProgress("Menulis konfigurasi...", 90);
                writeConfigs();
                
                notifyProgress("Verifikasi instalasi...", 95);
                verifyInstallation();
                
                File versionFile = new File(prefixDir, ".bootstrap_version");
                writeFile(versionFile, BOOTSTRAP_VERSION);
                
                notifyProgress("Selesai!", 100);
                notifySuccess();
                
            } catch (Exception e) {
                Log.e(TAG, "Error installing bootstrap", e);
                notifyError("Gagal menginstal bootstrap: " + e.getMessage());
            }
        }).start();
    }
    
    private void ensureDirectoryExists(File dir) {
        if (dir == null) return;
        
        if (!dir.exists()) {
            dir.mkdirs();
        }
        
        if (dir.isDirectory()) {
            try {
                Os.chmod(dir.getAbsolutePath(), 0755);
            } catch (ErrnoException e) {
                dir.setReadable(true, false);
                dir.setWritable(true, true);
                dir.setExecutable(true, false);
            }
        }
    }
    
    private void ensurePrivateDirectoryExists(File dir) {
        if (dir == null) return;
        
        if (!dir.exists()) {
            dir.mkdirs();
        }
        
        if (dir.isDirectory()) {
            try {
                Os.chmod(dir.getAbsolutePath(), 0700);
            } catch (ErrnoException e) {
                dir.setReadable(true, true);
                dir.setWritable(true, true);
                dir.setExecutable(true, true);
            }
        }
    }
    
    private byte[] downloadBootstrap(String urlString) throws IOException {
        HttpURLConnection connection = null;
        InputStream input = null;
        ByteArrayOutputStream output = null;
        
        try {
            URL url = new URL(urlString);
            connection = (HttpURLConnection) url.openConnection();
            connection.setConnectTimeout(30000);
            connection.setReadTimeout(120000);
            connection.setInstanceFollowRedirects(true);
            connection.setRequestProperty("User-Agent", "CodeEditor-Android/1.0");
            connection.connect();
            
            int responseCode = connection.getResponseCode();
            
            while (responseCode == HttpURLConnection.HTTP_MOVED_PERM || 
                   responseCode == HttpURLConnection.HTTP_MOVED_TEMP ||
                   responseCode == 307 || responseCode == 308) {
                String newUrl = connection.getHeaderField("Location");
                connection.disconnect();
                url = new URL(newUrl);
                connection = (HttpURLConnection) url.openConnection();
                connection.setConnectTimeout(30000);
                connection.setReadTimeout(120000);
                connection.setRequestProperty("User-Agent", "CodeEditor-Android/1.0");
                connection.connect();
                responseCode = connection.getResponseCode();
            }
            
            if (responseCode != HttpURLConnection.HTTP_OK) {
                throw new IOException("Server returned HTTP " + responseCode);
            }
            
            int fileLength = connection.getContentLength();
            input = new BufferedInputStream(connection.getInputStream());
            output = new ByteArrayOutputStream();
            
            byte[] buffer = new byte[8192];
            long total = 0;
            int count;
            int lastProgress = 5;
            
            while ((count = input.read(buffer)) != -1) {
                total += count;
                output.write(buffer, 0, count);
                
                if (fileLength > 0) {
                    int progress = 5 + (int) ((total * 35) / fileLength);
                    if (progress > lastProgress) {
                        lastProgress = progress;
                        notifyProgress("Mengunduh... " + (total / 1024) + " KB / " + (fileLength / 1024) + " KB", progress);
                    }
                }
            }
            
            return output.toByteArray();
            
        } finally {
            if (output != null) try { output.close(); } catch (Exception e) {}
            if (input != null) try { input.close(); } catch (Exception e) {}
            if (connection != null) connection.disconnect();
        }
    }
    
    private void extractBootstrap(byte[] archiveBytes, String urlString) throws IOException {
        // Detect format from URL or magic bytes
        boolean isTarGz = urlString.endsWith(".tar.gz") || urlString.endsWith(".tgz") ||
                          (archiveBytes.length > 2 && archiveBytes[0] == 0x1f && archiveBytes[1] == (byte) 0x8b);
        
        if (isTarGz) {
            Log.i(TAG, "Detected TAR.GZ format");
            extractTarGz(archiveBytes);
        } else {
            Log.i(TAG, "Detected ZIP format");
            extractZip(archiveBytes);
        }
    }
    
    private void extractZip(byte[] zipBytes) throws IOException {
        final byte[] buffer = new byte[8192];
        final List<Pair<String, String>> symlinks = new ArrayList<>(50);
        final String stagingPath = stagingPrefixDir.getAbsolutePath();
        
        try (ZipInputStream zipInput = new ZipInputStream(new ByteArrayInputStream(zipBytes))) {
            ZipEntry zipEntry;
            int entryCount = 0;
            
            while ((zipEntry = zipInput.getNextEntry()) != null) {
                String entryName = zipEntry.getName();
                
                if (entryName.equals("SYMLINKS.txt")) {
                    BufferedReader symlinksReader = new BufferedReader(new InputStreamReader(zipInput, "UTF-8"));
                    String line;
                    while ((line = symlinksReader.readLine()) != null) {
                        line = line.trim();
                        if (line.isEmpty()) continue;
                        
                        String[] parts = line.split("\u2190");
                        if (parts.length != 2) {
                            Log.w(TAG, "Malformed symlink line: " + line);
                            continue;
                        }
                        
                        String oldPath = parts[0].trim();
                        String newPath = parts[1].trim();
                        
                        if (newPath.startsWith("./")) {
                            newPath = newPath.substring(2);
                        }
                        
                        String fullNewPath = stagingPath + "/" + newPath;
                        symlinks.add(Pair.create(oldPath, fullNewPath));
                        
                        File parentDir = new File(fullNewPath).getParentFile();
                        ensureDirectoryExists(parentDir);
                    }
                } else {
                    File targetFile = new File(stagingPrefixDir, entryName);
                    boolean isDirectory = zipEntry.isDirectory();
                    
                    File parentDir = isDirectory ? targetFile : targetFile.getParentFile();
                    ensureDirectoryExists(parentDir);
                    
                    if (isDirectory) {
                        ensureDirectoryExists(targetFile);
                    } else {
                        try (FileOutputStream outStream = new FileOutputStream(targetFile)) {
                            int readBytes;
                            while ((readBytes = zipInput.read(buffer)) != -1) {
                                outStream.write(buffer, 0, readBytes);
                            }
                        }
                        
                        if (isExecutable(entryName)) {
                            setFileMode(targetFile, 0700);
                        } else {
                            setFileMode(targetFile, 0600);
                        }
                    }
                }
                
                entryCount++;
                if (entryCount % 200 == 0) {
                    int progress = 40 + Math.min(40, entryCount / 50);
                    notifyProgress("Mengekstrak: " + entryCount + " file", progress);
                }
                
                zipInput.closeEntry();
            }
        }
        
        createSymlinks(symlinks);
    }
    
    private void extractTarGz(byte[] tarGzBytes) throws IOException {
        final byte[] buffer = new byte[8192];
        final List<Pair<String, String>> symlinks = new ArrayList<>(50);
        final String stagingPath = stagingPrefixDir.getAbsolutePath();
        
        try (GZIPInputStream gzipInput = new GZIPInputStream(new ByteArrayInputStream(tarGzBytes));
             TarArchiveInputStream tarInput = new TarArchiveInputStream(gzipInput)) {
            
            TarArchiveEntry tarEntry;
            int entryCount = 0;
            
            while ((tarEntry = tarInput.getNextTarEntry()) != null) {
                String entryName = tarEntry.getName();
                
                // Remove leading ./ if present
                if (entryName.startsWith("./")) {
                    entryName = entryName.substring(2);
                }
                
                if (entryName.equals("SYMLINKS.txt")) {
                    ByteArrayOutputStream symlinkContent = new ByteArrayOutputStream();
                    int readBytes;
                    while ((readBytes = tarInput.read(buffer)) != -1) {
                        symlinkContent.write(buffer, 0, readBytes);
                    }
                    
                    BufferedReader symlinksReader = new BufferedReader(
                        new InputStreamReader(new ByteArrayInputStream(symlinkContent.toByteArray()), "UTF-8"));
                    String line;
                    while ((line = symlinksReader.readLine()) != null) {
                        line = line.trim();
                        if (line.isEmpty() || line.startsWith("#")) continue;
                        
                        String[] parts = line.split("\u2190");
                        if (parts.length != 2) {
                            Log.w(TAG, "Malformed symlink line: " + line);
                            continue;
                        }
                        
                        String oldPath = parts[0].trim();
                        String newPath = parts[1].trim();
                        
                        if (newPath.startsWith("./")) {
                            newPath = newPath.substring(2);
                        }
                        
                        String fullNewPath = stagingPath + "/" + newPath;
                        symlinks.add(Pair.create(oldPath, fullNewPath));
                        
                        File parentDir = new File(fullNewPath).getParentFile();
                        ensureDirectoryExists(parentDir);
                    }
                } else if (!entryName.isEmpty()) {
                    File targetFile = new File(stagingPrefixDir, entryName);
                    
                    if (tarEntry.isDirectory()) {
                        ensureDirectoryExists(targetFile);
                    } else if (tarEntry.isSymbolicLink()) {
                        // Handle symlinks in tar
                        String linkTarget = tarEntry.getLinkName();
                        symlinks.add(Pair.create(linkTarget, targetFile.getAbsolutePath()));
                        File parentDir = targetFile.getParentFile();
                        ensureDirectoryExists(parentDir);
                    } else {
                        File parentDir = targetFile.getParentFile();
                        ensureDirectoryExists(parentDir);
                        
                        try (FileOutputStream outStream = new FileOutputStream(targetFile)) {
                            int readBytes;
                            while ((readBytes = tarInput.read(buffer)) != -1) {
                                outStream.write(buffer, 0, readBytes);
                            }
                        }
                        
                        if (isExecutable(entryName)) {
                            setFileMode(targetFile, 0700);
                        } else {
                            setFileMode(targetFile, 0600);
                        }
                    }
                }
                
                entryCount++;
                if (entryCount % 200 == 0) {
                    int progress = 40 + Math.min(40, entryCount / 50);
                    notifyProgress("Mengekstrak: " + entryCount + " file", progress);
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Error extracting tar.gz", e);
            throw new IOException("Gagal extract tar.gz: " + e.getMessage(), e);
        }
        
        createSymlinks(symlinks);
    }
    
    private void createSymlinks(List<Pair<String, String>> symlinks) throws IOException {
        if (symlinks.isEmpty()) {
            throw new RuntimeException("SYMLINKS.txt tidak ditemukan atau kosong");
        }
        
        notifyProgress("Membuat symlinks (" + symlinks.size() + ")...", 80);
        
        int symlinkCount = 0;
        int symlinkErrors = 0;
        for (Pair<String, String> symlink : symlinks) {
            try {
                File linkFile = new File(symlink.second);
                if (linkFile.exists()) {
                    linkFile.delete();
                }
                
                Os.symlink(symlink.first, symlink.second);
                symlinkCount++;
                
                if (symlinkCount % 100 == 0) {
                    notifyProgress("Membuat symlinks: " + symlinkCount + "/" + symlinks.size(), 80 + (symlinkCount * 5 / symlinks.size()));
                }
            } catch (ErrnoException e) {
                symlinkErrors++;
                Log.w(TAG, "Failed to create symlink: " + symlink.first + " -> " + symlink.second + ": " + e.getMessage());
            }
        }
        
        Log.i(TAG, "Created " + symlinkCount + " symlinks, " + symlinkErrors + " errors, out of " + symlinks.size() + " total");
    }
    
    private boolean isExecutable(String entryName) {
        return entryName.startsWith("bin/") ||
               entryName.startsWith("libexec/") ||
               entryName.startsWith("lib/apt/apt-helper") ||
               entryName.startsWith("lib/apt/methods/");
    }
    
    private void setFileMode(File file, int mode) {
        try {
            Os.chmod(file.getAbsolutePath(), mode);
        } catch (ErrnoException e) {
            boolean ownerOnly = (mode & 0077) == 0;
            file.setReadable(true, ownerOnly);
            file.setWritable(true, true);
            if ((mode & 0100) != 0) {
                file.setExecutable(true, ownerOnly);
            }
        }
    }
    
    private void fixExecutablePermissions() {
        String[] executableDirs = {
            "bin",                  // Essential commands
            "sbin",                 // System commands
            "libexec",              // Library executables
            "lib/apt/methods",      // APT methods
            "usr/bin",              // User binaries
            "usr/sbin",             // User system binaries
            "usr/libexec"           // User library executables
        };
        
        // Fix permissions on executable directories
        for (String dirPath : executableDirs) {
            File dir = new File(prefixDir, dirPath);
            if (dir.exists() && dir.isDirectory()) {
                setExecutableRecursive(dir);
            }
        }
        
        // Fix specific helper tools if they exist
        File[] helpers = {
            new File(prefixDir, "lib/apt/apt-helper"),
            new File(prefixDir, "usr/lib/apt/methods/http"),
            new File(prefixDir, "usr/lib/apt/methods/https")
        };
        for (File helper : helpers) {
            if (helper.exists()) {
                setFileMode(helper, 0755);
            }
        }
        
        // Ensure critical binaries are executable
        String[] criticalBinaries = {
            "bash", "sh",           // Shells
            "pkg", "apt", "apt-get", "apt-cache", "dpkg",  // Package managers
            "ls", "cat", "echo", "grep"  // Basic tools
        };
        for (String binary : criticalBinaries) {
            File binFile = new File(prefixDir, "bin/" + binary);
            if (binFile.exists() && !binFile.canExecute()) {
                setFileMode(binFile, 0755);
                
                // Also fix symlink targets if needed
                if (!binFile.canExecute()) {
                    try {
                        String target = Os.readlink(binFile.getAbsolutePath());
                        if (target != null) {
                            File targetFile;
                            if (target.startsWith("/")) {
                                targetFile = new File(target);
                            } else {
                                targetFile = new File(binFile.getParentFile(), target);
                            }
                            if (targetFile.exists()) {
                                setFileMode(targetFile, 0755);
                            }
                        }
                    } catch (ErrnoException e) {
                        // Not a symlink, ignore
                    }
                }
            }
        }
        
        Log.i(TAG, "Fixed executable permissions");
    }
    
    private void setExecutableRecursive(File dir) {
        if (dir == null || !dir.exists()) return;
        
        File[] files = dir.listFiles();
        if (files == null) return;
        
        for (File file : files) {
            if (file.isDirectory()) {
                setExecutableRecursive(file);
            } else if (file.isFile()) {
                setFileMode(file, 0755);
            }
        }
    }
    
    private void verifyInstallation() throws Exception {
        // Essential files required in any bootstrap
        String[] essentialFiles = {
            "bin/bash",
            "bin/sh"
        };
        
        // Optional package managers - at least one should exist
        String[] packageManagers = {
            "bin/apt",      // Debian/Ubuntu
            "bin/pkg",      // Termux
            "bin/dpkg"      // Debian tools
        };
        
        List<String> missingEssential = new ArrayList<>();
        List<String> nonExecutableEssential = new ArrayList<>();
        boolean hasPackageManager = false;
        
        // Check essential files
        for (String path : essentialFiles) {
            File file = new File(prefixDir, path);
            if (!file.exists()) {
                try {
                    String target = Os.readlink(file.getAbsolutePath());
                    if (target == null) {
                        missingEssential.add(path);
                    }
                } catch (ErrnoException e) {
                    missingEssential.add(path);
                }
            } else if (!file.canExecute()) {
                setFileMode(file, 0755);
                if (!file.canExecute()) {
                    nonExecutableEssential.add(path);
                }
            }
        }
        
        // Check package managers
        for (String path : packageManagers) {
            File file = new File(prefixDir, path);
            if (file.exists() && file.canExecute()) {
                hasPackageManager = true;
                Log.i(TAG, "Found package manager: " + path);
            }
        }
        
        // Warn if no package manager found
        if (!hasPackageManager) {
            Log.w(TAG, "No package manager found. You may need to install apt or pkg manually.");
        }
        
        // Report missing essential files
        if (!missingEssential.isEmpty()) {
            throw new Exception("Missing essential files: " + missingEssential);
        }
        
        if (!nonExecutableEssential.isEmpty()) {
            Log.w(TAG, "Non-executable essential files: " + nonExecutableEssential);
        }
    }
    
    private void writeConfigs() {
        try {
            File bashrc = new File(homeDir, ".bashrc");
            if (!bashrc.exists()) {
                StringBuilder bashrcContent = new StringBuilder();
                bashrcContent.append("# CodeEditor Terminal - Linux Bootstrap Environment\n\n");
                bashrcContent.append("export PREFIX=\"").append(prefixDir.getAbsolutePath()).append("\"\n");
                bashrcContent.append("export HOME=\"").append(homeDir.getAbsolutePath()).append("\"\n");
                bashrcContent.append("export TMPDIR=\"").append(tmpDir.getAbsolutePath()).append("\"\n");
                bashrcContent.append("export LD_LIBRARY_PATH=\"$PREFIX/lib:$LD_LIBRARY_PATH\"\n");
                bashrcContent.append("export PATH=\"$PREFIX/bin:$PREFIX/sbin:$PATH\"\n");
                bashrcContent.append("export LANG=en_US.UTF-8\n");
                bashrcContent.append("export TERM=xterm-256color\n\n");
                bashrcContent.append("# Aliases\n");
                bashrcContent.append("alias ll='ls -la'\n");
                bashrcContent.append("alias la='ls -A'\n");
                bashrcContent.append("alias l='ls -CF'\n\n");
                bashrcContent.append("# PS1 Prompt\n");
                bashrcContent.append("PS1='\\[\\033[01;32m\\]shell\\[\\033[00m\\]:\\[\\033[01;34m\\]\\w\\[\\033[00m\\]\\$ '\n\n");
                bashrcContent.append("# Source common shell functions if available\n");
                bashrcContent.append("[ -f /etc/bashrc ] && . /etc/bashrc\n");
                
                writeFile(bashrc, bashrcContent.toString());
            }
            
            File profile = new File(homeDir, ".profile");
            if (!profile.exists()) {
                StringBuilder profileContent = new StringBuilder();
                profileContent.append("# Profile for CodeEditor Terminal\n\n");
                profileContent.append("# Set up environment\n");
                profileContent.append("export PREFIX=\"").append(prefixDir.getAbsolutePath()).append("\"\n");
                profileContent.append("export PATH=\"$PREFIX/bin:$PREFIX/sbin:$PATH\"\n");
                profileContent.append("export LD_LIBRARY_PATH=\"$PREFIX/lib:$LD_LIBRARY_PATH\"\n\n");
                profileContent.append("# Load bashrc if using bash\n");
                profileContent.append("if [ -n \"$BASH_VERSION\" ] && [ -f ~/.bashrc ]; then\n");
                profileContent.append("    . ~/.bashrc\n");
                profileContent.append("fi\n");
                
                writeFile(profile, profileContent.toString());
            }
            
            File etcDir = new File(prefixDir, "etc");
            if (!etcDir.exists()) {
                etcDir.mkdirs();
            }
            
            File motd = new File(etcDir, "motd");
            if (!motd.exists()) {
                StringBuilder motdContent = new StringBuilder();
                motdContent.append("Welcome to CodeEditor Terminal!\n");
                motdContent.append("This is a Linux bootstrap environment.\n\n");
                motdContent.append("Package management (if available):\n");
                motdContent.append("  Debian/Ubuntu: apt install <package>\n");
                motdContent.append("  Termux:        pkg install <package>\n\n");
                motdContent.append("Examples:\n");
                motdContent.append("  apt install python3\n");
                motdContent.append("  apt install nodejs\n");
                motdContent.append("  apt install git\n\n");
                motdContent.append("Type 'apt --help' or 'pkg --help' to learn more.\n");
                
                writeFile(motd, motdContent.toString());
            }
            
        } catch (Exception e) {
            Log.e(TAG, "Error writing configs", e);
        }
    }
    
    private void deleteDirectoryRecursive(File dir) {
        if (dir == null || !dir.exists()) return;
        
        File[] files = dir.listFiles();
        if (files != null) {
            for (File file : files) {
                if (file.isDirectory()) {
                    deleteDirectoryRecursive(file);
                } else {
                    file.delete();
                }
            }
        }
        dir.delete();
    }
    
    private String readFile(File file) throws IOException {
        FileInputStream fis = new FileInputStream(file);
        byte[] data = new byte[(int) file.length()];
        fis.read(data);
        fis.close();
        return new String(data, "UTF-8");
    }
    
    private void writeFile(File file, String content) throws IOException {
        FileOutputStream fos = new FileOutputStream(file);
        fos.write(content.getBytes("UTF-8"));
        fos.close();
    }
    
    public String[] buildTermuxEnvironment() {
        java.util.List<String> env = new java.util.ArrayList<>();
        
        String prefixPath = prefixDir.getAbsolutePath();
        String homePath = homeDir.getAbsolutePath();
        String tmpPath = tmpDir.getAbsolutePath();
        
        env.add("PREFIX=" + prefixPath);
        env.add("HOME=" + homePath);
        env.add("TMPDIR=" + tmpPath);
        env.add("PATH=" + prefixPath + "/bin:" + prefixPath + "/bin/applets:/system/bin:/system/xbin");
        env.add("LD_LIBRARY_PATH=" + prefixPath + "/lib");
        env.add("LANG=en_US.UTF-8");
        env.add("TERM=xterm-256color");
        env.add("COLORTERM=truecolor");
        env.add("SHELL=" + prefixPath + "/bin/bash");
        
        int uid = android.os.Process.myUid();
        String user = "u0_a" + (uid % 100000);
        env.add("USER=" + user);
        env.add("LOGNAME=" + user);
        env.add("HOSTNAME=localhost");
        
        File externalStorage = android.os.Environment.getExternalStorageDirectory();
        if (externalStorage != null) {
            env.add("EXTERNAL_STORAGE=" + externalStorage.getAbsolutePath());
        }
        
        env.add("ANDROID_DATA=/data");
        env.add("ANDROID_ROOT=/system");
        
        env.add("TERMUX_VERSION=0.118");
        env.add("TERMUX_APK_RELEASE=GITHUB");
        env.add("TERMUX_APP_PACKAGE_NAME=" + context.getPackageName());
        env.add("TERMUX_PREFIX=" + prefixPath);
        
        return env.toArray(new String[0]);
    }
    
    private void notifyProgress(String message, int progress) {
        if (listener != null) {
            new Handler(Looper.getMainLooper()).post(() -> 
                listener.onProgress(message, progress));
        }
    }
    
    private void notifySuccess() {
        if (listener != null) {
            new Handler(Looper.getMainLooper()).post(() -> 
                listener.onSuccess());
        }
    }
    
    private void notifyError(String error) {
        if (listener != null) {
            new Handler(Looper.getMainLooper()).post(() -> 
                listener.onError(error));
        }
    }
}
