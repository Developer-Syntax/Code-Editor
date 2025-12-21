package com.codeeditor.android.build.tasks;

import com.codeeditor.android.build.BuildContext;
import com.codeeditor.android.build.BuildException;
import com.codeeditor.android.build.BuildTask;
import com.codeeditor.android.build.SdkManager;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

public class CompileNativeTask implements BuildTask {
    
    private AtomicBoolean cancelled = new AtomicBoolean(false);
    private int progress = 0;
    private String progressMessage = "Compiling native code...";
    
    private static final String[] SUPPORTED_ABIS = {"arm64-v8a", "armeabi-v7a", "x86_64", "x86"};
    private static final String[] DEFAULT_ABIS = {"arm64-v8a", "armeabi-v7a"};
    
    @Override
    public String getName() {
        return "Compile Native Code";
    }
    
    @Override
    public boolean execute(BuildContext context) throws BuildException {
        // Check if NDK is enabled in build config
        if (!context.getConfig().isNdkEnabled()) {
            context.log("NDK compilation is disabled in build configuration, skipping");
            return true;
        }
        
        File projectDir = context.getConfig().getProjectDir();
        File jniDir = new File(projectDir, "src/main/jni");
        File cppDir = new File(projectDir, "src/main/cpp");
        
        boolean hasJni = jniDir.exists() && hasNativeFiles(jniDir);
        boolean hasCpp = cppDir.exists() && hasNativeFiles(cppDir);
        
        if (!hasJni && !hasCpp) {
            context.log("No native code found, skipping native compilation");
            return true;
        }
        
        context.phaseStarted("Native Compilation");
        
        try {
            SdkManager sdkManager = context.getSdkManager();
            
            // Check for NDK or Termux availability
            boolean hasNdk = sdkManager.isNdkInstalled();
            boolean hasTermux = sdkManager.isTermuxAvailable();
            
            if (!hasNdk && !hasTermux) {
                context.log("NDK not installed, attempting to install...");
                installNdk(context, sdkManager);
                
                // Recheck after installation
                hasNdk = sdkManager.isNdkInstalled();
                hasTermux = sdkManager.isTermuxAvailable();
                
                if (!hasNdk && !hasTermux) {
                    context.warning("NDK installation incomplete. Native compilation may fail.");
                    context.warning("For best results, install Termux and run: pkg install clang llvm");
                }
            }
            
            if (hasTermux && !hasNdk) {
                context.log("Using Termux clang for native compilation");
            } else if (hasNdk) {
                context.log("Using NDK toolchain for native compilation");
            }
            
            File nativeDir = hasJni ? jniDir : cppDir;
            File outputLibsDir = new File(context.getConfig().getIntermediatesDir(), "native_libs");
            outputLibsDir.mkdirs();
            
            File cmakeLists = new File(nativeDir, "CMakeLists.txt");
            File androidMk = new File(nativeDir, "Android.mk");
            
            // Check what toolchain is available
            boolean hasFullNdk = sdkManager.isNdkInstalled() && sdkManager.getSysroot().exists();
            boolean hasTermuxCmake = sdkManager.isCmakeAvailable() && sdkManager.isNinjaAvailable();
            
            // CMake requires full NDK (for android.toolchain.cmake) OR Termux cmake with NDK sysroot
            // ndk-build requires full NDK installation  
            // Direct compilation requires either NDK clang or Termux clang
            if (cmakeLists.exists()) {
                File toolchainFile = new File(sdkManager.getNdkDir(), "build/cmake/android.toolchain.cmake");
                if (hasFullNdk && hasTermuxCmake && toolchainFile.exists()) {
                    context.log("Found CMakeLists.txt, using CMake build with NDK toolchain...");
                    buildWithCmake(context, nativeDir, outputLibsDir);
                } else if (hasFullNdk) {
                    context.log("CMakeLists.txt found but CMake not available, using direct compilation...");
                    context.warning("For CMake support, install Termux and run: pkg install cmake ninja");
                    buildDirectly(context, nativeDir, outputLibsDir);
                } else {
                    context.error("CMake builds require full AndroidIDE NDK installation.");
                    context.error("Please wait for NDK download to complete or install Termux with: pkg install clang");
                    throw new BuildException("CMake Build", 
                        "CMakeLists.txt found but no NDK sysroot available. NDK required for CMake Android builds.");
                }
            } else if (androidMk.exists()) {
                File ndkBuild = sdkManager.getNdkBuild();
                if (hasFullNdk && ndkBuild.exists() && ndkBuild.canExecute()) {
                    context.log("Found Android.mk, using ndk-build...");
                    buildWithNdkBuild(context, nativeDir, outputLibsDir);
                } else if (hasFullNdk) {
                    context.log("Android.mk found but ndk-build not available, using direct compilation...");
                    buildDirectly(context, nativeDir, outputLibsDir);
                } else {
                    context.error("ndk-build requires full AndroidIDE NDK installation.");
                    throw new BuildException("NDK Build", 
                        "Android.mk found but no NDK available. Install full NDK for ndk-build support.");
                }
            } else {
                // Direct compilation - can work with NDK clang or Termux clang (with NDK sysroot)
                if (hasFullNdk || hasTermux) {
                    context.log("Using direct compilation for native files...");
                    buildDirectly(context, nativeDir, outputLibsDir);
                } else {
                    context.error("No native toolchain available for compilation.");
                    throw new BuildException("Native Compile", 
                        "No C/C++ compiler found. Install Termux with: pkg install clang llvm");
                }
            }
            
            context.putArtifact("native_libs_dir", outputLibsDir);
            context.phaseCompleted("Native Compilation");
            
            return true;
            
        } catch (Exception e) {
            throw new BuildException("Native Compile", "Native compilation failed: " + e.getMessage(), e);
        }
    }
    
