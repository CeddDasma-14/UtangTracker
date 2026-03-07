# ── App classes ───────────────────────────────────────────────────────────────
# Keep all app code (entities, DAOs, ViewModels, data classes for JSON backup)
-keep class com.cedd.utangtracker.** { *; }

# ── Room ──────────────────────────────────────────────────────────────────────
-keep class * extends androidx.room.RoomDatabase
-keep @androidx.room.Entity class *
-keepclassmembers @androidx.room.Entity class * { *; }

# ── OkHttp ────────────────────────────────────────────────────────────────────
-dontwarn okhttp3.**
-dontwarn okio.**
-keep class okhttp3.** { *; }
-keep interface okhttp3.** { *; }

# ── Firebase / Firestore ──────────────────────────────────────────────────────
-keep class com.google.firebase.** { *; }
-keep class com.google.android.gms.** { *; }
-dontwarn com.google.firebase.**
-dontwarn com.google.android.gms.**

# ── Kotlin Coroutines ─────────────────────────────────────────────────────────
-keepclassmembernames class kotlinx.** {
    volatile <fields>;
}

# ── Hilt / Dagger (has own consumer rules, but safety net) ───────────────────
-dontwarn dagger.**
-dontwarn javax.inject.**

# ── Coil ─────────────────────────────────────────────────────────────────────
-dontwarn coil.**

# ── Suppress common warnings ─────────────────────────────────────────────────
-dontwarn java.lang.invoke.**
-dontwarn kotlin.reflect.**
