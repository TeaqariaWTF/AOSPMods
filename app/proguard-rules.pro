# Kotlin
-assumenosideeffects class kotlin.jvm.internal.Intrinsics {
    public static void check*(...);
    public static void throw*(...);
}
-assumenosideeffects class java.util.Objects {
    public static ** requireNonNull(...);
}

# Strip debug log
-assumenosideeffects class android.util.Log {
    public static int v(...);
    public static int d(...);
}

# Xposed
-keep class de.robv.android.xposed.**

# PixelXpert - for debug and trace
-keep class sh.siava.pixelxpert.** { public protected private *; }

# AndroidX
-keepnames class androidx.compose.ui.**

#pytorch library
-keep class org.pytorch.** { *; }
-keep class com.facebook.** { *; }

# Keep the ConstraintLayout Motion class
-keep,allowoptimization,allowobfuscation class androidx.constraintlayout.motion.widget.** { *; }

# Keep Recycler View Stuff
-keep,allowoptimization,allowobfuscation class androidx.recyclerview.widget.** { *; }

# Keep Parcelable Creators
-keepnames class * implements android.os.Parcelable {
    public static final ** CREATOR;
}

# Services
-keep interface **.I* { *; }
-keep class **.I*$Stub { *; }
-keep class **.I*$Stub$Proxy { *; }
-keep class sh.siava.pixelxpert.service.* { *; }

# Keep all inner classes and their names within the specified package
# but allow optimization of their internal code
-keep class sh.siava.pixelxpert.**$* {
    public protected private *;
}

# Allow optimization and shrinking for all classes
-optimizations !code/simplification/arithmetic,!field/*,!class/merging/*
-keepattributes Exceptions,InnerClasses,Signature,Deprecated,*Annotation*,EnclosingMethod,SourceFile,LineNumberTable

# Keep all native method names
-keepclasseswithmembernames class * {
    native <methods>;
}

# Keep all annotation types
-keep @interface ** { *; }

# Markdown View
-dontwarn java.awt.image.RGBImageFilter
-keep class br.tiagohm.markdownview.**
-keep class com.vladsch.flexmark.**