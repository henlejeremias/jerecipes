# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# Keep kotlinx.serialization classes
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.AnnotationsKt

-keepclassmembers class kotlinx.serialization.json.** {
    *** Companion;
}
-keepclasseswithmembers class kotlinx.serialization.json.** {
    kotlinx.serialization.KSerializer serializer(...);
}

-keep,includedescriptorclasses class com.jerecipes.**$$serializer { *; }
-keepclassmembers class com.jerecipes.** {
    *** Companion;
}
-keepclasseswithmembers class com.jerecipes.** {
    kotlinx.serialization.KSerializer serializer(...);
}

# Firebase
-keep class com.google.firebase.** { *; }
-keep class com.jerecipes.data.model.** { *; }
