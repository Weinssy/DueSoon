package com.duesoon.app.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import java.util.UUID

@Database(entities = [TaskEntity::class], version = 4, exportSchema = true)
abstract class AppDatabase : RoomDatabase() {
    abstract fun taskDao(): TaskDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE tasks ADD COLUMN isRecurring INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE tasks ADD COLUMN recurrenceInterval TEXT")
            }
        }

        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE tasks ADD COLUMN snoozedUntil INTEGER DEFAULT NULL")
            }
        }

        val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // 1. Create temporary table with Schema 4
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS `tasks_temp` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, 
                        `title` TEXT NOT NULL, 
                        `description` TEXT, 
                        `deadline` INTEGER, 
                        `category` TEXT, 
                        `priority` TEXT NOT NULL, 
                        `reminderType` TEXT NOT NULL, 
                        `isRecurring` INTEGER NOT NULL, 
                        `recurrenceInterval` TEXT, 
                        `completed` INTEGER NOT NULL, 
                        `snoozedUntil` INTEGER, 
                        `createdAt` INTEGER NOT NULL, 
                        `updatedAt` INTEGER NOT NULL,
                        `uuid` TEXT NOT NULL,
                        `isDeleted` INTEGER NOT NULL,
                        `updatedAtUtc` INTEGER NOT NULL,
                        `revision` INTEGER NOT NULL,
                        `syncState` TEXT NOT NULL
                    )
                """.trimIndent())

                // 2. Loop through old data to populate missing fields and avoid UNIQUE constraint issues
                val cursor = db.query("SELECT * FROM tasks")
                val nowUtc = System.currentTimeMillis()
                
                if (cursor.moveToFirst()) {
                    do {
                        val id = cursor.getLong(cursor.getColumnIndexOrThrow("id"))
                        val title = cursor.getString(cursor.getColumnIndexOrThrow("title"))
                        val description = cursor.getString(cursor.getColumnIndexOrThrow("description"))
                        val deadlineIndex = cursor.getColumnIndexOrThrow("deadline")
                        val deadline = if (cursor.isNull(deadlineIndex)) null else cursor.getLong(deadlineIndex)
                        val category = cursor.getString(cursor.getColumnIndexOrThrow("category"))
                        val priority = cursor.getString(cursor.getColumnIndexOrThrow("priority"))
                        val reminderType = cursor.getString(cursor.getColumnIndexOrThrow("reminderType"))
                        val isRecurring = cursor.getInt(cursor.getColumnIndexOrThrow("isRecurring"))
                        val recurrenceInterval = cursor.getString(cursor.getColumnIndexOrThrow("recurrenceInterval"))
                        val completed = cursor.getInt(cursor.getColumnIndexOrThrow("completed"))
                        val snoozedUntilIndex = cursor.getColumnIndexOrThrow("snoozedUntil")
                        val snoozedUntil = if (cursor.isNull(snoozedUntilIndex)) null else cursor.getLong(snoozedUntilIndex)
                        val createdAt = cursor.getLong(cursor.getColumnIndexOrThrow("createdAt"))
                        val updatedAt = cursor.getLong(cursor.getColumnIndexOrThrow("updatedAt"))
                        
                        val uuid = UUID.randomUUID().toString()
                        
                        // Insert into tasks_temp
                        val insertSql = """
                            INSERT INTO `tasks_temp` (
                                `id`, `title`, `description`, `deadline`, `category`, `priority`, 
                                `reminderType`, `isRecurring`, `recurrenceInterval`, `completed`, 
                                `snoozedUntil`, `createdAt`, `updatedAt`, `uuid`, `isDeleted`, 
                                `updatedAtUtc`, `revision`, `syncState`
                            ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                        """.trimIndent()
                        
                        db.execSQL(insertSql, arrayOf(
                            id, title, description, deadline, category, priority, reminderType, 
                            isRecurring, recurrenceInterval, completed, snoozedUntil, createdAt, 
                            updatedAt, uuid, 0, nowUtc, 1L, "DIRTY"
                        ))
                        
                    } while (cursor.moveToNext())
                }
                cursor.close()

                // 3. Drop old table
                db.execSQL("DROP TABLE tasks")

                // 4. Rename temp table
                db.execSQL("ALTER TABLE tasks_temp RENAME TO tasks")

                // 5. Create UUID index
                db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_tasks_uuid` ON `tasks` (`uuid`)")
            }
        }

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "duesoon_database"
                )
                    .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4)
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
