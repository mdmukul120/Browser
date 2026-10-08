package com.example.data.local

import android.content.ContentValues
import android.database.Cursor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext

class BookmarkDao(private val dbHelper: AppDatabase) {
    private val _bookmarks = MutableStateFlow<List<BookmarkEntity>>(emptyList())

    suspend fun refresh() = withContext(Dispatchers.IO) {
        val list = mutableListOf<BookmarkEntity>()
        val db = dbHelper.readableDatabase
        val cursor: Cursor = db.rawQuery("SELECT id, title, url, createdAt FROM bookmarks ORDER BY createdAt DESC", null)
        cursor.use {
            while (it.moveToNext()) {
                list.add(
                    BookmarkEntity(
                        id = it.getLong(0),
                        title = it.getString(1),
                        url = it.getString(2),
                        createdAt = it.getLong(3)
                    )
                )
            }
        }
        _bookmarks.value = list
    }

    fun getAllBookmarks(): Flow<List<BookmarkEntity>> = _bookmarks.asStateFlow()

    suspend fun insertBookmark(bookmark: BookmarkEntity): Long = withContext(Dispatchers.IO) {
        val db = dbHelper.writableDatabase
        val values = ContentValues().apply {
            put("title", bookmark.title)
            put("url", bookmark.url)
            put("createdAt", bookmark.createdAt)
        }
        val id = db.insertWithOnConflict("bookmarks", null, values, android.database.sqlite.SQLiteDatabase.CONFLICT_REPLACE)
        refresh()
        id
    }

    suspend fun deleteBookmark(bookmark: BookmarkEntity) = withContext(Dispatchers.IO) {
        val db = dbHelper.writableDatabase
        db.delete("bookmarks", "id = ?", arrayOf(bookmark.id.toString()))
        refresh()
    }

    suspend fun isBookmarked(url: String): Boolean = withContext(Dispatchers.IO) {
        val db = dbHelper.readableDatabase
        val cursor = db.rawQuery("SELECT 1 FROM bookmarks WHERE url = ? LIMIT 1", arrayOf(url))
        cursor.use { it.moveToFirst() }
    }

    suspend fun deleteByUrl(url: String) = withContext(Dispatchers.IO) {
        val db = dbHelper.writableDatabase
        db.delete("bookmarks", "url = ?", arrayOf(url))
        refresh()
    }
}

class HistoryDao(private val dbHelper: AppDatabase) {
    private val _history = MutableStateFlow<List<HistoryEntity>>(emptyList())

    suspend fun refresh() = withContext(Dispatchers.IO) {
        val list = mutableListOf<HistoryEntity>()
        val db = dbHelper.readableDatabase
        val cursor: Cursor = db.rawQuery("SELECT id, title, url, visitedAt FROM history ORDER BY visitedAt DESC LIMIT 100", null)
        cursor.use {
            while (it.moveToNext()) {
                list.add(
                    HistoryEntity(
                        id = it.getLong(0),
                        title = it.getString(1),
                        url = it.getString(2),
                        visitedAt = it.getLong(3)
                    )
                )
            }
        }
        _history.value = list
    }

    fun getRecentHistory(): Flow<List<HistoryEntity>> = _history.asStateFlow()

    suspend fun insertHistory(history: HistoryEntity): Long = withContext(Dispatchers.IO) {
        val db = dbHelper.writableDatabase
        val values = ContentValues().apply {
            put("title", history.title)
            put("url", history.url)
            put("visitedAt", history.visitedAt)
        }
        val id = db.insert("history", null, values)
        refresh()
        id
    }

    suspend fun deleteHistory(id: Long) = withContext(Dispatchers.IO) {
        val db = dbHelper.writableDatabase
        db.delete("history", "id = ?", arrayOf(id.toString()))
        refresh()
    }

    suspend fun clearHistory() = withContext(Dispatchers.IO) {
        val db = dbHelper.writableDatabase
        db.delete("history", null, null)
        refresh()
    }
}

class SavedWebsiteDao(private val dbHelper: AppDatabase) {
    private val _savedSites = MutableStateFlow<List<SavedWebsiteEntity>>(emptyList())

    suspend fun refresh() = withContext(Dispatchers.IO) {
        val list = mutableListOf<SavedWebsiteEntity>()
        val db = dbHelper.readableDatabase
        val cursor: Cursor = db.rawQuery(
            "SELECT id, title, originalUrl, localIndexPath, totalPages, totalAssets, totalBytes, savedAt FROM saved_websites ORDER BY savedAt DESC",
            null
        )
        cursor.use {
            while (it.moveToNext()) {
                list.add(
                    SavedWebsiteEntity(
                        id = it.getString(0),
                        title = it.getString(1),
                        originalUrl = it.getString(2),
                        localIndexPath = it.getString(3),
                        totalPages = it.getInt(4),
                        totalAssets = it.getInt(5),
                        totalBytes = it.getLong(6),
                        savedAt = it.getLong(7)
                    )
                )
            }
        }
        _savedSites.value = list
    }

    fun getAllSavedWebsites(): Flow<List<SavedWebsiteEntity>> = _savedSites.asStateFlow()

    suspend fun insertSavedWebsite(website: SavedWebsiteEntity) = withContext(Dispatchers.IO) {
        val db = dbHelper.writableDatabase
        val values = ContentValues().apply {
            put("id", website.id)
            put("title", website.title)
            put("originalUrl", website.originalUrl)
            put("localIndexPath", website.localIndexPath)
            put("totalPages", website.totalPages)
            put("totalAssets", website.totalAssets)
            put("totalBytes", website.totalBytes)
            put("savedAt", website.savedAt)
        }
        db.insertWithOnConflict("saved_websites", null, values, android.database.sqlite.SQLiteDatabase.CONFLICT_REPLACE)
        refresh()
    }

    suspend fun deleteSavedWebsite(website: SavedWebsiteEntity) = withContext(Dispatchers.IO) {
        val db = dbHelper.writableDatabase
        db.delete("saved_websites", "id = ?", arrayOf(website.id))
        refresh()
    }
}
