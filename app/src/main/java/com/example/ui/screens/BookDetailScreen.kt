package com.example.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AutoStories
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.FormatListNumbered
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
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
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.R
import com.example.data.model.Book
import com.example.data.model.BookLanguage
import com.example.data.model.BookPart
import com.example.data.model.Chapter
import com.example.data.model.ReadingProgress
import com.example.ui.viewmodel.AppLanguage
import com.example.ui.viewmodel.BookContentState
import com.example.util.SafeIntentHelper

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BookDetailScreen(
    book: Book,
    chapters: List<Chapter>,
    parts: List<BookPart> = emptyList(),
    contentState: BookContentState? = null,
    onRetryLoadContent: () -> Unit = {},
    progress: ReadingProgress?,
    currentLanguage: AppLanguage,
    onBack: () -> Unit,
    onStartReading: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val isBookRtl = book.language == BookLanguage.ARABIC ||
            book.direction.equals("rtl", ignoreCase = true) ||
            book.id.endsWith("-ar")
    val title = if (isBookRtl) book.titleAr.ifBlank { book.titleEn } else book.titleEn.ifBlank { book.titleAr }
    val author = if (isBookRtl) book.authorAr else book.authorEn
    val genre = if (isBookRtl) book.genreAr else book.genreEn
    val desc = if (isBookRtl) book.descAr.ifBlank { book.descEn } else book.descEn.ifBlank { book.descAr }
    val hasProgress = progress != null && progress.completionPercent > 0
    val startChapterIndex = progress?.lastChapterIndex ?: 0

    val isParts = book.navType == "parts_and_chapters"
    val isStories = book.navType == "stories"

    val groupedChapters = remember(chapters) {
        val map = LinkedHashMap<String, MutableList<Pair<Int, Chapter>>>()
        chapters.forEachIndexed { index, chapter ->
            val key = chapter.partTitle ?: chapter.partId ?: ""
            map.getOrPut(key) { mutableListOf() }.add(index to chapter)
        }
        map
    }

    val isAppArabic = currentLanguage == AppLanguage.ARABIC
    val uiDirection = if (isAppArabic) LayoutDirection.Rtl else LayoutDirection.Ltr
    val bookDirection = if (isBookRtl) LayoutDirection.Rtl else LayoutDirection.Ltr

    CompositionLocalProvider(LocalLayoutDirection provides uiDirection) {
        Scaffold(
            modifier = modifier
                .fillMaxSize()
                .testTag("book_detail_screen"),
            topBar = {
                TopAppBar(
                    title = { Text(title, maxLines = 1) },
                    navigationIcon = {
                        IconButton(onClick = onBack, modifier = Modifier.testTag("detail_back_button")) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = stringResource(id = R.string.btn_back)
                            )
                        }
                    },
                    actions = {
                        IconButton(
                            onClick = {
                                val msg = context.getString(R.string.detail_share_recommend, title)
                                SafeIntentHelper.shareText(context, msg, title)
                            },
                            modifier = Modifier.testTag("share_book_button")
                        ) {
                            Icon(imageVector = Icons.Default.Share, contentDescription = stringResource(id = R.string.share_app))
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.background,
                        titleContentColor = MaterialTheme.colorScheme.onBackground
                    )
                )
            },
            containerColor = MaterialTheme.colorScheme.background
        ) { innerPadding ->
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(horizontal = 16.dp)
            ) {
                // Book Hero Header Card
                item {
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = MaterialTheme.colorScheme.surface,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .width(108.dp)
                                    .height(154.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(MaterialTheme.colorScheme.primaryContainer),
                                contentAlignment = Alignment.Center
                            ) {
                                val coverModel = ImageRequest.Builder(LocalContext.current)
                                    .data(book.coverUrl ?: R.drawable.img_app_icon)
                                    .crossfade(true)
                                    .placeholder(R.drawable.img_app_icon)
                                    .error(R.drawable.img_app_icon)
                                    .build()

                                AsyncImage(
                                    model = coverModel,
                                    contentDescription = title,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.matchParentSize()
                                )

                                if (book.isPdf) {
                                    Surface(
                                        color = Color(0xCCB71C1C),
                                        shape = RoundedCornerShape(4.dp),
                                        modifier = Modifier
                                            .align(Alignment.TopEnd)
                                            .padding(4.dp)
                                    ) {
                                        Text(
                                            text = "PDF",
                                            color = Color.White,
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.width(16.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = title,
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = author,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.primary
                                )

                                Spacer(modifier = Modifier.height(8.dp))

                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.CalendarToday,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.secondary,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "${book.year} — $genre",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }

                                Spacer(modifier = Modifier.height(4.dp))

                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = if (isStories) Icons.Default.AutoStories else Icons.Default.FormatListNumbered,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.secondary,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    val countDisplay = if (chapters.isNotEmpty()) chapters.size else book.totalChapters
                                    val countUnit = if (isStories) {
                                        stringResource(id = R.string.detail_unit_stories)
                                    } else {
                                        stringResource(id = R.string.detail_unit_chapters)
                                    }
                                    Text(
                                        text = "$countDisplay $countUnit",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }

                                Spacer(modifier = Modifier.height(4.dp))

                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.Language,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.secondary,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = if (isBookRtl) {
                                            stringResource(id = R.string.detail_arabic_text)
                                        } else {
                                            stringResource(id = R.string.detail_english_text)
                                        },
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))
                }

                // Start / Resume Reading Button
                item {
                    Button(
                        onClick = { onStartReading(startChapterIndex) },
                        enabled = chapters.isNotEmpty(),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                            .testTag("start_reading_button"),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Default.PlayArrow,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (hasProgress) {
                                stringResource(id = R.string.detail_continue_reading_format, startChapterIndex + 1)
                            } else {
                                stringResource(id = R.string.btn_start_reading)
                            },
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Spacer(modifier = Modifier.height(18.dp))
                }

                // Synopsis Card
                item {
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = MaterialTheme.colorScheme.surface,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp)
                        ) {
                            Text(
                                text = stringResource(id = R.string.detail_about_novel),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            CompositionLocalProvider(LocalLayoutDirection provides bookDirection) {
                                Text(
                                    text = desc,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.85f),
                                    lineHeight = 24.sp
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))
                }

                // Chapters List Section Header
                item {
                    val sectionTitle = when {
                        isStories -> stringResource(id = R.string.detail_section_stories)
                        isParts -> stringResource(id = R.string.detail_section_parts)
                        else -> stringResource(id = R.string.detail_section_chapters)
                    }
                    Text(
                        text = sectionTitle,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                }

                // Loading State if chapters not yet loaded
                if (chapters.isEmpty() && contentState is BookContentState.Loading) {
                    item {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.surface,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 12.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(20.dp),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(24.dp),
                                    strokeWidth = 2.5.dp,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Spacer(modifier = Modifier.width(16.dp))
                                Text(
                                    text = if (isBookRtl) "جارٍ تحميل محتوى وفصول الكتاب..." else "Loading book chapters...",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                } else if (chapters.isEmpty() && contentState is BookContentState.Error) {
                    item {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.5f),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp)
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = if (isBookRtl) "تعذر تحميل فصول الكتاب: ${contentState.message}" else "Failed to load chapters: ${contentState.message}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.error
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                OutlinedButton(
                                    onClick = onRetryLoadContent,
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Refresh,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(if (isBookRtl) "إعادة المحاولة" else "Retry")
                                }
                            }
                        }
                    }
                }

                // Render Chapters (with parts grouping if applicable)
                if (isParts) {
                    groupedChapters.forEach { (partTitle, partChaps) ->
                        if (partTitle.isNotBlank()) {
                            item(key = "detail_part_$partTitle") {
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.45f),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(top = 10.dp, bottom = 4.dp)
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 12.dp, vertical = 8.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Bookmark,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.secondary,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = partTitle,
                                            style = MaterialTheme.typography.titleSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onSecondaryContainer
                                        )
                                    }
                                }
                            }
                        }

                        items(partChaps.size, key = { i -> "part_chap_${partChaps[i].second.id}" }) { i ->
                            val (globalIndex, chapter) = partChaps[i]
                            val isCurrent = hasProgress && globalIndex == startChapterIndex
                            val chapterTitle = chapter.tocTitle?.ifBlank { null }
                                ?: if (isBookRtl) chapter.titleAr.ifBlank { chapter.titleEn }
                                else chapter.titleEn.ifBlank { chapter.titleAr }

                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = if (isCurrent) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp)
                                    .clickable { onStartReading(globalIndex) }
                                    .testTag("detail_chapter_item_$globalIndex")
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(14.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = if (isCurrent) Icons.Default.CheckCircle else Icons.Default.MenuBook,
                                        contentDescription = null,
                                        tint = if (isCurrent) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = chapterTitle,
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Medium,
                                            color = if (isCurrent) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
                                        )
                                    }
                                }
                            }
                        }
                    }
                } else {
                    itemsIndexed(chapters, key = { _, c -> c.id }) { index, chapter ->
                        val isCurrent = hasProgress && index == startChapterIndex
                        val chapterTitle = chapter.tocTitle?.ifBlank { null }
                            ?: if (isBookRtl) chapter.titleAr.ifBlank { chapter.titleEn }
                            else chapter.titleEn.ifBlank { chapter.titleAr }

                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = if (isCurrent) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                                .clickable { onStartReading(index) }
                                .testTag("detail_chapter_item_$index")
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = when {
                                        isCurrent -> Icons.Default.CheckCircle
                                        isStories -> Icons.Default.AutoStories
                                        else -> Icons.Default.MenuBook
                                    },
                                    contentDescription = null,
                                    tint = if (isCurrent) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = chapterTitle,
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Medium,
                                        color = if (isCurrent) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }
                        }
                    }
                }

                item {
                    Spacer(modifier = Modifier.height(30.dp))
                }
            }
        }
    }
}
