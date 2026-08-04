# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.

# Keep JSch classes
-keep class com.jcraft.jsch.** { *; }
-dontwarn com.jcraft.jsch.**

# Keep line numbers for actionable release stack traces.
-keepattributes SourceFile,LineNumberTable

# Tink references Error Prone annotations that are compile-time metadata only.
-dontwarn com.google.errorprone.annotations.**

# Coroutines
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}
