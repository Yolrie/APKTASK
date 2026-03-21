package com.example.apktask.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.apktask.data.db.dao.FriendDao
import com.example.apktask.data.db.dao.ProfileDao
import com.example.apktask.data.db.dao.SessionDao
import com.example.apktask.data.db.dao.StreakDao
import com.example.apktask.data.db.dao.TaskDao
import com.example.apktask.data.db.entity.FriendEntity
import com.example.apktask.data.db.entity.ProfileEntity
import com.example.apktask.data.db.entity.SessionEntity
import com.example.apktask.data.db.entity.StreakEntity
import com.example.apktask.data.db.entity.TaskEntity
import com.example.apktask.data.db.migration.Migrations
import net.sqlcipher.database.SupportFactory

/**
 * Base de données Room chiffrée via SQLCipher.
 *
 * Chiffrement :
 *  - [SupportFactory] injecte SQLCipher comme moteur SQLite sous-jacent.
 *  - La passphrase est récupérée depuis [DatabaseKeyManager] (double chiffrement :
 *    SQLCipher AES-256 + Android Keystore AES-256-GCM).
 *  - La passphrase locale est zerosée après ouverture de la base.
 *
 * Accès DB :
 *  - Tous les DAOs sont déclarés `suspend fun` — Room génère des implémentations
 *    coroutine-aware et garantit que les requêtes s'exécutent sur un thread I/O.
 *  - [allowMainThreadQueries] est intentionnellement absent : tout appel accidentel
 *    depuis le thread principal lèvera une IllegalStateException détectable en dev.
 *
 * [exportSchema] = false :
 *  - Le schéma n'est pas exporté dans les assets → pas d'exposition en production.
 *  - À passer à true si des migrations Room sont ajoutées.
 *
 * Singleton thread-safe via double-checked locking (@Volatile + synchronized).
 */
@Database(
    entities = [
        TaskEntity::class,
        SessionEntity::class,
        ProfileEntity::class,
        StreakEntity::class,
        FriendEntity::class
    ],
    version = 3,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun taskDao(): TaskDao
    abstract fun sessionDao(): SessionDao
    abstract fun profileDao(): ProfileDao
    abstract fun streakDao(): StreakDao
    abstract fun friendDao(): FriendDao

    companion object {
        private const val DB_NAME = "apktask_v3.db"

        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: buildDatabase(context.applicationContext).also { INSTANCE = it }
            }

        private fun buildDatabase(context: Context): AppDatabase {
            // Récupération de la passphrase (32 octets, déchiffrée depuis EncryptedSharedPreferences)
            val passphrase = DatabaseKeyManager.getOrCreatePassphrase(context)
            return try {
                Room.databaseBuilder(context, AppDatabase::class.java, DB_NAME)
                    .openHelperFactory(SupportFactory(passphrase))
                    .addMigrations(Migrations.MIGRATION_1_2, Migrations.MIGRATION_2_3)
                    .build()
            } finally {
                // Zérosage de la copie locale de la passphrase
                // (SQLCipher a déjà copié la passphrase en interne)
                passphrase.fill(0)
            }
        }
    }
}
