# The app is open-source, no need to obsfucate
-dontobfuscate

# Jackson (no Java 8)
-keepclassmembers class * {
    @com.fasterxml.jackson.annotation.* *;
}
-keep class com.fasterxml.jackson.** { *; }
-keep class * extends com.fasterxml.jackson.core.type.TypeReference
-keep class * implements com.fasterxml.jackson.core.type.TypeReference
-keepattributes Envelope, Signature, InnerClasses, EnclosingMethod, AnnotationDefault, *Annotation*, LocalVariableTable, LocalVariableTypeTable

# Reflection rules for Kotlin metadata that Jackson relies on
-keepattributes RuntimeVisibleAnnotations, RuntimeInvisibleAnnotations, RuntimeVisibleParameterAnnotations, RuntimeInvisibleParameterAnnotations
-dontwarn java.beans.ConstructorProperties
-dontwarn java.beans.Transient

# Logging stack
-dontwarn javax.mail.**
-keep class org.slf4j.** { *; }
-dontwarn com.oracle.svm.core.annotate.**

# F-Droid
-keep class kotlinx.coroutines.CoroutineExceptionHandler
-keep class kotlinx.coroutines.internal.MainDispatcherFactory

# Kotlin optimization
-assumenosideeffects class kotlin.jvm.internal.Intrinsics {
  public static void checkNotNull(...);
}

# Offline License Key
-keep class com.auth0.jwt.** { *; }
-keep class java.util.Base64 { *; }
-keep class java.util.Base64.* { *; }

# ClippingSourceFixMediaSourceFactory
-keepclassmembers class androidx.media3.exoplayer.source.ClippingMediaSource {
    boolean allowUnseekableMedia;
}

# Own serialization
-keep class ua.com.radiokot.photoprism.** { *; }
-keepclassmembers enum * {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}
