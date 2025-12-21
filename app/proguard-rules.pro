# Add project specific ProGuard rules here.
# By default, the flags in this file are appended to flags specified
# in /sdk/tools/proguard/proguard-android.txt

# Keep line numbers for debugging
-keepattributes SourceFile,LineNumberTable

# Keep custom view classes
-keep class com.codeeditor.android.view.** { *; }

# Keep Guava ListenableFuture
-keep class com.google.common.util.concurrent.** { *; }
-dontwarn com.google.common.util.concurrent.**

# Keep AndroidX concurrent futures
-keep class androidx.concurrent.futures.** { *; }
-dontwarn androidx.concurrent.futures.**

# Keep ProfileInstaller
-keep class androidx.profileinstaller.** { *; }
-dontwarn androidx.profileinstaller.**
