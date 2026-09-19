# Ekikrit ProGuard / R8 Optimization Rules

# Preserve Room entities and DAOs
-keep class androidx.room.RoomDatabase
-keep class * extends androidx.room.RoomDatabase { *; }
-keep @androidx.room.Entity class * { *; }
-keep @androidx.room.Dao class * { *; }
-keepclassmembers class * {
    @androidx.room.Query *;
    @androidx.room.Insert *;
    @androidx.room.Update *;
    @androidx.room.Delete *;
}

# Preserve Data Models and Entities
-keep class com.example.data.model.** { *; }
-keep class com.example.data.local.** { *; }

# Preserve Moshi & Kotlin serialization
-keepattributes *Annotation*, Signature, InnerClasses, EnclosingMethod
-dontwarn javax.annotation.**
-keepclassmembers class * {
    @com.squareup.moshi.* <fields>;
}

# Preserve Jetpack Compose & ViewModel components
-keepclassmembers class * extends androidx.lifecycle.ViewModel {
    <init>(...);
}

# Coroutines & Flow
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}
