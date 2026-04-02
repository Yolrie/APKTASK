package com.example.apktask

import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.sqlite.db.SupportSQLiteOpenHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.apktask.data.db.migration.Migrations
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Instrumented migration tests.
 *
 * These tests bypass SQLCipher by using [FrameworkSQLiteOpenHelperFactory] to
 * create a plain (non-encrypted) in-memory SQLite database, manually build the
 * schema at a given version, run the target [Migration], and assert the result.
 *
 * This approach does not require Room schema export files, so it works from the
 * very first build without any extra setup.
 */
@RunWith(AndroidJUnit4::class)
class MigrationTest {

    // ── MIGRATION_3_4 ─────────────────────────────────────────────────────────

    @Test
    fun migration3to4_addsRecurringTaskIdColumnToTasks() {
        val db = buildInMemoryV3()
        Migrations.MIGRATION_3_4.migrate(db)

        assertTrue(
            "recurring_task_id column should exist in tasks after MIGRATION_3_4",
            "recurring_task_id" in tableColumns(db, "tasks")
        )

        db.close()
    }

    @Test
    fun migration3to4_createsRecurringTasksTable() {
        val db = buildInMemoryV3()
        Migrations.MIGRATION_3_4.migrate(db)

        assertTrue(
            "recurring_tasks table should be created by MIGRATION_3_4",
            tableExists(db, "recurring_tasks")
        )

        db.close()
    }

    @Test
    fun migration3to4_recurringTasksTableHasExpectedColumns() {
        val db = buildInMemoryV3()
        Migrations.MIGRATION_3_4.migrate(db)

        val cols = tableColumns(db, "recurring_tasks")
        for (expected in listOf("id", "title", "priority", "frequency_code", "days_bitmask", "created_at", "is_active")) {
            assertTrue("recurring_tasks.$expected column missing", expected in cols)
        }

        db.close()
    }

    @Test
    fun migration3to4_preservesExistingTaskData() {
        val db = buildInMemoryV3()
        db.execSQL(
            "INSERT INTO tasks (id, title, createdAt, status, date, priority) VALUES (1, 'Task A', 1000, 1, '2026-03-01', 2)"
        )

        Migrations.MIGRATION_3_4.migrate(db)

        val cursor = db.query("SELECT id, recurring_task_id FROM tasks WHERE id = 1")
        assertTrue("pre-existing task should still be present", cursor.moveToFirst())
        assertTrue("recurring_task_id should be NULL for migrated rows", cursor.isNull(1))
        cursor.close()
        db.close()
    }

    // ── MIGRATION_4_5 ─────────────────────────────────────────────────────────

    @Test
    fun migration4to5_dropsFriendsTable() {
        val db = buildInMemoryV4()

        assertTrue(
            "friends table should exist before MIGRATION_4_5",
            tableExists(db, "friends")
        )

        Migrations.MIGRATION_4_5.migrate(db)

        assertFalse(
            "friends table should be gone after MIGRATION_4_5",
            tableExists(db, "friends")
        )

        db.close()
    }

    @Test
    fun migration4to5_doesNotDropOtherTables() {
        val db = buildInMemoryV4()
        Migrations.MIGRATION_4_5.migrate(db)

        for (table in listOf("tasks", "sessions", "profile", "streak", "recurring_tasks")) {
            assertTrue("$table table should survive MIGRATION_4_5", tableExists(db, table))
        }

        db.close()
    }

    // ── Schema builders ───────────────────────────────────────────────────────

    /** Builds an in-memory plain-SQLite database with the v3 schema. */
    private fun buildInMemoryV3(): SupportSQLiteDatabase {
        return buildInMemory(version = 3) {
            execSQL(
                """CREATE TABLE IF NOT EXISTS tasks (
                    id INTEGER PRIMARY KEY NOT NULL,
                    title TEXT NOT NULL,
                    createdAt INTEGER NOT NULL,
                    status INTEGER NOT NULL,
                    date TEXT NOT NULL,
                    priority INTEGER NOT NULL DEFAULT 0
                )"""
            )
            execSQL("CREATE INDEX IF NOT EXISTS index_tasks_date ON tasks (date)")
            execSQL(
                """CREATE TABLE IF NOT EXISTS sessions (
                    date TEXT PRIMARY KEY NOT NULL,
                    is_registered INTEGER NOT NULL DEFAULT 0
                )"""
            )
            execSQL(
                """CREATE TABLE IF NOT EXISTS profile (
                    id INTEGER PRIMARY KEY NOT NULL,
                    user_id TEXT NOT NULL,
                    display_name TEXT NOT NULL,
                    avatar_color_index INTEGER NOT NULL,
                    is_public INTEGER NOT NULL,
                    notif_morning_enabled INTEGER NOT NULL,
                    notif_evening_enabled INTEGER NOT NULL,
                    notif_morning_hour INTEGER NOT NULL,
                    notif_evening_hour INTEGER NOT NULL,
                    biometric_lock_enabled INTEGER NOT NULL DEFAULT 0
                )"""
            )
            execSQL(
                """CREATE TABLE IF NOT EXISTS streak (
                    id INTEGER PRIMARY KEY NOT NULL,
                    count INTEGER NOT NULL DEFAULT 0,
                    last_counted_date TEXT NOT NULL DEFAULT '',
                    longest_ever INTEGER NOT NULL DEFAULT 0
                )"""
            )
            execSQL(
                """CREATE TABLE IF NOT EXISTS friends (
                    user_id TEXT PRIMARY KEY NOT NULL,
                    display_name TEXT NOT NULL,
                    avatar_color_index INTEGER NOT NULL,
                    completed_today INTEGER NOT NULL DEFAULT 0,
                    total_today INTEGER NOT NULL DEFAULT 0,
                    streak_count INTEGER NOT NULL DEFAULT 0
                )"""
            )
        }
    }

