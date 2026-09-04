package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.ads.AdManager
import com.example.ads.UmpConsentManager
import com.example.ui.screens.BookDetailScreen
import com.example.ui.screens.BookmarksScreen
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.ReaderScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.theme.CharlesDickensTheme
import com.example.ui.viewmodel.NovelsViewModel

object NavRoutes {
    const val HOME = "home"
    const val BOOK_DETAIL = "book_detail/{bookId}"
    const val READER = "reader/{bookId}/{chapterIndex}"
    const val BOOKMARKS = "bookmarks"
    const val SETTINGS = "settings"

    fun bookDetail(bookId: String) = "book_detail/$bookId"
    fun reader(bookId: String, chapterIndex: Int) = "reader/$bookId/$chapterIndex"
}

class MainActivity : ComponentActivity() {

    private val viewModel: NovelsViewModel by viewModels()
    private val umpConsentManager by lazy { UmpConsentManager(this) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // 1. Gather GDPR / UMP Consent for EEA / UK / Switzerland
        umpConsentManager.gatherConsent(this) {
            // 2. Initialize AdMob safely once consent is verified
            AdManager.initializeMobileAds(this) {
                AdManager.loadAppOpenAd(this)
            }
        }

        setContent {
            val uiState by viewModel.uiState.collectAsState()

            CharlesDickensTheme {
                CompositionLocalProvider(LocalLayoutDirection provides viewModel.layoutDirection) {
                    Surface(modifier = Modifier.fillMaxSize()) {
                        AppNavigation(
                            viewModel = viewModel,
                            umpConsentManager = umpConsentManager
                        )
                    }
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        // Safely show App Open ad if available without recreating UI or resetting state
        AdManager.showAppOpenAdIfAvailable(this)
    }
}

@Composable
fun AppNavigation(
    viewModel: NovelsViewModel,
    umpConsentManager: UmpConsentManager
) {
    val navController = rememberNavController()
    val uiState by viewModel.uiState.collectAsState()

    NavHost(
        navController = navController,
        startDestination = NavRoutes.HOME
    ) {
        composable(NavRoutes.HOME) {
            LaunchedEffect(Unit) {
                viewModel.onHomeScreenEntered()
            }

            HomeScreen(
                uiState = uiState,
                onBookClick = { bookId ->
                    viewModel.selectBook(bookId)
                    navController.navigate(NavRoutes.bookDetail(bookId))
                },
                onLanguageChange = { viewModel.setLanguage(it) },
                onFilterChange = { viewModel.setFilter(it) },
                onSearchChange = { viewModel.setSearchQuery(it) },
                onRefreshQuote = { viewModel.onHomeScreenEntered() },
                onOpenBookmarks = { navController.navigate(NavRoutes.BOOKMARKS) },
                onOpenSettings = { navController.navigate(NavRoutes.SETTINGS) }
            )
        }

        composable(
            route = NavRoutes.BOOK_DETAIL,
            arguments = listOf(navArgument("bookId") { type = NavType.StringType })
        ) { backStackEntry ->
            val bookId = backStackEntry.arguments?.getString("bookId") ?: ""
            val book = viewModel.repository.getBookById(bookId)
            val chapters = viewModel.repository.getChaptersForBook(bookId)
            val progress = uiState.readingProgressMap[bookId]

            if (book != null) {
                BookDetailScreen(
                    book = book,
                    chapters = chapters,
                    progress = progress,
                    currentLanguage = uiState.currentLanguage,
                    onBack = { navController.popBackStack() },
                    onStartReading = { chapterIdx ->
                        viewModel.selectBook(bookId)
                        viewModel.selectChapter(chapterIdx)
                        navController.navigate(NavRoutes.reader(bookId, chapterIdx))
                    }
                )
            }
        }

        composable(
            route = NavRoutes.READER,
            arguments = listOf(
                navArgument("bookId") { type = NavType.StringType },
                navArgument("chapterIndex") { type = NavType.IntType }
            )
        ) { backStackEntry ->
            val bookId = backStackEntry.arguments?.getString("bookId") ?: ""
            val chapterIdx = backStackEntry.arguments?.getInt("chapterIndex") ?: 0

            val book = viewModel.repository.getBookById(bookId)
            val chapters = viewModel.repository.getChaptersForBook(bookId)
            val currentChapter = if (chapterIdx in chapters.indices) chapters[chapterIdx] else chapters.firstOrNull()
            val progress = uiState.readingProgressMap[bookId]
            val initialScroll = progress?.lastScrollOffset ?: 0

            val isBookmarked = uiState.bookmarks.any { it.bookId == bookId && it.chapterIndex == chapterIdx }

            if (book != null && currentChapter != null) {
                ReaderScreen(
                    book = book,
                    chapter = currentChapter,
                    allChapters = chapters,
                    currentChapterIndex = chapterIdx,
                    initialScrollOffset = initialScroll,
                    readingSettings = uiState.readingSettings,
                    isBookmarked = isBookmarked,
                    currentLanguage = uiState.currentLanguage,
                    onBack = { navController.popBackStack() },
                    onChapterChange = { newIdx ->
                        viewModel.selectChapter(newIdx)
                        navController.navigate(NavRoutes.reader(bookId, newIdx)) {
                            popUpTo(NavRoutes.reader(bookId, chapterIdx)) { inclusive = true }
                        }
                    },
                    onNextChapter = {
                        val next = chapterIdx + 1
                        if (next < chapters.size) {
                            viewModel.selectChapter(next)
                            navController.navigate(NavRoutes.reader(bookId, next)) {
                                popUpTo(NavRoutes.reader(bookId, chapterIdx)) { inclusive = true }
                            }
                        }
                    },
                    onPrevChapter = {
                        val prev = chapterIdx - 1
                        if (prev >= 0) {
                            viewModel.selectChapter(prev)
                            navController.navigate(NavRoutes.reader(bookId, prev)) {
                                popUpTo(NavRoutes.reader(bookId, chapterIdx)) { inclusive = true }
                            }
                        }
                    },
                    onToggleBookmark = { viewModel.toggleBookmark() },
                    onSaveScrollPosition = { cIdx, sOffset ->
                        viewModel.saveReadingPosition(cIdx, sOffset)
                    },
                    onFontSizeChange = { viewModel.updateFontSize(it) },
                    onThemeChange = { viewModel.updateThemeMode(it) },
                    onToggleAutoScroll = { viewModel.toggleAutoScroll() },
                    onAutoScrollSpeedChange = { viewModel.updateAutoScrollSpeed(it) },
                    onToggleDistractionFree = { viewModel.toggleDistractionFree() },
                    onZoomScaleChange = { viewModel.updateContentZoom(it) }
                )
            }
        }

        composable(NavRoutes.BOOKMARKS) {
            BookmarksScreen(
                bookmarks = uiState.bookmarks,
                currentLanguage = uiState.currentLanguage,
                onBookmarkClick = { bookId, chapterIndex ->
                    viewModel.selectBook(bookId)
                    viewModel.selectChapter(chapterIndex)
                    navController.navigate(NavRoutes.reader(bookId, chapterIndex))
                },
                onDeleteBookmark = { id -> viewModel.removeBookmark(id) },
                onBack = { navController.popBackStack() }
            )
        }

        composable(NavRoutes.SETTINGS) {
            SettingsScreen(
                uiState = uiState,
                umpConsentManager = umpConsentManager,
                onLanguageChange = { viewModel.setLanguage(it) },
                onSyncServer = { url -> viewModel.syncWithServer(url) },
                onClearNovels = { viewModel.clearAllNovels() },
                onLoadDefaultNovels = { viewModel.loadDefaultNovels() },
                onBack = { navController.popBackStack() }
            )
        }
    }
}
