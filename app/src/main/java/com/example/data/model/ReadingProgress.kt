package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "reading_progress")
data class ReadingProgress(
    @PrimaryKey
    val bookId: String,
    val lastChapterIndex: Int = 0,
    val lastScrollOffset: Int = 0,
    val completionPercent: Float = 0f,
    val lastReadTimestamp: Long = System.currentTimeMillis()
)
