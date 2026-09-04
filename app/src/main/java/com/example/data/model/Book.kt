package com.example.data.model

data class Book(
    val id: String,
    val titleAr: String,
    val titleEn: String,
    val authorAr: String = "تشارلز ديكنز",
    val authorEn: String = "Charles Dickens",
    val descAr: String,
    val descEn: String,
    val year: Int,
    val genreAr: String,
    val genreEn: String,
    val language: BookLanguage, // ARABIC, ENGLISH, BOTH
    val coverRes: Int? = null,
    val coverUrl: String? = null,
    val isPdf: Boolean = false,
    val pdfUrl: String? = null,
    val samplePdfTitle: String? = null,
    val totalChapters: Int = 0
)

enum class BookLanguage {
    ARABIC,
    ENGLISH,
    BILINGUAL
}
