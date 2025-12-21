# Code Editor for Android

<p align="center">
  <img src="app/src/main/res/drawable/ic_launcher_foreground.xml" width="120" alt="Code Editor Icon">
</p>

<p align="center">
  <strong>A powerful, lightweight code editor for Android devices</strong><br>
  Write, edit, and build code directly on your phone or tablet
</p>

<p align="center">
  <img src="https://img.shields.io/badge/Version-2.1-blue" alt="Version">
  <img src="https://img.shields.io/badge/Min%20SDK-28-green" alt="Min SDK">
  <img src="https://img.shields.io/badge/Target%20SDK-29-orange" alt="Target SDK">
  <img src="https://img.shields.io/badge/Java-17-red" alt="Java">
</p>

<p align="center">
  <a href="#features">Features</a> •
  <a href="#installation">Installation</a> •
  <a href="#build">Build</a> •
  <a href="#github-actions">GitHub Actions</a> •
  <a href="#github-integration">GitHub</a> •
  <a href="#license">License</a>
</p>

---

## Overview

Code Editor for Android is a professional code editor designed for developers who want to code on mobile devices. Built with modern Android development practices and Material Design 3, it offers a complete development environment right in your pocket.

**Key Highlights:**
- Syntax highlighting for 25+ programming languages
- Built-in Android APK build system (like AIDE)
- Full GitHub integration with OAuth login
- Multiple themes (VS Code Dark, Monokai, Dracula, etc.)
- Symbol toolbar and autocomplete for faster coding
- Embedded terminal emulator (Termux-based)

## Features

### Code Editor
| Feature | Description |
|---------|-------------|
| **Syntax Highlighting** | Support for Java, Kotlin, Python, JavaScript, TypeScript, C/C++, Go, Rust, Swift, Dart, PHP, Ruby, HTML, CSS, JSON, XML, YAML, SQL, Markdown, and more |
| **Line Numbers** | Synchronized scrolling with the editor |
| **Undo/Redo** | Full history with up to 100 states |
| **Find & Replace** | With regex, case-sensitive, and whole word options |
| **Go to Line** | Quick navigation to any line number |
| **Auto-indent** | Smart indentation based on language syntax |
| **Bracket Matching** | Highlight matching brackets `()`, `[]`, `{}` |
| **Auto-close Brackets** | Automatically close brackets and quotes |
| **Autocomplete** | Language-aware code suggestions |
| **Code Snippets** | Built-in and custom snippets for faster coding |
| **Pinch to Zoom** | Adjust font size with gestures (8-32px) |

### File Management
- **Multi-tab Editor** - Open multiple files simultaneously
- **File Templates** - 18+ templates for different file types
- **Recent Files** - Quick access to recently opened files
- **File Explorer** - Tree view navigation
- **Auto-save** - Configurable auto-save interval

### GitHub Integration
- **OAuth Web Flow** - Secure login with your GitHub account
- **Repository Browser** - Browse public and private repositories
- **File Editor** - Edit files directly from GitHub
- **Commit Changes** - Commit and push changes
- **Branch Support** - View and switch branches
- **Secure Storage** - Credentials stored in EncryptedSharedPreferences

### In-App Build System (AIDE-like)
Build Android APKs directly on your device without a computer:

```
┌─────────────────────────────────────────────────────────────┐
│                      BUILD PIPELINE                          │
├─────────────────────────────────────────────────────────────┤
│  Resolve    →   Compile    →   Compile   →   Process        │
│  Dependencies   Kotlin         Java          Resources      │
│  (Maven)        (.kt)          (.java)       (AAPT2)        │
│                                     ↓                        │
│  Install    ←   Sign       ←   Package   ←   Generate       │
│  APK            APK            APK           DEX (D8)       │
└─────────────────────────────────────────────────────────────┘
```

**Build Features:**
- **Java Compiler** - ECJ (Eclipse Compiler for Java)
- **Kotlin Compiler** - Auto-download kotlinc
- **NDK Support** - Native C/C++ compilation
- **Resource Processing** - AAPT2 for layouts, drawables, values
- **DEX Generation** - D8 for modern DEX conversion
- **R8 Optimization** - Code shrinking for release builds
- **APK Signing** - Debug and release signing
- **SDK Manager** - Auto-download SDK tools on first build
- **Incremental Build** - Only compile changed files

### NDK / Native Code Support
Build native C/C++ code directly on your Android device:

