# Keep names used only by reflection for kotlinx.serialization and Room generated code.
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.**

# OkHttp
-dontwarn okhttp3.**
-dontwarn okio.**

# Room
-keep class * extends androidx.room.RoomDatabase
