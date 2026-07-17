# Media3 session: controller <-> service messaging uses reflection on some paths
-keep class androidx.media3.session.MediaLibraryService { *; }

# kotlinx.serialization: keep serializers for navigation route classes
-keepclassmembers @kotlinx.serialization.Serializable class com.resonate.player.** {
    *** Companion;
}
-keepclasseswithmembers class com.resonate.player.** {
    kotlinx.serialization.KSerializer serializer(...);
}