| Feature | Description |
|---------|-------------|
| **Supported ABIs** | `arm64-v8a`, `armeabi-v7a`, `x86_64`, `x86` |
| **Default ABIs** | `arm64-v8a`, `armeabi-v7a` |
| **CMake** | Detects `CMakeLists.txt` automatically |
| **ndk-build** | Detects `Android.mk` automatically |
| **Direct Compilation** | Falls back to clang/clang++ if no build files found |
| **Auto-download NDK** | Downloads NDK r27b on first native build |
| **Termux Fallback** | Uses Termux clang if available |

### Embedded Terminal
Built-in terminal emulator powered by Termux libraries:

| Feature | Description |
|---------|-------------|
| **Terminal Emulator** | Full VT100/xterm-256color compatible terminal |
| **Shell Access** | Uses system shell or bootstrap bash |
| **Color Schemes** | Dracula, Monokai, Solarized Dark, Default |
| **Text Scaling** | Small, Medium, Large text sizes |
| **Bootstrap Install** | Optional full Linux environment with `pkg` manager |
| **Keyboard Support** | Full keyboard with Ctrl keys and arrow keys |

### Themes
- VS Code Dark (default)
- Monokai
- Dracula
- One Dark
- Solarized Dark
- Solarized Light
- GitHub Light

### Keyboard Shortcuts
| Shortcut | Action |
|----------|--------|
| `Ctrl+S` | Save |
| `Ctrl+O` | Open |
| `Ctrl+N` | New File |
| `Ctrl+F` | Find |
| `Ctrl+H` | Find & Replace |
| `Ctrl+G` | Go to Line |
| `Ctrl+Z` | Undo |
| `Ctrl+Y` | Redo |
| `Ctrl+D` | Duplicate Line |
| `Ctrl+/` | Toggle Comment |

## Installation

### Download APK
Download the latest APK from the [Releases](../../releases) page or build it yourself using GitHub Actions.

### Requirements
- Android 9.0 (API 28) or higher
- ~50MB storage for the app
- ~500MB additional storage for SDK tools (downloaded on first build)

## Build

### Prerequisites
- JDK 17 or higher
- Android SDK with:
  - Build Tools 34.0.0
  - Platform SDK 34
- Gradle 8.5

### Build Debug APK

```bash
# Clone the repository
git clone https://github.com/yourusername/code-editor-android.git
cd code-editor-android

# Build debug APK
./gradlew assembleDebug
```

APK location: `app/build/outputs/apk/debug/app-debug.apk`

### Build Release APK

```bash
# Build release APK
./gradlew assembleRelease
```

APK location: `app/build/outputs/apk/release/app-release.apk`

### Build Scripts
The project includes convenient build scripts:

```bash
# Build debug APK
./build_debug.sh

# Build release APK (with signing)
./build_release.sh

# Zip project (high compression)
./zip_project.sh
```

## GitHub Actions

This project includes automated CI/CD with GitHub Actions. APKs are automatically built on every push.

### Automatic Builds
The workflow runs on:
- Push to `main` or `master` branch
- Pull requests to `main` or `master`
- Manual trigger (workflow_dispatch)

### Download Built APKs
1. Go to **Actions** tab in your GitHub repository
2. Click on the latest workflow run
3. Download artifacts:
   - `app-debug` - Debug APK
   - `app-release` - Release APK (requires secrets)

### Setup Release Signing
To enable signed release builds, add these secrets in **Settings → Secrets → Actions**:

| Secret Name | Description | How to Get |
|-------------|-------------|------------|
| `KEYSTORE_BASE64` | Base64 encoded keystore | Run: `base64 -w 0 release-keystore.jks` |
| `KEYSTORE_PASSWORD` | Keystore password | Your keystore password |
| `KEY_ALIAS` | Key alias name | Usually `key0` |
| `KEY_PASSWORD` | Key password | Your key password |

## GitHub Integration

### Setting Up OAuth

