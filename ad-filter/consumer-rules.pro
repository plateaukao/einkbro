# ProGuard rules for Kotlin Serialization

-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.AnnotationsKt # core serialization annotations

# kotlinx-serialization-json specific. Add this if you have java.lang.NoClassDefFoundError kotlinx.serialization.json.JsonObjectSerializer
-keepclassmembers class kotlinx.serialization.json.** {
    *** Companion;
}
-keepclasseswithmembers class kotlinx.serialization.json.** {
    kotlinx.serialization.KSerializer serializer(...);
}

# Change here com.yourcompany.yourpackage
-keep,includedescriptorclasses class io.github.edsuns.adfilter.**$$serializer { *; } # <-- change package name to your app's
-keepclassmembers class io.github.edsuns.adfilter.** { # <-- change package name to your app's
    *** Companion;
}
-keepclasseswithmembers class io.github.edsuns.adfilter.** { # <-- change package name to your app's
    kotlinx.serialization.KSerializer serializer(...);
}