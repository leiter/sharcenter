# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# If your project uses WebView with JS, uncomment the following
# and specify the fully qualified class name to the JavaScript interface
# class:
#-keepclassmembers class fqcn.of.javascript.interface.for.webview {
#   public *;
#}

# Uncomment this to preserve the line number information for
# debugging stack traces.
#-keepattributes SourceFile,LineNumberTable

# Keep line numbers and hide the original source file name so crash
# stack traces remain deobfuscatable via the generated mapping.txt.
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile

# ---------------------------------------------------------------------------
# kotlinx.serialization
# R8 full mode strips the synthetic Companion / $serializer members that the
# plugin generates, which breaks (de)serialization at runtime. These rules are
# the officially recommended set. Ktor's JSON content-negotiation relies on
# these too.
# ---------------------------------------------------------------------------
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.**

# Keep the generated serializer classes and their fields/methods.
-keepclassmembers class **$$serializer { *; }
-if @kotlinx.serialization.Serializable class **
-keepclassmembers class <1> {
    static <1>$Companion Companion;
}
-if @kotlinx.serialization.Serializable class ** {
    static **$* *;
}
-keepclassmembers class <2>$<3> {
    kotlinx.serialization.KSerializer serializer(...);
}

# Keep this project's @Serializable data models by name. Their property names
# are used directly as JSON keys, so they must survive obfuscation.
-keep,includedescriptorclasses class cut.the.crap.data.rest.**$$serializer { *; }
-keepclassmembers @kotlinx.serialization.Serializable class cut.the.crap.data.rest.** {
    <fields>;
}

# ---------------------------------------------------------------------------
# Ktor client (okhttp engine)
# ---------------------------------------------------------------------------
-dontwarn org.slf4j.**
-keep class io.ktor.** { *; }
-keepclassmembers class io.ktor.** { volatile <fields>; }
# Ktor's debug detector references JVM-only java.lang.management APIs that do
# not exist on Android; they are never reached at runtime on-device.
-dontwarn java.lang.management.ManagementFactory
-dontwarn java.lang.management.RuntimeMXBean

# ---------------------------------------------------------------------------
# Coroutines
# ---------------------------------------------------------------------------
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}
-keepclassmembers class kotlinx.coroutines.** {
    volatile <fields>;
}

# ---------------------------------------------------------------------------
# Room, Hilt/Dagger, and Kotlin metadata ship their own consumer ProGuard
# rules via their AARs, so no additional keeps are required here.
# ---------------------------------------------------------------------------