    private boolean hasNativeFiles(File dir) {
        if (!dir.exists() || !dir.isDirectory()) return false;
        
        File[] files = dir.listFiles();
        if (files == null) return false;
        
        for (File file : files) {
            if (file.isFile()) {
                String name = file.getName().toLowerCase();
                if (name.endsWith(".c") || name.endsWith(".cpp") || 
                    name.endsWith(".cc") || name.endsWith(".cxx") ||
                    name.endsWith(".h") || name.endsWith(".hpp") ||
                    name.equals("cmakelists.txt") || name.equals("android.mk")) {
                    return true;
                }
            } else if (file.isDirectory()) {
                if (hasNativeFiles(file)) return true;
            }
        }
        return false;
    }
    
    private void installNdk(BuildContext context, SdkManager sdkManager) throws BuildException {
        final Object lock = new Object();
        final boolean[] installComplete = {false};
        final String[] installError = {null};
        
        sdkManager.setListener(new SdkManager.DownloadListener() {
            @Override
            public void onProgress(int progress, String message) {
                context.progress(progress / 4, "NDK: " + message);
            }
            
            @Override
            public void onCompleted(boolean success, String message) {
                synchronized (lock) {
                    installComplete[0] = true;
                    lock.notifyAll();
                }
            }
            
            @Override
            public void onError(String error) {
                synchronized (lock) {
                    installComplete[0] = true;
                    installError[0] = error;
                    lock.notifyAll();
                }
            }
        });
        
        sdkManager.downloadAndInstallNdk();
        
        synchronized (lock) {
            long timeout = System.currentTimeMillis() + 600000;
            while (!installComplete[0] && installError[0] == null) {
                long remaining = timeout - System.currentTimeMillis();
                if (remaining <= 0) {
                    throw new BuildException("NDK Install", "NDK installation timed out");
                }
                try {
                    lock.wait(remaining);
                } catch (InterruptedException e) {
                    throw new BuildException("NDK Install", "NDK installation interrupted");
                }
            }
        }
        
        if (installError[0] != null) {
            throw new BuildException("NDK Install", "NDK installation failed: " + installError[0]);
        }
    }
    
    private String[] getConfiguredAbis(BuildContext context) {
        String[] configAbis = context.getConfig().getNdkAbiFilters();
        if (configAbis != null && configAbis.length > 0) {
            return configAbis;
        }
        return DEFAULT_ABIS;
    }
    
