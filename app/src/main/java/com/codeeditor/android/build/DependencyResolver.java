package com.codeeditor.android.build;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class DependencyResolver {
    
    private static final String[] MAVEN_REPOSITORIES = {
        "https://repo1.maven.org/maven2",
        "https://dl.google.com/dl/android/maven2",
        "https://jcenter.bintray.com",
        "https://jitpack.io"
    };
    
    private File cacheDir;
    private Map<String, File> resolvedDependencies;
    private Set<String> failedDependencies;
    private DownloadListener listener;
    private ExecutorService executor;
    
    public interface DownloadListener {
        void onProgress(String dependency, int progress, String message);
        void onCompleted(String dependency, File file);
        void onFailed(String dependency, String error);
        void onLog(String message);
    }
    
    public static class Dependency {
        public String groupId;
        public String artifactId;
        public String version;
        public String scope;
        public boolean transitive;
        
        public Dependency(String groupId, String artifactId, String version) {
            this.groupId = groupId;
            this.artifactId = artifactId;
            this.version = version;
            this.scope = "compile";
            this.transitive = true;
        }
        
        public static Dependency parse(String notation) {
            String[] parts = notation.split(":");
            if (parts.length >= 3) {
                return new Dependency(parts[0], parts[1], parts[2]);
            }
            return null;
        }
        
        public String getCoordinate() {
            return groupId + ":" + artifactId + ":" + version;
        }
        
        public String getFileName() {
            return artifactId + "-" + version + ".jar";
        }
        
        public String getAarFileName() {
            return artifactId + "-" + version + ".aar";
        }
        
        public String getMavenPath() {
            return groupId.replace('.', '/') + "/" + artifactId + "/" + version;
        }
        
        @Override
        public String toString() {
            return getCoordinate();
        }
        
        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            Dependency that = (Dependency) o;
            return getCoordinate().equals(that.getCoordinate());
        }
        
        @Override
        public int hashCode() {
            return getCoordinate().hashCode();
        }
    }
    
    public DependencyResolver(File cacheDir) {
        this.cacheDir = cacheDir;
        this.resolvedDependencies = new HashMap<>();
        this.failedDependencies = new HashSet<>();
        this.executor = Executors.newFixedThreadPool(4);
        
        cacheDir.mkdirs();
    }
    
    public void setListener(DownloadListener listener) {
        this.listener = listener;
    }
    
    public List<File> resolveDependencies(List<Dependency> dependencies) {
        List<File> resolvedFiles = new ArrayList<>();
        Set<Dependency> allDependencies = new HashSet<>();
        
        for (Dependency dep : dependencies) {
            allDependencies.add(dep);
        }
        
        log("Resolving " + allDependencies.size() + " dependencies...");
        
        List<Future<File>> futures = new ArrayList<>();
        
        for (Dependency dep : allDependencies) {
            Future<File> future = executor.submit(() -> resolveSingleDependency(dep));
            futures.add(future);
        }
        
        for (Future<File> future : futures) {
            try {
                File file = future.get(60, TimeUnit.SECONDS);
                if (file != null && file.exists()) {
                    resolvedFiles.add(file);
                }
            } catch (Exception e) {
                log("Dependency resolution failed: " + e.getMessage());
            }
        }
        
        log("Resolved " + resolvedFiles.size() + " dependencies");
        
        return resolvedFiles;
    }
    
    private File resolveSingleDependency(Dependency dep) {
        String coordinate = dep.getCoordinate();
        
        if (resolvedDependencies.containsKey(coordinate)) {
            return resolvedDependencies.get(coordinate);
        }
        
        if (failedDependencies.contains(coordinate)) {
            return null;
        }
        
        File cachedJar = new File(cacheDir, dep.groupId.replace('.', '/') + "/" + 
            dep.artifactId + "/" + dep.version + "/" + dep.getFileName());
        
        if (cachedJar.exists() && cachedJar.length() > 0) {
            log("Using cached: " + dep.getCoordinate());
            resolvedDependencies.put(coordinate, cachedJar);
            if (listener != null) {
                listener.onCompleted(coordinate, cachedJar);
            }
            return cachedJar;
        }
        
        File cachedAar = new File(cacheDir, dep.groupId.replace('.', '/') + "/" + 
            dep.artifactId + "/" + dep.version + "/" + dep.getAarFileName());
        
        if (cachedAar.exists() && cachedAar.length() > 0) {
            log("Using cached AAR: " + dep.getCoordinate());
            resolvedDependencies.put(coordinate, cachedAar);
            if (listener != null) {
                listener.onCompleted(coordinate, cachedAar);
            }
            return cachedAar;
        }
        
        log("Downloading: " + dep.getCoordinate());
        
        for (String repo : MAVEN_REPOSITORIES) {
            File downloaded = downloadFromRepository(dep, repo, cachedJar);
            if (downloaded != null) {
                resolvedDependencies.put(coordinate, downloaded);
                if (listener != null) {
                    listener.onCompleted(coordinate, downloaded);
                }
                return downloaded;
            }
            
            downloaded = downloadAarFromRepository(dep, repo, cachedAar);
            if (downloaded != null) {
                resolvedDependencies.put(coordinate, downloaded);
                if (listener != null) {
                    listener.onCompleted(coordinate, downloaded);
                }
                return downloaded;
            }
        }
        
        log("Failed to resolve: " + dep.getCoordinate());
        failedDependencies.add(coordinate);
        if (listener != null) {
            listener.onFailed(coordinate, "Dependency not found in any repository");
        }
        
        return null;
    }
    
    private File downloadFromRepository(Dependency dep, String repoUrl, File destFile) {
        String jarUrl = repoUrl + "/" + dep.getMavenPath() + "/" + dep.getFileName();
        
        return downloadFile(jarUrl, destFile, dep.getCoordinate());
    }
    
    private File downloadAarFromRepository(Dependency dep, String repoUrl, File destFile) {
        String aarUrl = repoUrl + "/" + dep.getMavenPath() + "/" + dep.getAarFileName();
        
        return downloadFile(aarUrl, destFile, dep.getCoordinate());
    }
    
    private File downloadFile(String urlString, File destFile, String depName) {
        HttpURLConnection conn = null;
        
        try {
            URL url = new URL(urlString);
            conn = (HttpURLConnection) url.openConnection();
            conn.setRequestProperty("User-Agent", "CodeEditor-Android/2.0");
            conn.setConnectTimeout(15000);
            conn.setReadTimeout(30000);
            conn.setInstanceFollowRedirects(true);
            
            int responseCode = conn.getResponseCode();
            
            if (responseCode == 301 || responseCode == 302) {
                String newUrl = conn.getHeaderField("Location");
                if (newUrl != null) {
                    conn.disconnect();
                    return downloadFile(newUrl, destFile, depName);
                }
            }
            
            if (responseCode != 200) {
                return null;
            }
            
            destFile.getParentFile().mkdirs();
            
            long totalSize = conn.getContentLengthLong();
            long downloaded = 0;
            
            try (InputStream is = conn.getInputStream();
                 FileOutputStream fos = new FileOutputStream(destFile)) {
                
                byte[] buffer = new byte[8192];
                int bytesRead;
                
                while ((bytesRead = is.read(buffer)) != -1) {
                    fos.write(buffer, 0, bytesRead);
                    downloaded += bytesRead;
                    
                    if (listener != null && totalSize > 0) {
                        int progress = (int) ((downloaded * 100) / totalSize);
                        listener.onProgress(depName, progress, 
                            "Downloading: " + (downloaded / 1024) + " KB / " + (totalSize / 1024) + " KB");
                    }
                }
            }
            
            if (destFile.exists() && destFile.length() > 0) {
                log("Downloaded: " + depName + " (" + destFile.length() / 1024 + " KB)");
                return destFile;
            }
            
        } catch (IOException e) {
        } finally {
            if (conn != null) {
                conn.disconnect();
            }
        }
        
        return null;
    }
    
    public List<Dependency> parseDependenciesFromGradle(File buildGradle) {
        List<Dependency> dependencies = new ArrayList<>();
        
        if (!buildGradle.exists()) {
            return dependencies;
        }
        
        try {
            String content = readFile(buildGradle);
            
            Pattern pattern = Pattern.compile(
                "(implementation|api|compileOnly|runtimeOnly|testImplementation)\\s*['\"]([^'\"]+)['\"]"
            );
            
            Matcher matcher = pattern.matcher(content);
            
            while (matcher.find()) {
                String notation = matcher.group(2);
                Dependency dep = Dependency.parse(notation);
                if (dep != null) {
                    dependencies.add(dep);
                    log("Found dependency: " + dep.getCoordinate());
                }
            }
            
            Pattern kotlinPattern = Pattern.compile(
                "(implementation|api)\\s*\\(\\s*['\"]([^'\"]+)['\"]\\s*\\)"
            );
            
            Matcher kotlinMatcher = kotlinPattern.matcher(content);
            
            while (kotlinMatcher.find()) {
                String notation = kotlinMatcher.group(2);
                Dependency dep = Dependency.parse(notation);
                if (dep != null && !dependencies.contains(dep)) {
                    dependencies.add(dep);
                    log("Found dependency (Kotlin DSL): " + dep.getCoordinate());
                }
            }
            
        } catch (IOException e) {
            log("Error parsing build.gradle: " + e.getMessage());
        }
        
        return dependencies;
    }
    
    public String buildClasspath(List<File> jarFiles) {
        StringBuilder classpath = new StringBuilder();
        
        for (int i = 0; i < jarFiles.size(); i++) {
            if (i > 0) {
                classpath.append(File.pathSeparator);
            }
            classpath.append(jarFiles.get(i).getAbsolutePath());
        }
        
        return classpath.toString();
    }
    
    public File extractClassesFromAar(File aarFile, File outputDir) {
        try {
            String safeName = aarFile.getName().replaceAll("[^a-zA-Z0-9.-]", "_").replace(".aar", "");
            File extractDir = new File(outputDir, safeName);
            extractDir.mkdirs();
            
            File classesJar = new File(extractDir, "classes.jar");
            
            try (java.util.zip.ZipInputStream zis = new java.util.zip.ZipInputStream(
                    new java.io.FileInputStream(aarFile))) {
                
                java.util.zip.ZipEntry entry;
                while ((entry = zis.getNextEntry()) != null) {
                    String entryName = entry.getName();
                    
                    if (entryName.contains("..") || entryName.startsWith("/") || 
                        entryName.startsWith("\\")) {
                        log("Skipping suspicious entry: " + entryName);
                        continue;
                    }
                    
                    if (entryName.equals("classes.jar")) {
                        try (FileOutputStream fos = new FileOutputStream(classesJar)) {
                            byte[] buffer = new byte[8192];
                            int len;
                            while ((len = zis.read(buffer)) > 0) {
                                fos.write(buffer, 0, len);
                            }
                        }
                        break;
                    }
                    zis.closeEntry();
                }
            }
            
            if (classesJar.exists() && classesJar.length() > 0) {
                return classesJar;
            }
            
        } catch (Exception e) {
            log("Error extracting AAR: " + e.getMessage());
        }
        
        return null;
    }
    
    private String readFile(File file) throws IOException {
        StringBuilder content = new StringBuilder();
        
        try (java.io.BufferedReader reader = new java.io.BufferedReader(
                new java.io.FileReader(file))) {
            String line;
            while ((line = reader.readLine()) != null) {
                content.append(line).append("\n");
            }
        }
        
        return content.toString();
    }
    
    private void log(String message) {
        if (listener != null) {
            listener.onLog(message);
        }
    }
    
    public void clearCache() {
        resolvedDependencies.clear();
        failedDependencies.clear();
    }
    
    public long getCacheSize() {
        return calculateDirSize(cacheDir);
    }
    
    private long calculateDirSize(File dir) {
        long size = 0;
        File[] files = dir.listFiles();
        if (files != null) {
            for (File file : files) {
                if (file.isDirectory()) {
                    size += calculateDirSize(file);
                } else {
                    size += file.length();
                }
            }
        }
        return size;
    }
    
    public void shutdown() {
        executor.shutdown();
        try {
            if (!executor.awaitTermination(10, TimeUnit.SECONDS)) {
                executor.shutdownNow();
            }
        } catch (InterruptedException e) {
            executor.shutdownNow();
        }
    }
}
