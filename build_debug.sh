#!/bin/bash

echo "=========================================="
echo "  Building Debug APK"
echo "=========================================="

ANDROID_HOME="${ANDROID_HOME:-$HOME/android-sdk}"
export ANDROID_HOME
export ANDROID_SDK_ROOT="$ANDROID_HOME"

install_jdk() {
    JDK_DIR="$HOME/jdk"
    JDK_VERSION="17.0.9+9"
    JDK_PATH="$JDK_DIR/jdk-$JDK_VERSION"
    
    if [ -d "$JDK_PATH" ] && [ -f "$JDK_PATH/bin/java" ]; then
        echo "[JDK] OpenJDK 17 already installed"
        export JAVA_HOME="$JDK_PATH"
        export PATH="$JAVA_HOME/bin:$PATH"
        return 0
    fi
    
    echo "[JDK] Installing OpenJDK 17..."
    mkdir -p "$JDK_DIR"
    
    JDK_URL="https://github.com/adoptium/temurin17-binaries/releases/download/jdk-17.0.9%2B9/OpenJDK17U-jdk_x64_linux_hotspot_17.0.9_9.tar.gz"
    
    echo "[JDK] Downloading from Adoptium..."
    wget -q --show-progress "$JDK_URL" -O /tmp/openjdk17.tar.gz
    
    if [ $? -ne 0 ]; then
        echo "[JDK] Failed to download JDK"
        return 1
    fi
    
    echo "[JDK] Extracting..."
    tar -xzf /tmp/openjdk17.tar.gz -C "$JDK_DIR"
    rm -f /tmp/openjdk17.tar.gz
    
    if [ -d "$JDK_PATH" ] && [ -f "$JDK_PATH/bin/java" ]; then
        echo "[JDK] OpenJDK 17 installed successfully!"
        export JAVA_HOME="$JDK_PATH"
        export PATH="$JAVA_HOME/bin:$PATH"
        return 0
    else
        echo "[JDK] Failed to install JDK"
        return 1
    fi
}

install_android_sdk() {
    if [ -f "$ANDROID_HOME/cmdline-tools/latest/bin/sdkmanager" ] && \
       [ -d "$ANDROID_HOME/platforms/android-34" ] && \
       [ -d "$ANDROID_HOME/build-tools/34.0.0" ]; then
        echo "[SDK] Android SDK already installed"
        return 0
    fi
    
    echo "[SDK] Installing Android SDK..."
    
    mkdir -p "$ANDROID_HOME/cmdline-tools"
    
    if [ ! -f "$ANDROID_HOME/cmdline-tools/latest/bin/sdkmanager" ]; then
        echo "[SDK] Downloading command line tools..."
        wget -q --show-progress https://dl.google.com/android/repository/commandlinetools-linux-11076708_latest.zip -O /tmp/cmdline-tools.zip
        
        if [ $? -ne 0 ]; then
            echo "[SDK] Failed to download SDK tools"
            return 1
        fi
        
        echo "[SDK] Extracting..."
        unzip -q -o /tmp/cmdline-tools.zip -d "$ANDROID_HOME/cmdline-tools"
        mv "$ANDROID_HOME/cmdline-tools/cmdline-tools" "$ANDROID_HOME/cmdline-tools/latest" 2>/dev/null || true
        rm -f /tmp/cmdline-tools.zip
    fi
    
    echo "[SDK] Accepting licenses..."
    yes | "$ANDROID_HOME/cmdline-tools/latest/bin/sdkmanager" --licenses --sdk_root="$ANDROID_HOME" > /dev/null 2>&1
    
    echo "[SDK] Installing platform-tools, build-tools, and platform..."
    "$ANDROID_HOME/cmdline-tools/latest/bin/sdkmanager" \
        "platform-tools" \
        "platforms;android-34" \
        "build-tools;34.0.0" \
        --sdk_root="$ANDROID_HOME"
    
    if [ $? -eq 0 ]; then
        echo "[SDK] Android SDK installed successfully!"
        return 0
    else
        echo "[SDK] Failed to install SDK packages"
        return 1
    fi
}

create_local_properties() {
    LOCAL_PROPS="local.properties"
    echo "[CONFIG] Creating $LOCAL_PROPS..."
    cat > "$LOCAL_PROPS" << EOF
sdk.dir=$ANDROID_HOME
EOF
    echo "[CONFIG] $LOCAL_PROPS created with sdk.dir=$ANDROID_HOME"
}

echo ""
echo "[STEP 1/4] Setting up JDK..."
install_jdk
if [ $? -ne 0 ]; then
    echo "Failed to setup JDK"
    exit 1
fi

echo "[JDK] Using: $JAVA_HOME"
java -version 2>&1 | head -1

echo ""
echo "[STEP 2/4] Setting up Android SDK..."
install_android_sdk
if [ $? -ne 0 ]; then
    echo "Failed to setup Android SDK"
    exit 1
fi

export PATH="$ANDROID_HOME/cmdline-tools/latest/bin:$ANDROID_HOME/platform-tools:$ANDROID_HOME/build-tools/34.0.0:$PATH"

echo ""
echo "[STEP 3/4] Creating configuration..."
create_local_properties

echo ""
echo "[STEP 4/4] Starting Gradle build..."
echo ""

echo "[BUILD] Cleaning previous build..."
./gradlew clean --no-daemon 2>/dev/null || true

echo "[BUILD] Assembling Debug APK..."
./gradlew assembleDebug --no-daemon --stacktrace

if [ $? -eq 0 ]; then
    echo ""
    echo "=========================================="
    echo "  Build Successful!"
    echo "=========================================="
    
    APK_PATH="app/build/outputs/apk/debug/app-debug.apk"
    
    if [ -f "$APK_PATH" ]; then
        APK_SIZE=$(du -h "$APK_PATH" | cut -f1)
        echo "APK Location: $APK_PATH"
        echo "APK Size: $APK_SIZE"
        
        mkdir -p output
        cp "$APK_PATH" "output/app-debug-$(date +%Y%m%d_%H%M%S).apk"
        cp "$APK_PATH" "output/app-debug.apk"
        echo "Copied to: output/app-debug.apk"
    else
        echo "Warning: APK file not found at expected location"
        find . -name "*.apk" -type f 2>/dev/null | head -5
    fi
else
    echo ""
    echo "=========================================="
    echo "  Build Failed!"
    echo "=========================================="
    exit 1
fi
