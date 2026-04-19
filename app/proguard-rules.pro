# Add project specific ProGuard rules here.
-keepattributes *Annotation*
-keepattributes SourceFile,LineNumberTable

# Keep model classes
-keep class com.jiying.launcher.data.model.** { *; }

# Keep service classes
-keep class com.jiying.launcher.service.** { *; }

# Keep receiver classes
-keep class com.jiying.launcher.receiver.** { *; }

# Glide
-keep public class * implements com.bumptech.glide.module.GlideModule
-keep class * extends com.bumptech.glide.module.AppGlideModule {
 <init>(...);
}
-keep public enum com.bumptech.glide.load.ImageHeaderParser$** {
  **[] $VALUES;
  public *;
}
