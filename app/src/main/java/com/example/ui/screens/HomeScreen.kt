package com.example.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.ui.components.BookCard
import com.example.ui.components.NativeAdCard
import com.example.ui.components.QuoteCard
import com.example.ui.viewmodel.AppLanguage
import com.example.ui.viewmodel.NovelsUiState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    uiState: NovelsUiState,
    onBookClick: (String) -> Unit,
    onRefreshQuote: () -> Unit,
    onOpenBookmarks: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isArabic = uiState.currentLanguage == AppLanguage.ARABIC

    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .testTag("home_screen"),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // 1. Top Header Bar (Spans full width)
        item(key = "home_header", span = { GridItemSpan(maxLineSpan) }) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 2.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = stringResource(id = R.string.app_name),
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = stringResource(id = R.string.app_tagline),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                IconButton(
                    onClick = onOpenBookmarks,
                    modifier = Modifier.testTag("bookmarks_nav_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Bookmark,
                        contentDescription = stringResource(id = R.string.screen_bookmarks),
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }

        // 2. Hero Victorian Banner (Spans full width)
        item(key = "home_hero_banner", span = { GridItemSpan(maxLineSpan) }) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(130.dp)
                    .clip(RoundedCornerShape(18.dp))
                    .testTag("hero_banner")
            ) {
                Image(
                    painter = painterResource(id = R.drawable.img_dickens_hero),
                    contentDescription = stringResource(id = R.string.hero_banner_title),
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.matchParentSize()
                )
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .background(
                            Brush.horizontalGradient(
                                colors = listOf(
                                    Color(0xCC1A1512),
                                    Color(0x881A1512),
                                    Color(0x44000000)
                                )
                            )
                        )
                )
                Column(
                    modifier = Modifier
                        .align(Alignment.CenterStart)
                        .padding(horizontal = 18.dp, vertical = 12.dp)
                ) {
                    Text(
                        text = stringResource(id = R.string.hero_banner_title),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFF7EBD2)
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = stringResource(id = R.string.hero_banner_subtitle),
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFFE5D5BA),
                        maxLines = 2
                    )
                }
            }
        }

        // 3. Quote of the Day Section (Spans full width)
        item(key = "home_quote_card", span = { GridItemSpan(maxLineSpan) }) {
            QuoteCard(
                quote = uiState.activeQuote,
                currentLanguage = uiState.currentLanguage,
                onRefreshQuote = onRefreshQuote
            )
        }

        // 4. Books Section Header (Spans full width)
        item(key = "home_books_header", span = { GridItemSpan(maxLineSpan) }) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp, bottom = 2.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(id = R.string.home_section_classics),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Text(
                    text = stringResource(id = R.string.home_books_count, uiState.filteredBooks.size),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }

        // 5. Empty State or Books Grid with Native Ads
        if (uiState.filteredBooks.isEmpty()) {
            item(key = "empty_novels_card", span = { GridItemSpan(maxLineSpan) }) {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surface,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 12.dp)
                        .testTag("empty_novels_card")
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(28.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.MenuBook,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                            modifier = Modifier.size(54.dp)
                        )
                        Spacer(modifier = Modifier.height(14.dp))
                        Text(
                            text = stringResource(id = R.string.empty_library_title),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = stringResource(id = R.string.empty_library_desc),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        } else {
            // Group books into chunks of 6; insert a full-width Native Ad after each chunk of 6
            val chunks = uiState.filteredBooks.chunked(6)
            chunks.forEachIndexed { chunkIndex, chunk ->
                items(chunk, key = { it.id }) { book ->
                    val progress = uiState.readingProgressMap[book.id]
                    BookCard(
                        book = book,
                        progress = progress,
                        currentLanguage = uiState.currentLanguage,
                        onClick = { onBookClick(book.id) },
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                // Insert ONE Native Ad after every 6 books (spanning full 2 columns)
                if (chunk.size == 6) {
                    val adIndex = chunkIndex + 1
                    item(
                        key = "home_native_ad_$adIndex",
                        span = { GridItemSpan(maxLineSpan) }
                    ) {
                        NativeAdCard(
                            currentLanguage = uiState.currentLanguage,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                                .testTag("home_native_ad_$adIndex")
                        )
                    }
                }
            }
        }
    }
}
