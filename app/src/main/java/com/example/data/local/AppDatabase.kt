package com.example.data.local

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class AppDatabase(context: Context) : SQLiteOpenHelper(
    context.applicationContext,
    "devchrome_browser_database.db",
    null,
    1
) {
    val bookmarkDao by lazy { BookmarkDao(this) }
    val historyDao by lazy { HistoryDao(this) }
    val savedWebsiteDao by lazy { SavedWebsiteDao(this) }

    init {
        CoroutineScope(Dispatchers.IO).launch {
            bookmarkDao.refresh()
            historyDao.refresh()
            savedWebsiteDao.refresh()
        }
    }

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS bookmarks (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                title TEXT NOT NULL,
                url TEXT NOT NULL,
                createdAt INTEGER NOT NULL
            )
            """.trimIndent()
        )
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS history (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                title TEXT NOT NULL,
                url TEXT NOT NULL,
                visitedAt INTEGER NOT NULL
            )
            """.trimIndent()
        )
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS saved_websites (
                id TEXT PRIMARY KEY,
                title TEXT NOT NULL,
                originalUrl TEXT NOT NULL,
                localIndexPath TEXT NOT NULL,
                totalPages INTEGER NOT NULL,
                totalAssets INTEGER NOT NULL,
                totalBytes INTEGER NOT NULL,
                savedAt INTEGER NOT NULL
            )
            """.trimIndent()
        )
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        // Migration if needed
    }

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = AppDatabase(context)
                INSTANCE = instance
                instance
            }
        }
    }
}