1. Go to [GitHub Developer Settings](https://github.com/settings/developers)
2. Click **"New OAuth App"**
3. Fill in the details:
   - **Application name**: Your app name
   - **Homepage URL**: Your website or repo URL
   - **Authorization callback URL**: `codeeditor://github-callback`
4. Copy the **Client ID** and **Client Secret**
5. Add to `gradle.properties`:
   ```properties
   GITHUB_CLIENT_ID=your_client_id
   GITHUB_CLIENT_SECRET=your_client_secret
   ```
6. Rebuild the app

> **Security Note**: Never commit `gradle.properties` with secrets to public repositories.

### For Users (Without Building)
If the app is pre-configured with OAuth, simply:
1. Open the app
2. Tap the GitHub icon in the navigation drawer
3. Tap "Connect GitHub"
4. Authorize in your browser
5. You'll be redirected back to the app

## Project Structure

```
code-editor-android/
├── app/
│   ├── src/main/
│   │   ├── java/com/codeeditor/android/
│   │   │   ├── MainActivity.java           # Main activity with editor
│   │   │   ├── SettingsActivity.java       # Preferences screen
│   │   │   ├── BuildActivity.java          # Build interface
│   │   │   ├── TerminalActivity.java       # Terminal emulator
│   │   │   ├── adapter/                    # RecyclerView adapters
│   │   │   ├── autocomplete/               # Code completion engine
│   │   │   ├── build/                      # Build system
│   │   │   │   ├── BuildPipeline.java      # Build orchestrator
│   │   │   │   ├── BuildService.java       # Background build service
│   │   │   │   ├── SdkManager.java         # SDK management
│   │   │   │   ├── DependencyResolver.java # Maven dependencies
│   │   │   │   └── tasks/                  # Build tasks
│   │   │   ├── github/                     # GitHub integration
│   │   │   ├── syntax/                     # Syntax highlighting
│   │   │   ├── theme/                      # Theme management
│   │   │   ├── utils/                      # Utility classes
│   │   │   └── view/                       # Custom views
│   │   └── res/                            # Resources
│   ├── build.gradle                        # App build config
│   └── proguard-rules.pro                  # ProGuard rules
├── .github/
│   └── workflows/
│       └── build-apk.yml                   # GitHub Actions workflow
├── gradle/
│   └── wrapper/                            # Gradle wrapper
├── build.gradle                            # Root build config
├── settings.gradle                         # Project settings
├── build_debug.sh                          # Debug build script
├── build_release.sh                        # Release build script
├── zip_project.sh                          # Project zip script
└── README.md                               # This file
```

## Tech Stack

| Component | Technology |
|-----------|------------|
| Language | Java 17 |
| Min SDK | 28 (Android 9.0) |
| Target SDK | 29 (Android 10) |
| Compile SDK | 34 (Android 14) |
| UI | Material Design 3 |
| Architecture | View Binding + Data Binding |
| Networking | OkHttp 4.12 |
| JSON | Gson 2.11 |
| Security | EncryptedSharedPreferences |
| Java Compiler | ECJ 3.33.0 |
| Terminal | Termux Terminal Libraries 0.118.0 |

## Dependencies

```groovy
// AndroidX
implementation 'androidx.appcompat:appcompat:1.6.1'
implementation 'androidx.core:core:1.12.0'
implementation 'androidx.activity:activity:1.8.2'
implementation 'androidx.fragment:fragment:1.6.2'
implementation 'androidx.lifecycle:lifecycle-viewmodel:2.7.0'
implementation 'androidx.lifecycle:lifecycle-livedata:2.7.0'
implementation 'androidx.preference:preference:1.2.1'
implementation 'androidx.browser:browser:1.7.0'
implementation 'androidx.security:security-crypto:1.1.0-alpha06'
implementation 'androidx.concurrent:concurrent-futures:1.1.0'

// UI
implementation 'com.google.android.material:material:1.11.0'
implementation 'androidx.constraintlayout:constraintlayout:2.1.4'
implementation 'androidx.recyclerview:recyclerview:1.3.2'
implementation 'de.hdodenhof:circleimageview:3.1.0'

// Networking
implementation 'com.squareup.okhttp3:okhttp:4.12.0'
implementation 'com.google.code.gson:gson:2.11.0'

// Auth
implementation 'net.openid:appauth:0.11.1'

// Build System
implementation 'org.eclipse.jdt:ecj:3.33.0'

// Terminal
implementation 'com.termux.termux-app:terminal-view:0.118.0'
implementation 'com.termux.termux-app:terminal-emulator:0.118.0'

// Utilities
implementation 'com.google.guava:listenablefuture:1.0'
```

## Contributing

Contributions are welcome! Please feel free to submit a Pull Request.

1. Fork the repository
2. Create your feature branch (`git checkout -b feature/AmazingFeature`)
3. Commit your changes (`git commit -m 'Add some AmazingFeature'`)
4. Push to the branch (`git push origin feature/AmazingFeature`)
5. Open a Pull Request

## License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

## Acknowledgments

- [ECJ](https://www.eclipse.org/jdt/core/) - Eclipse Compiler for Java
- [OkHttp](https://square.github.io/okhttp/) - HTTP client
- [Material Components](https://material.io/components) - UI components
- [Termux](https://github.com/termux/termux-app) - Terminal emulator libraries
- [lzhiyong/android-sdk-tools](https://github.com/lzhiyong/android-sdk-tools) - Android SDK tools for ARM64
- [lzhiyong/termux-ndk](https://github.com/lzhiyong/termux-ndk) - ARM64 NDK for on-device native compilation

---

<p align="center">
  Made with love for mobile developers
</p>
