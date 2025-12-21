package com.codeeditor.android.build;

import android.content.Context;
import android.content.SharedPreferences;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.io.File;
import java.lang.reflect.Type;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class IncrementalBuildCache {
    
    private static final String PREFS_NAME = "incremental_build_cache";
    private static final String KEY_FILE_HASHES = "file_hashes";
    private static final String KEY_LAST_BUILD_TIME = "last_build_time";
    private static final String KEY_BUILD_CONFIG_HASH = "build_config_hash";
    
    private SharedPreferences prefs;
    private Gson gson;
    private Map<String, FileInfo> fileInfoMap;
    private String projectPath;
    
    public static class FileInfo {
        public String path;
        public String hash;
        public long lastModified;
        public long size;
        
        public FileInfo(String path, String hash, long lastModified, long size) {
            this.path = path;
            this.hash = hash;
            this.lastModified = lastModified;
            this.size = size;
        }
    }
    
    public static class IncrementalResult {
        public List<File> addedFiles = new ArrayList<>();
        public List<File> modifiedFiles = new ArrayList<>();
        public List<File> deletedFiles = new ArrayList<>();
        public List<File> unchangedFiles = new ArrayList<>();
        
        public boolean hasChanges() {
            return !addedFiles.isEmpty() || !modifiedFiles.isEmpty() || !deletedFiles.isEmpty();
        }
        
        public List<File> getFilesToCompile() {
            List<File> files = new ArrayList<>();
            files.addAll(addedFiles);
            files.addAll(modifiedFiles);
            return files;
        }
        
        public int getTotalChanges() {
            return addedFiles.size() + modifiedFiles.size() + deletedFiles.size();
        }
    }
    
    public IncrementalBuildCache(Context context, String projectPath) {
        this.prefs = context.getSharedPreferences(PREFS_NAME + "_" + projectPath.hashCode(), 
            Context.MODE_PRIVATE);
        this.gson = new Gson();
        this.projectPath = projectPath;
        loadCache();
    }
    
    private void loadCache() {
        String json = prefs.getString(KEY_FILE_HASHES, "{}");
        Type type = new TypeToken<HashMap<String, FileInfo>>(){}.getType();
        fileInfoMap = gson.fromJson(json, type);
        
        if (fileInfoMap == null) {
            fileInfoMap = new HashMap<>();
        }
    }
    
    private void saveCache() {
        String json = gson.toJson(fileInfoMap);
        prefs.edit()
            .putString(KEY_FILE_HASHES, json)
            .putLong(KEY_LAST_BUILD_TIME, System.currentTimeMillis())
            .apply();
    }
    
    public IncrementalResult analyzeChanges(File sourceDir, String... extensions) {
        IncrementalResult result = new IncrementalResult();
        
        Map<String, File> currentFiles = new HashMap<>();
        collectFiles(sourceDir, currentFiles, extensions);
        
        for (Map.Entry<String, File> entry : currentFiles.entrySet()) {
            String path = entry.getKey();
            File file = entry.getValue();
            
            FileInfo cachedInfo = fileInfoMap.get(path);
            
            if (cachedInfo == null) {
                result.addedFiles.add(file);
            } else if (hasFileChanged(file, cachedInfo)) {
                result.modifiedFiles.add(file);
            } else {
                result.unchangedFiles.add(file);
            }
        }
        
        for (String cachedPath : fileInfoMap.keySet()) {
            if (!currentFiles.containsKey(cachedPath)) {
                result.deletedFiles.add(new File(cachedPath));
            }
        }
        
        if (result.hasChanges()) {
            updateCache(result.getFilesToCompile());
            markDeleted(result.deletedFiles);
        }
        
        return result;
    }
    
    private void collectFiles(File dir, Map<String, File> files, String... extensions) {
        if (!dir.exists()) return;
        
        File[] children = dir.listFiles();
        if (children == null) return;
        
        for (File child : children) {
            if (child.isDirectory()) {
                collectFiles(child, files, extensions);
            } else {
                if (matchesExtension(child.getName(), extensions)) {
                    files.put(child.getAbsolutePath(), child);
                }
            }
        }
    }
    
    private boolean matchesExtension(String fileName, String... extensions) {
        if (extensions == null || extensions.length == 0) {
            return true;
        }
        
        for (String ext : extensions) {
            if (fileName.endsWith(ext)) {
                return true;
            }
        }
        
        return false;
    }
    
    private boolean hasFileChanged(File file, FileInfo cachedInfo) {
        if (file.lastModified() != cachedInfo.lastModified) {
            return true;
        }
        
        if (file.length() != cachedInfo.size) {
            return true;
        }
        
        return false;
    }
    
    public void updateCache(List<File> compiledFiles) {
        for (File file : compiledFiles) {
            String path = file.getAbsolutePath();
            String hash = calculateQuickHash(file);
            
            FileInfo info = new FileInfo(path, hash, file.lastModified(), file.length());
            fileInfoMap.put(path, info);
        }
        
        saveCache();
    }
    
    public void markDeleted(List<File> deletedFiles) {
        for (File file : deletedFiles) {
            fileInfoMap.remove(file.getAbsolutePath());
        }
        
        saveCache();
    }
    
    public void invalidateCache() {
        fileInfoMap.clear();
        saveCache();
    }
    
    public boolean shouldDoFullRebuild(BuildConfig config) {
        String currentConfigHash = calculateConfigHash(config);
        String savedConfigHash = prefs.getString(KEY_BUILD_CONFIG_HASH, "");
        
        if (!currentConfigHash.equals(savedConfigHash)) {
            prefs.edit().putString(KEY_BUILD_CONFIG_HASH, currentConfigHash).apply();
            return true;
        }
        
        return false;
    }
    
    private String calculateConfigHash(BuildConfig config) {
        String configString = config.getPackageName() + 
            config.getMinSdkVersion() + 
            config.getTargetSdkVersion() + 
            config.isDebugBuild();
        
        return calculateHash(configString.getBytes());
    }
    
    private String calculateQuickHash(File file) {
        return file.getAbsolutePath() + "_" + file.length() + "_" + file.lastModified();
    }
    
    private String calculateHash(byte[] data) {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] digest = md.digest(data);
            
            StringBuilder sb = new StringBuilder();
            for (byte b : digest) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
            
        } catch (Exception e) {
            return String.valueOf(java.util.Arrays.hashCode(data));
        }
    }
    
    public long getLastBuildTime() {
        return prefs.getLong(KEY_LAST_BUILD_TIME, 0);
    }
    
    public int getCachedFileCount() {
        return fileInfoMap.size();
    }
    
    public void cleanupStaleEntries(File sourceDir) {
        List<String> staleEntries = new ArrayList<>();
        
        for (String path : fileInfoMap.keySet()) {
            File file = new File(path);
            if (!file.exists() || !path.startsWith(sourceDir.getAbsolutePath())) {
                staleEntries.add(path);
            }
        }
        
        for (String path : staleEntries) {
            fileInfoMap.remove(path);
        }
        
        if (!staleEntries.isEmpty()) {
            saveCache();
        }
    }
    
    public Map<String, Integer> getStatistics() {
        Map<String, Integer> stats = new HashMap<>();
        
        int javaFiles = 0;
        int kotlinFiles = 0;
        int otherFiles = 0;
        
        for (String path : fileInfoMap.keySet()) {
            if (path.endsWith(".java")) {
                javaFiles++;
            } else if (path.endsWith(".kt") || path.endsWith(".kts")) {
                kotlinFiles++;
            } else {
                otherFiles++;
            }
        }
        
        stats.put("java_files", javaFiles);
        stats.put("kotlin_files", kotlinFiles);
        stats.put("other_files", otherFiles);
        stats.put("total_files", fileInfoMap.size());
        
        return stats;
    }
}