    /** Builds an in-memory plain-SQLite database with the v4 schema (v3 + migration 3→4 applied). */
    private fun buildInMemoryV4(): SupportSQLiteDatabase {
        return buildInMemory(version = 4) {
            execSQL(
                """CREATE TABLE IF NOT EXISTS tasks (
                    id INTEGER PRIMARY KEY NOT NULL,
                    title TEXT NOT NULL,
                    createdAt INTEGER NOT NULL,
                    status INTEGER NOT NULL,
                    date TEXT NOT NULL,
                    priority INTEGER NOT NULL DEFAULT 0,
                    recurring_task_id INTEGER
                )"""
            )
            execSQL("CREATE INDEX IF NOT EXISTS index_tasks_date ON tasks (date)")
            execSQL(
                """CREATE TABLE IF NOT EXISTS sessions (
                    date TEXT PRIMARY KEY NOT NULL,
                    is_registered INTEGER NOT NULL DEFAULT 0
                )"""
            )
            execSQL(
                """CREATE TABLE IF NOT EXISTS profile (
                    id INTEGER PRIMARY KEY NOT NULL,
                    user_id TEXT NOT NULL,
                    display_name TEXT NOT NULL,
                    avatar_color_index INTEGER NOT NULL,
                    is_public INTEGER NOT NULL,
                    notif_morning_enabled INTEGER NOT NULL,
                    notif_evening_enabled INTEGER NOT NULL,
                    notif_morning_hour INTEGER NOT NULL,
                    notif_evening_hour INTEGER NOT NULL,
                    biometric_lock_enabled INTEGER NOT NULL DEFAULT 0
                )"""
            )
            execSQL(
                """CREATE TABLE IF NOT EXISTS streak (
                    id INTEGER PRIMARY KEY NOT NULL,
                    count INTEGER NOT NULL DEFAULT 0,
                    last_counted_date TEXT NOT NULL DEFAULT '',
                    longest_ever INTEGER NOT NULL DEFAULT 0
                )"""
            )
            execSQL(
                """CREATE TABLE IF NOT EXISTS friends (
                    user_id TEXT PRIMARY KEY NOT NULL,
                    display_name TEXT NOT NULL,
                    avatar_color_index INTEGER NOT NULL,
                    completed_today INTEGER NOT NULL DEFAULT 0,
                    total_today INTEGER NOT NULL DEFAULT 0,
                    streak_count INTEGER NOT NULL DEFAULT 0
                )"""
            )
            execSQL(
                """CREATE TABLE IF NOT EXISTS recurring_tasks (
                    id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                    title TEXT NOT NULL,
                    priority INTEGER NOT NULL DEFAULT 0,
                    frequency_code INTEGER NOT NULL,
                    days_bitmask INTEGER NOT NULL DEFAULT 0,
                    created_at INTEGER NOT NULL DEFAULT 0,
                    is_active INTEGER NOT NULL DEFAULT 1
                )"""
            )
        }
    }

    // ── Utilities ─────────────────────────────────────────────────────────────

    private fun buildInMemory(version: Int, createFn: SupportSQLiteDatabase.() -> Unit): SupportSQLiteDatabase {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        val config = SupportSQLiteOpenHelper.Configuration.builder(context)
            .name(null) // null = in-memory
            .callback(object : SupportSQLiteOpenHelper.Callback(version) {
                override fun onCreate(db: SupportSQLiteDatabase) = db.createFn()
                override fun onUpgrade(db: SupportSQLiteDatabase, oldVersion: Int, newVersion: Int) {}
            })
            .build()
        return FrameworkSQLiteOpenHelperFactory().create(config).writableDatabase
    }

    /** Returns the set of column names for [tableName] (via PRAGMA table_info). */
    private fun tableColumns(db: SupportSQLiteDatabase, tableName: String): Set<String> {
        val columns = mutableSetOf<String>()
        val cursor = db.query("PRAGMA table_info($tableName)")
        val nameIdx = cursor.getColumnIndex("name")
        while (cursor.moveToNext()) {
            columns.add(cursor.getString(nameIdx))
        }
        cursor.close()
        return columns
    }

    /** Returns true if [tableName] exists in sqlite_master. */
    private fun tableExists(db: SupportSQLiteDatabase, tableName: String): Boolean {
        val cursor = db.query(
            "SELECT name FROM sqlite_master WHERE type='table' AND name=?",
            arrayOf(tableName)
        )
        val exists = cursor.count > 0
        cursor.close()
        return exists
    }
}
