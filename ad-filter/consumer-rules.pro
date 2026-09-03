# ProGuard rules for Kotlin Serialization
# (modern AGP ships kotlinx-serialization's own r8 rules automatically, so the
# old kotlinx.serialization.json.** wildcard keeps are gone; only this module's
# serializable classes need rules here)

-keep,includedescriptorclasses class io.github.edsuns.adfilter.**$$serializer { *; }
-keepclassmembers class io.github.edsuns.adfilter.** {
    *** Companion;
}
-keepclasseswithmembers class io.github.edsuns.adfilter.** {
    kotlinx.serialization.KSerializer serializer(...);
}