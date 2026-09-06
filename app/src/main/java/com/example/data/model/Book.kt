package com.example.data.model

data class BookPart(
    val id: String,
    val title: String,
    val chapterCount: Int = 0
)

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
    val totalChapters: Int = 0,
    val jsonUrl: String? = null,
    val navType: String = "chapters",
    val direction: String = "ltr",
    val parts: List<BookPart> = emptyList()
)

enum class BookLanguage {
    ARABIC,
    ENGLISH,
    BILINGUAL
}
