#!/bin/bash

echo "======================================"
echo "  Code Editor Android Project"
echo "======================================"
echo ""

echo "Project Structure Validation"
echo "----------------------------"

check_file() {
    if [ -f "$1" ]; then
        echo "[OK] $1"
        return 0
    else
        echo "[MISSING] $1"
        return 1
    fi
}

check_dir() {
    if [ -d "$1" ]; then
        echo "[OK] $1/"
        return 0
    else
        echo "[MISSING] $1/"
        return 1
    fi
}

echo ""
echo "Checking Gradle Files:"
check_file "build.gradle"
check_file "settings.gradle"
check_file "gradle.properties"
check_file "app/build.gradle"
check_file "app/proguard-rules.pro"

echo ""
echo "Checking Android Manifest:"
check_file "app/src/main/AndroidManifest.xml"

echo ""
echo "Checking Java Source Files:"
check_file "app/src/main/java/com/codeeditor/android/MainActivity.java"
check_file "app/src/main/java/com/codeeditor/android/view/CodeEditorView.java"
check_file "app/src/main/java/com/codeeditor/android/syntax/SyntaxHighlighter.java"
check_file "app/src/main/java/com/codeeditor/android/utils/UndoRedoManager.java"
check_file "app/src/main/java/com/codeeditor/android/utils/FileUtils.java"

echo ""
echo "Checking Resource Files:"
check_file "app/src/main/res/layout/activity_main.xml"
check_file "app/src/main/res/menu/menu_main.xml"
check_file "app/src/main/res/values/strings.xml"
check_file "app/src/main/res/values/colors.xml"
check_file "app/src/main/res/values/themes.xml"

echo ""
echo "Checking Drawable Resources:"
check_file "app/src/main/res/drawable/ic_add.xml"
check_file "app/src/main/res/drawable/ic_folder.xml"
check_file "app/src/main/res/drawable/ic_save.xml"
check_file "app/src/main/res/drawable/ic_undo.xml"
check_file "app/src/main/res/drawable/ic_redo.xml"
check_file "app/src/main/res/drawable/ic_launcher_foreground.xml"

echo ""
echo "======================================"
echo "  Project Features"
echo "======================================"
echo ""
echo "1. Text Editor with monospace font"
echo "2. Syntax Highlighting:"
echo "   - Java"
echo "   - Python"
echo "   - JavaScript/TypeScript"
echo "   - HTML/XML"
echo "   - CSS"
echo "   - JSON"
echo "3. Line Numbers"
echo "4. Undo/Redo Support"
echo "5. File Open/Save"
echo "6. Dark Theme (VS Code-like)"
echo "7. Material Design 3 UI"
echo ""
echo "======================================"
echo "  Build Instructions"
echo "======================================"
echo ""
echo "To build this Android app:"
echo ""
echo "Option 1: Using Android Studio"
echo "  1. Download project files"
echo "  2. Open in Android Studio"
echo "  3. Build > Make Project"
echo "  4. Run on emulator or device"
echo ""
echo "Option 2: Using Command Line (with Android SDK)"
echo "  ./gradlew assembleDebug"
echo "  APK: app/build/outputs/apk/debug/app-debug.apk"
echo ""
echo "======================================"
echo "  Validation Complete!"
echo "======================================"
