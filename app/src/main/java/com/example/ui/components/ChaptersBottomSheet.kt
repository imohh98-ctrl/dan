package com.example.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoStories
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.model.Book
import com.example.data.model.BookLanguage
import com.example.data.model.Chapter
import com.example.ui.viewmodel.AppLanguage

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChaptersBottomSheet(
    book: Book,
    chapters: List<Chapter>,
    currentChapterIndex: Int,
    currentLanguage: AppLanguage,
    onChapterSelected: (Int) -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    // RTL/LTR layout based on the book's language/direction
    val isBookRtl = book.language == BookLanguage.ARABIC ||
            book.direction.equals("rtl", ignoreCase = true) ||
            book.id.endsWith("-ar")
    val bookDirection = if (isBookRtl) LayoutDirection.Rtl else LayoutDirection.Ltr

    val isParts = book.navType == "parts_and_chapters"
    val isStories = book.navType == "stories"

    val listState = rememberLazyListState()

    // Auto-scroll to current chapter when TOC opens
    LaunchedEffect(currentChapterIndex) {
        if (currentChapterIndex in chapters.indices) {
            listState.animateScrollToItem(currentChapterIndex.coerceAtLeast(0))
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
        modifier = Modifier.testTag("chapters_bottom_sheet")
    ) {
        CompositionLocalProvider(LocalLayoutDirection provides bookDirection) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 8.dp)
                    .padding(bottom = 28.dp)
            ) {
                // Header Bar
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val headerTitle = when {
                        isStories -> if (isBookRtl) "قائمة القصص" else "Stories"
                        isParts -> if (isBookRtl) "الأجزاء والفصول" else "Parts & Chapters"
                        else -> if (isBookRtl) "فهرس الفصول" else "Table of Contents"
                    }
                    val countLabel = when {
                        isStories -> "${chapters.size} " + if (isBookRtl) "قصة" else "stories"
                        else -> "${chapters.size} " + if (isBookRtl) "فصل" else "chapters"
                    }

                    Text(
                        text = headerTitle,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = countLabel,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))
                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
                Spacer(modifier = Modifier.height(8.dp))

                if (isParts) {
                    // Group chapters by part
                    val groupedChapters = remember(chapters) {
                        val map = LinkedHashMap<String, MutableList<Pair<Int, Chapter>>>()
                        chapters.forEachIndexed { index, chapter ->
                            val key = chapter.partTitle ?: chapter.partId ?: ""
                            map.getOrPut(key) { mutableListOf() }.add(index to chapter)
                        }
                        map
                    }

                    LazyColumn(
                        state = listState,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        groupedChapters.forEach { (partTitle, partChapters) ->
                            if (partTitle.isNotBlank()) {
                                item(key = "part_$partTitle") {
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.45f),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(top = 10.dp, bottom = 4.dp)
                                            .testTag("part_header_${partTitle.take(15)}")
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

                            items(
                                count = partChapters.size,
                                key = { i -> "chap_${partChapters[i].second.id}" }
                            ) { i ->
                                val (globalIndex, chapter) = partChapters[i]
                                val isCurrent = globalIndex == currentChapterIndex
                                val title = chapter.tocTitle?.ifBlank { null }
                                    ?: if (isBookRtl) chapter.titleAr.ifBlank { chapter.titleEn }
                                    else chapter.titleEn.ifBlank { chapter.titleAr }

                                ChapterItemRow(
                                    title = title,
                                    isCurrent = isCurrent,
                                    isStory = false,
                                    onClick = {
                                        onChapterSelected(globalIndex)
                                        onDismiss()
                                    },
                                    testTag = "chapter_item_$globalIndex"
                                )
                            }
                        }
                    }
                } else {
                    // Flat Chapters or Stories
                    LazyColumn(
                        state = listState,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        itemsIndexed(chapters, key = { _, c -> c.id }) { index, chapter ->
                            val isCurrent = index == currentChapterIndex
                            val title = chapter.tocTitle?.ifBlank { null }
                                ?: if (isBookRtl) chapter.titleAr.ifBlank { chapter.titleEn }
                                else chapter.titleEn.ifBlank { chapter.titleAr }

                            ChapterItemRow(
                                title = title,
                                isCurrent = isCurrent,
                                isStory = isStories,
                                onClick = {
                                    onChapterSelected(index)
                                    onDismiss()
                                },
                                testTag = "chapter_item_$index"
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ChapterItemRow(
    title: String,
    isCurrent: Boolean,
    isStory: Boolean,
    onClick: () -> Unit,
    testTag: String
) {
    Surface(
        color = if (isCurrent) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 3.dp)
            .clickable(onClick = onClick)
            .testTag(testTag)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 11.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = when {
                    isCurrent -> Icons.Default.CheckCircle
                    isStory -> Icons.Default.AutoStories
                    else -> Icons.Default.MenuBook
                },
                contentDescription = null,
                tint = if (isCurrent) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Normal,
                    color = if (isCurrent) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }
}
