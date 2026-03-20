# ── Stack traces lisibles ─────────────────────────────────────────────────────
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile

# ── Room ──────────────────────────────────────────────────────────────────────
# Room génère des implémentations de RoomDatabase et des DAOs à la compilation.
# R8 doit conserver les classes annotées et leurs membres (constructeurs, champs).
-keep class * extends androidx.room.RoomDatabase
-keep @androidx.room.Entity class * { *; }
-keep @androidx.room.Dao class * { *; }
-keep @androidx.room.Database class * { *; }
-keepclassmembers class * extends androidx.room.RoomDatabase { *; }

# ── SQLCipher ─────────────────────────────────────────────────────────────────
# SQLCipher utilise du code natif (JNI) : les signatures des méthodes natives
# doivent être préservées exactement telles quelles.
-keep class net.sqlcipher.** { *; }
-keep class net.sqlcipher.database.** { *; }
-dontwarn net.sqlcipher.**

# ── EncryptedSharedPreferences / Security-Crypto ──────────────────────────────
-keep class androidx.security.crypto.** { *; }
-dontwarn androidx.security.crypto.**

# ── WorkManager Workers ───────────────────────────────────────────────────────
# WorkManager instancie les Workers par réflexion via leur constructeur à 2 args.
-keep class * extends androidx.work.Worker
-keep class * extends androidx.work.CoroutineWorker
-keepclassmembers class * extends androidx.work.ListenableWorker {
    public <init>(android.content.Context, androidx.work.WorkerParameters);
}

# ── Kotlin Coroutines / Flow ──────────────────────────────────────────────────
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}
-dontwarn kotlinx.coroutines.**

# ── Modèles de domaine et entités Room ────────────────────────────────────────
# data class avec @Entity : Room et Kotlin reflètent constructeurs et champs.
-keepclassmembers class com.example.apktask.data.db.entity.** { *; }
-keepclassmembers class com.example.apktask.model.** { *; }

# ── AndroidX ──────────────────────────────────────────────────────────────────
-dontwarn androidx.**
