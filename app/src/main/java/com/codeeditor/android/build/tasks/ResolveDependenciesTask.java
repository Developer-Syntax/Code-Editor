package com.codeeditor.android.build.tasks;

import com.codeeditor.android.build.BuildContext;
import com.codeeditor.android.build.BuildException;
import com.codeeditor.android.build.BuildTask;
import com.codeeditor.android.build.DependencyResolver;
import com.codeeditor.android.build.DependencyResolver.Dependency;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

public class ResolveDependenciesTask implements BuildTask {
    
    private AtomicBoolean cancelled = new AtomicBoolean(false);
    private AtomicInteger progress = new AtomicInteger(0);
    private String progressMessage = "Resolving dependencies...";
    private DependencyResolver resolver;
    
    @Override
    public String getName() {
        return "Resolve Dependencies";
    }
    
    @Override
    public boolean execute(BuildContext context) throws BuildException {
        context.phaseStarted("Dependency Resolution");
        
        try {
            File projectDir = context.getConfig().getProjectDir();
            File cacheDir = new File(context.getSdkManager().getSdkDir(), "maven-cache");
            
            resolver = new DependencyResolver(cacheDir);
            
            resolver.setListener(new DependencyResolver.DownloadListener() {
                @Override
                public void onProgress(String dependency, int depProgress, String message) {
                    progressMessage = message;
                    context.progress(progress.get(), progressMessage);
                }
                
                @Override
                public void onCompleted(String dependency, File file) {
                    context.log("Resolved: " + dependency);
                }
                
                @Override
                public void onFailed(String dependency, String error) {
                    context.warning("Failed to resolve: " + dependency + " - " + error);
                }
                
                @Override
                public void onLog(String message) {
                    context.log(message);
                }
            });
            
            List<Dependency> dependencies = collectDependencies(context, projectDir);
            
            if (dependencies.isEmpty()) {
                context.log("No external dependencies found");
                context.phaseCompleted("Dependency Resolution");
                return true;
            }
            
            context.log("Found " + dependencies.size() + " dependencies to resolve");
            
            List<File> resolvedFiles = resolver.resolveDependencies(dependencies);
            
            List<File> jarFiles = new ArrayList<>();
            File extractDir = new File(context.getBuildDir(), "extracted-aars");
            extractDir.mkdirs();
            
            for (File file : resolvedFiles) {
                if (cancelled.get()) {
                    throw new BuildException("Dependencies", "Cancelled");
                }
                
                if (file.getName().endsWith(".aar")) {
                    File extractedJar = resolver.extractClassesFromAar(file, extractDir);
                    if (extractedJar != null) {
                        jarFiles.add(extractedJar);
                        context.log("Extracted: " + file.getName() + " -> classes.jar");
                    }
                } else if (file.getName().endsWith(".jar")) {
                    jarFiles.add(file);
                }
            }
            
            context.putArtifact("dependency_jars", jarFiles);
            
            String classpath = resolver.buildClasspath(jarFiles);
            context.putArtifact("dependency_classpath", classpath);
            
            context.log("Resolved " + jarFiles.size() + " JAR files for compilation");
            context.log("Dependencies cache size: " + (resolver.getCacheSize() / 1024 / 1024) + " MB");
            
            progress.set(100);
            progressMessage = "Dependencies resolved";
            context.progress(100, progressMessage);
            
            context.phaseCompleted("Dependency Resolution");
            return true;
            
        } catch (Exception e) {
            throw new BuildException("Dependencies", "Dependency resolution failed: " + e.getMessage(), e);
        } finally {
            if (resolver != null) {
                resolver.shutdown();
            }
        }
    }
    
    private List<Dependency> collectDependencies(BuildContext context, File projectDir) {
        List<Dependency> dependencies = new ArrayList<>();
        
        File buildGradle = new File(projectDir, "build.gradle");
        if (buildGradle.exists()) {
            context.log("Parsing build.gradle for dependencies...");
            List<Dependency> gradleDeps = resolver.parseDependenciesFromGradle(buildGradle);
            dependencies.addAll(gradleDeps);
        }
        
        File buildGradleKts = new File(projectDir, "build.gradle.kts");
        if (buildGradleKts.exists()) {
            context.log("Parsing build.gradle.kts for dependencies...");
            List<Dependency> ktsDeps = resolver.parseDependenciesFromGradle(buildGradleKts);
            dependencies.addAll(ktsDeps);
        }
        
        File depsFile = new File(projectDir, "dependencies.txt");
        if (depsFile.exists()) {
            context.log("Parsing dependencies.txt...");
            List<Dependency> txtDeps = parseDependenciesFile(depsFile);
            dependencies.addAll(txtDeps);
        }
        
        List<Dependency> configDeps = context.getDependencies();
        if (configDeps != null) {
            dependencies.addAll(configDeps);
        }
        
        return dependencies;
    }
    
    private List<Dependency> parseDependenciesFile(File file) {
        List<Dependency> dependencies = new ArrayList<>();
        
        try (java.io.BufferedReader reader = new java.io.BufferedReader(
                new java.io.FileReader(file))) {
            String line;
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                
                if (line.isEmpty() || line.startsWith("#") || line.startsWith("//")) {
                    continue;
                }
                
                Dependency dep = Dependency.parse(line);
                if (dep != null) {
                    dependencies.add(dep);
                }
            }
        } catch (Exception e) {
        }
        
        return dependencies;
    }
    
    @Override
    public void cancel() {
        cancelled.set(true);
        if (resolver != null) {
            resolver.shutdown();
        }
    }
    
    @Override
    public int getProgress() {
        return progress.get();
    }
    
    @Override
    public String getProgressMessage() {
        return progressMessage;
    }
}
