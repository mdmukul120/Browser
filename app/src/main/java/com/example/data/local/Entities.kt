package com.example.data.local

data class BookmarkEntity(
    val id: Long = 0,
    val title: String,
    val url: String,
    val createdAt: Long = System.currentTimeMillis()
)

data class HistoryEntity(
    val id: Long = 0,
    val title: String,
    val url: String,
    val visitedAt: Long = System.currentTimeMillis()
)

data class SavedWebsiteEntity(
    val id: String, // UUID
    val title: String,
    val originalUrl: String,
    val localIndexPath: String,
    val totalPages: Int,
    val totalAssets: Int,
    val totalBytes: Long,
    val savedAt: Long = System.currentTimeMillis()
)