    private void buildWithCmake(BuildContext context, File sourceDir, File outputDir) 
            throws IOException, InterruptedException, BuildException {
        
        SdkManager sdkManager = context.getSdkManager();
        File cmake = sdkManager.getCmake();
        File ninja = sdkManager.getNinja();
        File ndkDir = sdkManager.getNdkDir();
        File toolchainFile = new File(ndkDir, "build/cmake/android.toolchain.cmake");
        
        String[] targetAbis = getConfiguredAbis(context);
        int abiIndex = 0;
        for (String abi : targetAbis) {
            if (cancelled.get()) return;
            
            progress = 20 + (abiIndex * 30);
            progressMessage = "Building for " + abi + "...";
            context.progress(progress, progressMessage);
            
            File buildDir = new File(outputDir, "cmake_build_" + abi);
            buildDir.mkdirs();
            
            File abiOutputDir = new File(outputDir, abi);
            abiOutputDir.mkdirs();
            
            List<String> cmakeArgs = new ArrayList<>();
            cmakeArgs.add(cmake.getAbsolutePath());
            cmakeArgs.add("-G");
            cmakeArgs.add("Ninja");
            cmakeArgs.add("-DCMAKE_TOOLCHAIN_FILE=" + toolchainFile.getAbsolutePath());
            cmakeArgs.add("-DANDROID_ABI=" + abi);
            cmakeArgs.add("-DANDROID_PLATFORM=android-" + context.getConfig().getMinSdkVersion());
            cmakeArgs.add("-DANDROID_NDK=" + ndkDir.getAbsolutePath());
            cmakeArgs.add("-DCMAKE_MAKE_PROGRAM=" + ninja.getAbsolutePath());
            cmakeArgs.add("-DCMAKE_LIBRARY_OUTPUT_DIRECTORY=" + abiOutputDir.getAbsolutePath());
            cmakeArgs.add("-DCMAKE_BUILD_TYPE=" + (context.getConfig().isDebugBuild() ? "Debug" : "Release"));
            cmakeArgs.add(sourceDir.getAbsolutePath());
            
            runCommand(context, cmakeArgs, buildDir);
            
            List<String> buildArgs = new ArrayList<>();
            buildArgs.add(cmake.getAbsolutePath());
            buildArgs.add("--build");
            buildArgs.add(buildDir.getAbsolutePath());
            buildArgs.add("--parallel");
            
            runCommand(context, buildArgs, buildDir);
            
            abiIndex++;
        }
        
        progress = 90;
        progressMessage = "Native libraries built successfully";
        context.progress(progress, progressMessage);
    }
    
    private void buildWithNdkBuild(BuildContext context, File jniDir, File outputDir) 
            throws IOException, InterruptedException, BuildException {
        
        SdkManager sdkManager = context.getSdkManager();
        File ndkBuild = sdkManager.getNdkBuild();
        File projectDir = context.getConfig().getProjectDir();
        
        progress = 30;
        progressMessage = "Running ndk-build...";
        context.progress(progress, progressMessage);
        
        String[] targetAbis = getConfiguredAbis(context);
        StringBuilder abiFilter = new StringBuilder();
        for (int i = 0; i < targetAbis.length; i++) {
            if (i > 0) abiFilter.append(" ");
            abiFilter.append(targetAbis[i]);
        }
        
        List<String> args = new ArrayList<>();
        args.add(ndkBuild.getAbsolutePath());
        args.add("NDK_PROJECT_PATH=" + projectDir.getAbsolutePath());
        args.add("NDK_APPLICATION_MK=" + new File(jniDir, "Application.mk").getAbsolutePath());
        args.add("APP_BUILD_SCRIPT=" + new File(jniDir, "Android.mk").getAbsolutePath());
        args.add("NDK_LIBS_OUT=" + outputDir.getAbsolutePath());
        args.add("NDK_OUT=" + new File(outputDir, "obj").getAbsolutePath());
        args.add("APP_ABI=" + abiFilter.toString());
        args.add("-j4");
        
        if (context.getConfig().isDebugBuild()) {
            args.add("NDK_DEBUG=1");
        }
        
        runCommand(context, args, projectDir);
        
        progress = 90;
        progressMessage = "ndk-build completed";
        context.progress(progress, progressMessage);
    }
    
