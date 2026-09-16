package com.duesoon.app.data.local

import android.content.Context
import androidx.room.Room
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.sqlite.db.SupportSQLiteOpenHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

@RunWith(AndroidJUnit4::class)
class AppDatabaseMigrationTest {

    private val dbName = "migration-test.db"
    private lateinit var context: Context

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        context.getDatabasePath(dbName).delete()
    }

    @After
    fun teardown() {
        context.getDatabasePath(dbName).delete()
    }

    @Test
    fun testMigration2To3() {
        // Step 1: Create a raw database matching Version 2 schema
        val factory = FrameworkSQLiteOpenHelperFactory()
        val configuration = SupportSQLiteOpenHelper.Configuration.builder(context)
            .name(dbName)
            .callback(object : SupportSQLiteOpenHelper.Callback(2) {
                override fun onCreate(db: SupportSQLiteDatabase) {
                    db.execSQL(
                        "CREATE TABLE IF NOT EXISTS `tasks` (" +
                        "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                        "`title` TEXT NOT NULL, " +
                        "`description` TEXT, " +
                        "`deadline` INTEGER, " +
                        "`category` TEXT, " +
                        "`priority` TEXT NOT NULL, " +
                        "`reminderType` TEXT NOT NULL, " +
                        "`isRecurring` INTEGER NOT NULL, " +
                        "`recurrenceInterval` TEXT, " +
                        "`completed` INTEGER NOT NULL, " +
                        "`createdAt` INTEGER NOT NULL, " +
                        "`updatedAt` INTEGER NOT NULL)"
                    )
                    // Insert a V2 record
                    db.execSQL(
                        "INSERT INTO tasks (title, priority, reminderType, isRecurring, completed, createdAt, updatedAt) " +
                        "VALUES ('Test V2', 'NORMAL', 'SMART', 0, 0, 100, 100)"
                    )
                }

                override fun onUpgrade(db: SupportSQLiteDatabase, oldVersion: Int, newVersion: Int) {}
            })
            .build()

        val rawDb = factory.create(configuration).writableDatabase
        rawDb.close()

        // Step 2: Open with Room using version 3 and MIGRATION_2_3
        val roomDb = Room.databaseBuilder(context, AppDatabase::class.java, dbName)
            .addMigrations(AppDatabase.MIGRATION_1_2)
            .addMigrations(AppDatabase.MIGRATION_2_3)
            .build()

        // Query the database via Room to trigger migration and verify the new column is readable
        val cursor = roomDb.query("SELECT * FROM tasks", null)
        assertTrue(cursor.moveToFirst())
        
        // Verify snoozedUntil column exists and is null
        val columnIndex = cursor.getColumnIndex("snoozedUntil")
        assertTrue("snoozedUntil column should exist", columnIndex != -1)
        assertTrue("snoozedUntil should be null for existing V2 records", cursor.isNull(columnIndex))
        
        cursor.close()
        roomDb.close()
    }
}
