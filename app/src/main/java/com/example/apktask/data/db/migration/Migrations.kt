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
}