    private void buildDirectly(BuildContext context, File sourceDir, File outputDir) 
            throws IOException, InterruptedException, BuildException {
        
        SdkManager sdkManager = context.getSdkManager();
        
        List<File> sourceFiles = new ArrayList<>();
        collectSourceFiles(sourceDir, sourceFiles);
        
        if (sourceFiles.isEmpty()) {
            context.log("No C/C++ source files found");
            return;
        }
        
        context.log("Found " + sourceFiles.size() + " source files");
        
        String[] targetAbis = getConfiguredAbis(context);
        int abiIndex = 0;
        for (String abi : targetAbis) {
            if (cancelled.get()) return;
            
            progress = 20 + (abiIndex * 30);
            progressMessage = "Compiling for " + abi + "...";
            context.progress(progress, progressMessage);
            
            File abiOutputDir = new File(outputDir, abi);
            abiOutputDir.mkdirs();
            
            File objDir = new File(outputDir, "obj_" + abi);
            objDir.mkdirs();
            
            List<File> objectFiles = new ArrayList<>();
            
            for (File sourceFile : sourceFiles) {
                if (cancelled.get()) return;
                
                File objectFile = new File(objDir, 
                    sourceFile.getName().replaceAll("\\.(c|cpp|cc|cxx)$", ".o"));
                
                compileSourceFile(context, sdkManager, abi, sourceFile, objectFile);
                objectFiles.add(objectFile);
            }
            
            String libName = "lib" + context.getConfig().getProjectName().toLowerCase()
                .replaceAll("[^a-z0-9]", "") + ".so";
            File outputLib = new File(abiOutputDir, libName);
            
            linkSharedLibrary(context, sdkManager, abi, objectFiles, outputLib);
            
            context.log("Built: " + outputLib.getName() + " for " + abi);
            
            abiIndex++;
        }
        
        progress = 90;
        progressMessage = "Direct compilation completed";
        context.progress(progress, progressMessage);
    }
    
    private void collectSourceFiles(File dir, List<File> files) {
        File[] contents = dir.listFiles();
        if (contents == null) return;
        
        for (File file : contents) {
            if (file.isDirectory()) {
                collectSourceFiles(file, files);
            } else {
                String name = file.getName().toLowerCase();
                if (name.endsWith(".c") || name.endsWith(".cpp") || 
                    name.endsWith(".cc") || name.endsWith(".cxx")) {
                    files.add(file);
                }
            }
        }
    }
    
    private void compileSourceFile(BuildContext context, SdkManager sdkManager, 
            String abi, File sourceFile, File objectFile) 
            throws IOException, InterruptedException, BuildException {
        
        boolean hasNdk = sdkManager.isNdkInstalled();
        boolean hasTermux = sdkManager.isTermuxAvailable();
        
        File compiler;
        File sysroot = sdkManager.getSysroot();
        boolean useSysroot = hasNdk && sysroot.exists();
        
        if (sourceFile.getName().endsWith(".c")) {
            compiler = sdkManager.getClang();
            // Fallback to Termux if NDK clang not available
            if (!compiler.exists() && hasTermux) {
                compiler = new File("/data/data/com.termux/files/usr/bin/clang");
            }
        } else {
            compiler = sdkManager.getClangPlusPlus();
            // Fallback to Termux if NDK clang++ not available
            if (!compiler.exists() && hasTermux) {
                compiler = new File("/data/data/com.termux/files/usr/bin/clang++");
            }
        }
        
        if (!compiler.exists()) {
            throw new BuildException("Native Compile", 
                "No C/C++ compiler available. Install Termux with clang or full NDK.");
        }
        
        String target = getTargetTriple(abi, context.getConfig().getMinSdkVersion());
        
        List<String> args = new ArrayList<>();
        args.add(compiler.getAbsolutePath());
        args.add("-target");
        args.add(target);
        
        // Only add sysroot if we have a valid NDK installation
        if (useSysroot) {
            args.add("--sysroot=" + sysroot.getAbsolutePath());
        }
        
        args.add("-fPIC");
        args.add("-ffunction-sections");
        args.add("-fdata-sections");
        
        if (context.getConfig().isDebugBuild()) {
            args.add("-g");
            args.add("-O0");
        } else {
            args.add("-O2");
            args.add("-DNDEBUG");
        }
        
        args.add("-I" + sourceFile.getParentFile().getAbsolutePath());
        args.add("-c");
        args.add(sourceFile.getAbsolutePath());
        args.add("-o");
        args.add(objectFile.getAbsolutePath());
        
        runCommand(context, args, sourceFile.getParentFile());
    }
    
