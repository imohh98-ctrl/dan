package com.example.ui.screens

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.FormatListBulleted
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.FullscreenExit
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.model.Book
import com.example.data.model.Chapter
import com.example.data.model.ReaderThemeMode
import com.example.data.model.ReadingSettings
import com.example.ui.components.ChaptersBottomSheet
import com.example.ui.components.ReadingControlsSheet
import com.example.ui.theme.ReaderCreamBg
import com.example.ui.theme.ReaderCreamText
import com.example.ui.theme.ReaderDarkBg
import com.example.ui.theme.ReaderDarkText
import com.example.ui.theme.ReaderParchmentBg
import com.example.ui.theme.ReaderParchmentText
import com.example.ui.theme.ReaderSepiaBg
import com.example.ui.theme.ReaderSepiaText
import com.example.ui.theme.ReaderWhiteBg
import com.example.ui.theme.ReaderWhiteText
import com.example.ui.viewmodel.AppLanguage
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReaderScreen(
    book: Book,
    chapter: Chapter,
    allChapters: List<Chapter>,
    currentChapterIndex: Int,
    initialScrollOffset: Int,
    readingSettings: ReadingSettings,
    isBookmarked: Boolean,
    currentLanguage: AppLanguage,
    onBack: () -> Unit,
    onChapterChange: (Int) -> Unit,
    onNextChapter: () -> Unit,
    onPrevChapter: () -> Unit,
    onToggleBookmark: () -> Unit,
    onSaveScrollPosition: (Int, Int) -> Unit,
    onFontSizeChange: (Float) -> Unit,
    onThemeChange: (ReaderThemeMode) -> Unit,
    onToggleAutoScroll: () -> Unit,
    onAutoScrollSpeedChange: (Float) -> Unit,
    onToggleDistractionFree: () -> Unit,
    onZoomScaleChange: (Float) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val isArabic = currentLanguage == AppLanguage.ARABIC
    val chapterTitle = if (isArabic) chapter.titleAr else chapter.titleEn
    val bookTitle = if (isArabic) book.titleAr else book.titleEn
    val textContent = if (isArabic) chapter.contentAr else chapter.contentEn

    // Palette Resolution
    val (readerBg, readerTextColor) = when (readingSettings.themeMode) {
        ReaderThemeMode.PARCHMENT -> ReaderParchmentBg to ReaderParchmentText
        ReaderThemeMode.CREAM -> ReaderCreamBg to ReaderCreamText
        ReaderThemeMode.SEPIA -> ReaderSepiaBg to ReaderSepiaText
        ReaderThemeMode.DARK -> ReaderDarkBg to ReaderDarkText
        ReaderThemeMode.WHITE -> ReaderWhiteBg to ReaderWhiteText
    }

    var showControlsSheet by remember { mutableStateOf(false) }
    var showChaptersSheet by remember { mutableStateOf(false) }

    val scrollState = rememberScrollState()

    // Restore saved scroll position once loaded
    LaunchedEffect(chapter.id) {
        if (initialScrollOffset > 0) {
            scrollState.scrollTo(initialScrollOffset)
        }
    }

    // Auto-save scroll position periodically or on stop
    LaunchedEffect(scrollState.value) {
        delay(400)
        onSaveScrollPosition(currentChapterIndex, scrollState.value)
    }

    // Auto-scroll loop
    LaunchedEffect(readingSettings.isAutoScrolling, readingSettings.autoScrollSpeed) {
        if (readingSettings.isAutoScrolling) {
            val step = (2.5f * readingSettings.autoScrollSpeed).toInt().coerceAtLeast(1)
            while (isActive && readingSettings.isAutoScrolling) {
                if (scrollState.value < scrollState.maxValue) {
                    scrollState.scrollBy(step.toFloat())
                }
                delay(40)
            }
        }
    }

    // Determine layout direction for text reading
    val textDirection = if (isArabic) LayoutDirection.Rtl else LayoutDirection.Ltr

    Scaffold(
        modifier = modifier
            .fillMaxSize()
            .testTag("reader_screen"),
        containerColor = readerBg,
        topBar = {
            AnimatedVisibility(
                visible = !readingSettings.isDistractionFree,
                enter = slideInVertically() + fadeIn(),
                exit = slideOutVertically() + fadeOut()
            ) {
                TopAppBar(
                    title = {
                        Column {
                            Text(
                                text = chapterTitle,
                                style = MaterialTheme.typography.titleSmall,
                                maxLines = 1,
                                color = readerTextColor
                            )
                            Text(
                                text = bookTitle,
                                style = MaterialTheme.typography.labelSmall,
                                color = readerTextColor.copy(alpha = 0.7f),
                                maxLines = 1
                            )
                        }
                    },
                    navigationIcon = {
                        IconButton(
                            onClick = {
                                onSaveScrollPosition(currentChapterIndex, scrollState.value)
                                onBack()
                            },
                            modifier = Modifier.testTag("reader_back_button")
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back",
                                tint = readerTextColor
                            )
                        }
                    },
                    actions = {
                        IconButton(
                            onClick = {
                                onToggleBookmark()
                                val msg = if (isBookmarked) {
                                    context.getString(R.string.bookmark_removed)
                                } else {
                                    context.getString(R.string.bookmark_saved)
                                }
                                Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                            },
                            modifier = Modifier.testTag("reader_bookmark_button")
                        ) {
                            Icon(
                                imageVector = if (isBookmarked) Icons.Default.Bookmark else Icons.Default.BookmarkBorder,
                                contentDescription = "Bookmark",
                                tint = if (isBookmarked) MaterialTheme.colorScheme.secondary else readerTextColor
                            )
                        }
                        IconButton(
                            onClick = { showChaptersSheet = true },
                            modifier = Modifier.testTag("reader_toc_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.FormatListBulleted,
                                contentDescription = "Chapters",
                                tint = readerTextColor
                            )
                        }
                        IconButton(
                            onClick = { showControlsSheet = true },
                            modifier = Modifier.testTag("reader_settings_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Tune,
                                contentDescription = "Reading Settings",
                                tint = readerTextColor
                            )
                        }
                        IconButton(
                            onClick = onToggleDistractionFree,
                            modifier = Modifier.testTag("distraction_free_toggle")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Fullscreen,
                                contentDescription = "Distraction-Free Mode",
                                tint = readerTextColor
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = readerBg,
                        titleContentColor = readerTextColor
                    )
                )
            }
        },
        floatingActionButton = {
            // Floating exit button when in distraction-free mode
            if (readingSettings.isDistractionFree) {
                FloatingActionButton(
                    onClick = onToggleDistractionFree,
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                    shape = CircleShape,
                    modifier = Modifier
                        .size(44.dp)
                        .testTag("exit_fullscreen_fab")
                ) {
                    Icon(
                        imageVector = Icons.Default.FullscreenExit,
                        contentDescription = "Exit Fullscreen",
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(readerBg)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null
                ) {
                    // Tap anywhere in distraction-free mode to reveal tools
                    if (readingSettings.isDistractionFree) {
                        onToggleDistractionFree()
                    }
                }
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(scrollState)
                    .padding(horizontal = 22.dp, vertical = 18.dp)
                    .scale(if (book.isPdf) readingSettings.contentZoomScale else 1.0f)
            ) {
                // PDF notice banner if applicable
                if (book.isPdf) {
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
                        ),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 14.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "نسخة مخطوطة محسنة: يمكنك استخدام تكبير المحتوى حتى ${(readingSettings.contentZoomScale * 100).toInt()}% لقراءة أدق وأوضح.",
                                style = MaterialTheme.typography.labelSmall,
                                color = readerTextColor.copy(alpha = 0.8f)
                            )
                        }
                    }
                }

                // Chapter Header in Victorian Calligraphy Style
                Text(
                    text = chapterTitle,
                    style = MaterialTheme.typography.headlineMedium.copy(
                        fontFamily = FontFamily.Serif,
                        fontWeight = FontWeight.Bold,
                        fontSize = (readingSettings.fontSizeSp + 6f).sp,
                        textAlign = TextAlign.Center
                    ),
                    color = readerTextColor,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = bookTitle,
                    style = MaterialTheme.typography.labelMedium.copy(
                        fontFamily = FontFamily.Serif,
                        textAlign = TextAlign.Center
                    ),
                    color = readerTextColor.copy(alpha = 0.7f),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(14.dp))
                HorizontalDivider(
                    color = readerTextColor.copy(alpha = 0.2f),
                    thickness = 1.dp
                )
                Spacer(modifier = Modifier.height(20.dp))

                // Chapter Reading Body Text with explicit bidirectional support
                CompositionLocalProvider(LocalLayoutDirection provides textDirection) {
                    Text(
                        text = textContent,
                        style = MaterialTheme.typography.bodyLarge.copy(
                            fontFamily = FontFamily.Serif,
                            fontSize = readingSettings.fontSizeSp.sp,
                            lineHeight = (readingSettings.fontSizeSp * readingSettings.lineSpacingMultiplier).sp,
                            textAlign = if (isArabic) TextAlign.Justify else TextAlign.Start
                        ),
                        color = readerTextColor,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("reader_content_text")
                    )
                }

                Spacer(modifier = Modifier.height(36.dp))
                HorizontalDivider(color = readerTextColor.copy(alpha = 0.2f))
                Spacer(modifier = Modifier.height(18.dp))

                // Bottom Navigation Row: Next / Previous Chapter
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 24.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedButton(
                        onClick = onPrevChapter,
                        enabled = currentChapterIndex > 0,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.testTag("prev_chapter_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (isArabic) "الفصل السابق" else "Previous Chapter",
                            style = MaterialTheme.typography.labelSmall
                        )
                    }

                    Text(
                        text = "${currentChapterIndex + 1} / ${allChapters.size}",
                        style = MaterialTheme.typography.labelMedium,
                        color = readerTextColor.copy(alpha = 0.75f)
                    )

                    OutlinedButton(
                        onClick = onNextChapter,
                        enabled = currentChapterIndex < allChapters.size - 1,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.testTag("next_chapter_button")
                    ) {
                        Text(
                            text = if (isArabic) "الفصل التالي" else "Next Chapter",
                            style = MaterialTheme.typography.labelSmall
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }

            // Floating Quick Auto-Scroll indicator if active
            if (readingSettings.isAutoScrolling) {
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.9f),
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 16.dp)
                        .clickable { onToggleAutoScroll() }
                        .testTag("auto_scroll_indicator")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Pause,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (isArabic) "التمرير التلقائي نشط (${readingSettings.autoScrollSpeed}x)" else "Auto-scroll active (${readingSettings.autoScrollSpeed}x)",
                            color = Color.White,
                            style = MaterialTheme.typography.labelSmall
                        )
                    }
                }
            }
        }
    }

    // Modal Sheet for Theme, Font Size & Auto-Scroll
    if (showControlsSheet) {
        ReadingControlsSheet(
            settings = readingSettings,
            isPdfOrScanned = book.isPdf,
            currentLanguage = currentLanguage,
            onFontSizeChange = onFontSizeChange,
            onThemeChange = onThemeChange,
            onAutoScrollToggle = onToggleAutoScroll,
            onAutoScrollSpeedChange = onAutoScrollSpeedChange,
            onDistractionFreeToggle = onToggleDistractionFree,
            onZoomScaleChange = onZoomScaleChange,
            onDismiss = { showControlsSheet = false }
        )
    }

    // Modal Sheet for Chapter selection
    if (showChaptersSheet) {
        ChaptersBottomSheet(
            chapters = allChapters,
            currentChapterIndex = currentChapterIndex,
            currentLanguage = currentLanguage,
            onChapterSelected = { idx ->
                onChapterChange(idx)
                showChaptersSheet = false
            },
            onDismiss = { showChaptersSheet = false }
        )
    }
}
