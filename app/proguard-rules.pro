# R8 / release optimization rules

# --- Room: keep generated impl + entities/DAOs (referenced by generated code) ---
-keep class * extends androidx.room.RoomDatabase
-keep @androidx.room.Entity class *
-keep class com.rainweaver.invoicemaker.data.** { *; }
-dontwarn androidx.room.paging.**

# --- Keep annotations & signatures ---
-keepattributes *Annotation*, Signature, InnerClasses, EnclosingMethod

# --- Kotlin coroutines ---
-dontwarn kotlinx.coroutines.**

# Keep native methods and enums (Compose/Lifecycle may use valueOf)
-keepclassmembers enum * {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}
