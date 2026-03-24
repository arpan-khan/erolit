# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.

# Kotlin
-keepclassmembers class **$WhenMappings {
    <fields>;
}

# Jsoup (HTML parser — must keep for scraping)
-keep class org.jsoup.** { *; }
-keepclassmembers class org.jsoup.** { *; }

# OkHttp
-dontwarn okhttp3.**
-dontwarn okio.**
-keep class okhttp3.** { *; }
-keep interface okhttp3.** { *; }

# Room
-keep class * extends androidx.room.RoomDatabase
-keep @androidx.room.Entity class *
-keep @androidx.room.Dao class *

# Hilt generated code
-keep class dagger.hilt.** { *; }
-keep class * extends dagger.hilt.android.lifecycle.HiltViewModel
-dontwarn dagger.hilt.**

# Coil
-dontwarn coil.**

# Coroutines
-keepclassmembers class kotlinx.coroutines.** { *; }

# Domain models (used in Room and serialization)
-keep class com.erolit.app.domain.model.** { *; }
-keep class com.erolit.app.data.local.entity.** { *; }
# JSpecify (annotations used by Jsoup)
-dontwarn org.jspecify.annotations.**
