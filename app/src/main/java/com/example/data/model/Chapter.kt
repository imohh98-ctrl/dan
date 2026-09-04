package com.example.data.model

data class Chapter(
    val id: String,
    val bookId: String,
    val chapterNumber: Int,
    val titleAr: String,
    val titleEn: String,
    val contentAr: String,
    val contentEn: String,
    val isPdfFormatted: Boolean = false
)
