#!/bin/bash

echo "=========================================="
echo "  Building Release APK"
echo "=========================================="

ANDROID_HOME="${ANDROID_HOME:-$HOME/android-sdk}"
export ANDROID_HOME
export ANDROID_SDK_ROOT="$ANDROID_HOME"

install_android_sdk() {
    if [ -f "$ANDROID_HOME/cmdline-tools/latest/bin/sdkmanager" ]; then
        echo "[SDK] Android SDK already installed"
        return 0
    fi
    
    echo "[SDK] Installing Android SDK..."
    
    mkdir -p "$ANDROID_HOME/cmdline-tools"
    
    echo "[SDK] Downloading command line tools..."
    wget -q --show-progress https://dl.google.com/android/repository/commandlinetools-linux-11076708_latest.zip -O /tmp/cmdline-tools.zip
    
    if [ $? -ne 0 ]; then
        echo "[SDK] Failed to download SDK tools"
        return 1
    fi
    
    echo "[SDK] Extracting..."
    unzip -q -o /tmp/cmdline-tools.zip -d "$ANDROID_HOME/cmdline-tools"
    mv "$ANDROID_HOME/cmdline-tools/cmdline-tools" "$ANDROID_HOME/cmdline-tools/latest"
    rm /tmp/cmdline-tools.zip
    
    echo "[SDK] Accepting licenses..."
    yes | "$ANDROID_HOME/cmdline-tools/latest/bin/sdkmanager" --licenses --sdk_root="$ANDROID_HOME" > /dev/null 2>&1
    
    echo "[SDK] Installing platform-tools and build-tools..."
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

install_android_sdk
if [ $? -ne 0 ]; then
    echo "Failed to setup Android SDK"
    exit 1
fi

export PATH="$ANDROID_HOME/cmdline-tools/latest/bin:$ANDROID_HOME/platform-tools:$ANDROID_HOME/build-tools/34.0.0:$PATH"

KEYSTORE_PATH="${KEYSTORE_PATH:-release-keystore.jks}"
KEYSTORE_PASSWORD="${KEYSTORE_PASSWORD:-android}"
KEY_ALIAS="${KEY_ALIAS:-release-key}"
KEY_PASSWORD="${KEY_PASSWORD:-android}"

if [ ! -f "$KEYSTORE_PATH" ]; then
    echo "[0/5] Generating release keystore..."
    keytool -genkey -v \
        -keystore "$KEYSTORE_PATH" \
        -keyalg RSA \
        -keysize 2048 \
        -validity 10000 \
        -alias "$KEY_ALIAS" \
        -storepass "$KEYSTORE_PASSWORD" \
        -keypass "$KEY_PASSWORD" \
        -dname "CN=Developer, OU=Mobile, O=Company, L=City, ST=State, C=ID"
    
    if [ $? -ne 0 ]; then
        echo "Failed to generate keystore"
        exit 1
    fi
fi

echo "[1/5] Cleaning previous build..."
./gradlew clean 2>/dev/null || true

echo "[2/5] Assembling Release APK..."
./gradlew assembleRelease --stacktrace

if [ $? -eq 0 ]; then
    UNSIGNED_APK="app/build/outputs/apk/release/app-release-unsigned.apk"
    SIGNED_APK="output/app-release-$(date +%Y%m%d_%H%M%S).apk"
    
    mkdir -p output
    
    if [ -f "$UNSIGNED_APK" ]; then
        echo "[3/5] Signing APK..."
        
        jarsigner -verbose \
            -sigalg SHA256withRSA \
            -digestalg SHA-256 \
            -keystore "$KEYSTORE_PATH" \
            -storepass "$KEYSTORE_PASSWORD" \
            -keypass "$KEY_PASSWORD" \
            "$UNSIGNED_APK" \
            "$KEY_ALIAS"
        
        if [ $? -eq 0 ]; then
            echo "[4/5] Verifying signature..."
            jarsigner -verify "$UNSIGNED_APK"
            
            echo "[5/5] Copying signed APK..."
            cp "$UNSIGNED_APK" "$SIGNED_APK"
            
            echo ""
            echo "=========================================="
            echo "  Release Build Successful!"
            echo "=========================================="
            APK_SIZE=$(du -h "$SIGNED_APK" | cut -f1)
            echo "APK Location: $SIGNED_APK"
            echo "APK Size: $APK_SIZE"
        else
            echo "APK signing failed"
            exit 1
        fi
    else
        RELEASE_APK="app/build/outputs/apk/release/app-release.apk"
        if [ -f "$RELEASE_APK" ]; then
            cp "$RELEASE_APK" "$SIGNED_APK"
            echo ""
            echo "=========================================="
            echo "  Release Build Successful!"
            echo "=========================================="
            APK_SIZE=$(du -h "$SIGNED_APK" | cut -f1)
            echo "APK Location: $SIGNED_APK"
            echo "APK Size: $APK_SIZE"
        else
            echo "No APK found. Searching..."
            find . -name "*.apk" -type f 2>/dev/null | head -5
            exit 1
        fi
    fi
else
    echo ""
    echo "=========================================="
    echo "  Build Failed!"
    echo "=========================================="
    exit 1
fi
