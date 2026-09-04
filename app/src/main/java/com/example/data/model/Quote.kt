package com.example.data.model

data class Quote(
    val id: String,
    val quoteAr: String,
    val quoteEn: String,
    val sourceNovelAr: String,
    val sourceNovelEn: String,
    val character: String? = null
)