    private void linkSharedLibrary(BuildContext context, SdkManager sdkManager,
            String abi, List<File> objectFiles, File outputLib) 
            throws IOException, InterruptedException, BuildException {
        
        boolean hasNdk = sdkManager.isNdkInstalled();
        boolean hasTermux = sdkManager.isTermuxAvailable();
        
        File linker = sdkManager.getClangPlusPlus();
        File sysroot = sdkManager.getSysroot();
        boolean useSysroot = hasNdk && sysroot.exists();
        
        // Fallback to Termux if NDK linker not available
        if (!linker.exists() && hasTermux) {
            linker = new File("/data/data/com.termux/files/usr/bin/clang++");
        }
        
        if (!linker.exists()) {
            throw new BuildException("Native Link", 
                "No linker available. Install Termux with clang or full NDK.");
        }
        
        String target = getTargetTriple(abi, context.getConfig().getMinSdkVersion());
        
        List<String> args = new ArrayList<>();
        args.add(linker.getAbsolutePath());
        args.add("-target");
        args.add(target);
        
        // Only add sysroot if we have a valid NDK installation
        if (useSysroot) {
            args.add("--sysroot=" + sysroot.getAbsolutePath());
        }
        
        args.add("-shared");
        args.add("-Wl,--gc-sections");
        args.add("-Wl,--build-id=sha1");
        
        if (!context.getConfig().isDebugBuild()) {
            args.add("-Wl,--strip-all");
        }
        
        for (File objFile : objectFiles) {
            args.add(objFile.getAbsolutePath());
        }
        
        // Only link Android NDK libraries if we have sysroot
        if (useSysroot) {
            args.add("-llog");
            args.add("-landroid");
        }
        args.add("-lm");
        args.add("-lc");
        
        args.add("-o");
        args.add(outputLib.getAbsolutePath());
        
        runCommand(context, args, outputLib.getParentFile());
    }
    
    private String getTargetTriple(String abi, int minSdk) {
        switch (abi) {
            case "arm64-v8a":
                return "aarch64-linux-android" + minSdk;
            case "armeabi-v7a":
                return "armv7a-linux-androideabi" + minSdk;
            case "x86_64":
                return "x86_64-linux-android" + minSdk;
            case "x86":
                return "i686-linux-android" + minSdk;
            default:
                return "aarch64-linux-android" + minSdk;
        }
    }
    
    private void runCommand(BuildContext context, List<String> args, File workDir) 
            throws IOException, InterruptedException, BuildException {
        
        ProcessBuilder pb = new ProcessBuilder(args);
        pb.directory(workDir);
        pb.redirectErrorStream(true);
        
        context.log("Running: " + String.join(" ", args));
        
        Process process = pb.start();
        
        StringBuilder output = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(process.getInputStream()))) {
            String line;
            while ((line = reader.readLine()) != null) {
                output.append(line).append("\n");
                context.log(line);
            }
        }
        
        int exitCode = process.waitFor();
        
        if (exitCode != 0) {
            throw new BuildException("Native Compile", 
                "Command failed with exit code " + exitCode + "\n" + output.toString());
        }
    }
    
    @Override
    public void cancel() {
        cancelled.set(true);
    }
    
    @Override
    public int getProgress() {
        return progress;
    }
    
    @Override
    public String getProgressMessage() {
        return progressMessage;
    }
}
