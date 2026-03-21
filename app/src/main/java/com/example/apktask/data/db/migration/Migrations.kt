package com.example.apktask.data.db.migration

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * Toutes les migrations Room regroupées ici pour faciliter l'audit.
 *
 * Règle de sécurité : les migrations n'utilisent que DDL/DML paramétré.
 * Aucune donnée utilisateur n'est interpolée dans les chaînes SQL.
 */
object Migrations {

    /**
     * v1 → v2 : Ajout de la colonne `priority` dans `tasks`.
     * DEFAULT 0 = Priority.NONE — les tâches existantes restent sans priorité.
     */
    val MIGRATION_1_2: Migration = object : Migration(1, 2) {
        override fun migrate(database: SupportSQLiteDatabase) {
            database.execSQL(
                "ALTER TABLE tasks ADD COLUMN priority INTEGER NOT NULL DEFAULT 0"
            )
        }
    }

    /**
     * v2 → v3 : Ajout de la colonne `biometric_lock_enabled` dans `profile`.
     * DEFAULT 0 = verrou biométrique désactivé pour les profils existants.
     */
    val MIGRATION_2_3: Migration = object : Migration(2, 3) {
        override fun migrate(database: SupportSQLiteDatabase) {
            database.execSQL(
                "ALTER TABLE profile ADD COLUMN biometric_lock_enabled INTEGER NOT NULL DEFAULT 0"
            )
        }
    }

    /**
     * v3 → v4 : tâches récurrentes
     *  - Nouvelle colonne nullable [tasks.recurring_task_id] : lien vers le template source
     *  - Nouvelle table [recurring_tasks] : stockage des templates de routines
     */
    val MIGRATION_3_4: Migration = object : Migration(3, 4) {
        override fun migrate(database: SupportSQLiteDatabase) {
            database.execSQL(
                "ALTER TABLE tasks ADD COLUMN recurring_task_id INTEGER"
            )
            database.execSQL(
                """
                CREATE TABLE IF NOT EXISTS recurring_tasks (
                    id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                    title TEXT NOT NULL,
                    priority INTEGER NOT NULL DEFAULT 0,
                    frequency_code INTEGER NOT NULL,
                    days_bitmask INTEGER NOT NULL DEFAULT 0,
                    created_at INTEGER NOT NULL DEFAULT 0,
                    is_active INTEGER NOT NULL DEFAULT 1
                )
                """.trimIndent()
            )
        }
    }
}
