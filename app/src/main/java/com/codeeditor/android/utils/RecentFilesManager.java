package com.codeeditor.android.utils;

import android.content.Context;
import android.content.SharedPreferences;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class RecentFilesManager {

    private static final String PREFS_NAME = "recent_files_prefs";
    private static final String KEY_RECENT_FILES = "recent_files";
    private static final int MAX_RECENT_FILES = 20;

    public static class RecentFile {
        public String name;
        public String path;
        public String localUri;
        public String owner;
        public String repo;
        public String branch;
        public long lastOpened;
        public boolean isGitHub;

        public RecentFile(String name, String path, boolean isGitHub) {
            this.name = name;
            this.path = path;
            this.isGitHub = isGitHub;
            this.lastOpened = System.currentTimeMillis();
        }

        public RecentFile setGitHubInfo(String owner, String repo, String branch) {
            this.owner = owner;
            this.repo = repo;
            this.branch = branch;
            return this;
        }

        public RecentFile setLocalUri(String uri) {
            this.localUri = uri;
            return this;
        }
    }

    private SharedPreferences prefs;
    private Gson gson;

    public RecentFilesManager(Context context) {
        prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        gson = new Gson();
    }

    public void addRecentFile(RecentFile file) {
        List<RecentFile> recentFiles = getRecentFiles();

        Iterator<RecentFile> iterator = recentFiles.iterator();
        while (iterator.hasNext()) {
            RecentFile existing = iterator.next();
            if (isSameFile(existing, file)) {
                iterator.remove();
                break;
            }
        }

        file.lastOpened = System.currentTimeMillis();
        recentFiles.add(0, file);

        while (recentFiles.size() > MAX_RECENT_FILES) {
            recentFiles.remove(recentFiles.size() - 1);
        }

        saveRecentFiles(recentFiles);
    }

    public List<RecentFile> getRecentFiles() {
        String json = prefs.getString(KEY_RECENT_FILES, "[]");
        Type type = new TypeToken<ArrayList<RecentFile>>(){}.getType();
        List<RecentFile> files = gson.fromJson(json, type);
        return files != null ? files : new ArrayList<>();
    }

    public void removeRecentFile(RecentFile file) {
        List<RecentFile> recentFiles = getRecentFiles();
        
        Iterator<RecentFile> iterator = recentFiles.iterator();
        while (iterator.hasNext()) {
            RecentFile existing = iterator.next();
            if (isSameFile(existing, file)) {
                iterator.remove();
                break;
            }
        }

        saveRecentFiles(recentFiles);
    }

    public void clearRecentFiles() {
        prefs.edit().remove(KEY_RECENT_FILES).apply();
    }

    private void saveRecentFiles(List<RecentFile> files) {
        String json = gson.toJson(files);
        prefs.edit().putString(KEY_RECENT_FILES, json).apply();
    }

    private boolean isSameFile(RecentFile a, RecentFile b) {
        if (a.isGitHub && b.isGitHub) {
            return a.path != null && a.path.equals(b.path) &&
                   a.owner != null && a.owner.equals(b.owner) &&
                   a.repo != null && a.repo.equals(b.repo);
        } else if (!a.isGitHub && !b.isGitHub) {
            return a.localUri != null && a.localUri.equals(b.localUri);
        }
        return false;
    }
}
