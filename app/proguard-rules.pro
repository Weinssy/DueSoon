# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.kts.

# Room
-keep class * extends androidx.room.RoomDatabase
-dontwarn androidx.room.paging.**

# DataStore
-keep class androidx.datastore.** { *; }

# Models and Entities
-keep class com.duesoon.app.data.local.** { *; }
-keep class com.duesoon.app.domain.model.** { *; }

# Glance Widget
-keep class com.duesoon.app.widget.** { *; }

# Compose
-keepclassmembers class * {
    @androidx.compose.runtime.Composable *;
}
