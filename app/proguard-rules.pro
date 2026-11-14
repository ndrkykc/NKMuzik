-dontwarn io.ktor.**
-dontwarn org.jaudiotagger.**
-dontwarn okhttp3.**
-dontwarn androidx.media3.**
-dontwarn org.slf4j.**
-dontwarn java.beans.**

# Room Database Entities & DAOs
-keep class com.nkapps.nkmuzik.data.db.entity.** { *; }
-keep class com.nkapps.nkmuzik.data.db.dao.** { *; }
-keep class * extends androidx.room.RoomDatabase

# Data Models & DTOs (Moshi / Serialization)
-keep class com.nkapps.nkmuzik.data.model.** { *; }
-keepclassmembers class com.nkapps.nkmuzik.data.model.** { *; }

# Keep annotated classes/fields
-keep @androidx.annotation.Keep class * { *; }
-keepclassmembers class * {
    @androidx.annotation.Keep *;
}

# Strip verbose, debug, and info logs in Release builds
-assumenosideeffects class android.util.Log {
    public static *** v(...);
    public static *** d(...);
    public static *** i(...);
}
