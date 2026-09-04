package com.example.data.model

enum class ReaderThemeMode {
    PARCHMENT,
    CREAM,
    SEPIA,
    DARK,
    WHITE
}

data class ReadingSettings(
    val fontSizeSp: Float = 19f,
    val themeMode: ReaderThemeMode = ReaderThemeMode.PARCHMENT,
    val isDistractionFree: Boolean = false,
    val isAutoScrolling: Boolean = false,
    val autoScrollSpeed: Float = 1.0f, // 0.5x, 1x, 1.5x, 2x
    val lineSpacingMultiplier: Float = 1.4f,
    val contentZoomScale: Float = 1.0f // for PDF / Arabic scan magnification
)
