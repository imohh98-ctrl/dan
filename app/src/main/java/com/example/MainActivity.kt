package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.ads.AdManager
import com.example.ads.UmpConsentManager
import com.example.ui.screens.BookDetailScreen
import com.example.ui.screens.BookmarksScreen
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.ReaderScreen
import com.example.ui.screens.SearchScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.theme.CharlesDickensTheme
import com.example.ui.viewmodel.NovelsViewModel

object NavRoutes {
    const val HOME = "home"
    const val SEARCH = "search"
    const val SETTINGS = "settings"
    const val BOOK_DETAIL = "book_detail/{bookId}"
    const val READER = "reader/{bookId}/{chapterIndex}"
    const val BOOKMARKS = "bookmarks"

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

            CharlesDickensTheme(appThemeMode = uiState.appThemeMode) {
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
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    // Show Bottom Bar only on the 3 main tabs: Home, Search, and Settings
    val isBottomBarVisible = currentRoute in listOf(
        NavRoutes.HOME,
        NavRoutes.SEARCH,
        NavRoutes.SETTINGS
    )

    Scaffold(
        bottomBar = {
            if (isBottomBarVisible) {
                NavigationBar(
                    modifier = Modifier.testTag("main_bottom_nav_bar"),
                    containerColor = MaterialTheme.colorScheme.surface,
                    contentColor = MaterialTheme.colorScheme.onSurface
                ) {
                    // 1. Home Tab
                    NavigationBarItem(
                        selected = currentRoute == NavRoutes.HOME,
                        onClick = {
                            if (currentRoute != NavRoutes.HOME) {
                                navController.navigate(NavRoutes.HOME) {
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            }
                        },
                        icon = {
                            Icon(
                                imageVector = Icons.Default.Home,
                                contentDescription = stringResource(id = R.string.tab_home)
                            )
                        },
                        label = {
                            Text(text = stringResource(id = R.string.tab_home))
                        },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = MaterialTheme.colorScheme.onPrimary,
                            selectedTextColor = MaterialTheme.colorScheme.primary,
                            indicatorColor = MaterialTheme.colorScheme.primary,
                            unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                        ),
                        modifier = Modifier.testTag("nav_tab_home")
                    )

                    // 2. Search Tab
                    NavigationBarItem(
                        selected = currentRoute == NavRoutes.SEARCH,
                        onClick = {
                            if (currentRoute != NavRoutes.SEARCH) {
                                navController.navigate(NavRoutes.SEARCH) {
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            }
                        },
                        icon = {
                            Icon(
                                imageVector = Icons.Default.Search,
                                contentDescription = stringResource(id = R.string.tab_search)
                            )
                        },
                        label = {
                            Text(text = stringResource(id = R.string.tab_search))
                        },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = MaterialTheme.colorScheme.onPrimary,
                            selectedTextColor = MaterialTheme.colorScheme.primary,
                            indicatorColor = MaterialTheme.colorScheme.primary,
                            unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                        ),
                        modifier = Modifier.testTag("nav_tab_search")
                    )

                    // 3. Settings Tab
                    NavigationBarItem(
                        selected = currentRoute == NavRoutes.SETTINGS,
                        onClick = {
                            if (currentRoute != NavRoutes.SETTINGS) {
                                navController.navigate(NavRoutes.SETTINGS) {
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            }
                        },
                        icon = {
                            Icon(
                                imageVector = Icons.Default.Settings,
                                contentDescription = stringResource(id = R.string.tab_settings)
                            )
                        },
                        label = {
                            Text(text = stringResource(id = R.string.tab_settings))
                        },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = MaterialTheme.colorScheme.onPrimary,
                            selectedTextColor = MaterialTheme.colorScheme.primary,
                            indicatorColor = MaterialTheme.colorScheme.primary,
                            unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                        ),
                        modifier = Modifier.testTag("nav_tab_settings")
                    )
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = NavRoutes.HOME,
            modifier = Modifier.padding(innerPadding)
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
                    onRefreshQuote = { viewModel.onHomeScreenEntered() },
                    onOpenBookmarks = { navController.navigate(NavRoutes.BOOKMARKS) }
                )
            }

            composable(NavRoutes.SEARCH) {
                SearchScreen(
                    uiState = uiState,
                    onBookClick = { bookId ->
                        viewModel.selectBook(bookId)
                        navController.navigate(NavRoutes.bookDetail(bookId))
                    },
                    onSearchChange = { viewModel.setSearchQuery(it) }
                )
            }

            composable(NavRoutes.SETTINGS) {
                SettingsScreen(
                    uiState = uiState,
                    umpConsentManager = umpConsentManager,
                    onLanguageChange = { viewModel.setLanguage(it) },
                    onThemeChange = { viewModel.setAppThemeMode(it) },
                    onSyncServer = { url -> viewModel.syncWithServer(url) },
                    onClearNovels = { viewModel.clearAllNovels() },
                    onLoadDefaultNovels = { viewModel.loadDefaultNovels() },
                    onBack = null
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
        }
    }
}

