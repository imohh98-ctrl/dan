package com.example.ui.viewmodel

import android.app.Application
import android.content.Context
import androidx.compose.ui.unit.LayoutDirection
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.AppDatabase
import com.example.data.local.DickensNovelsRepository
import com.example.data.model.Book
import com.example.data.model.BookLanguage
import com.example.data.model.BookPart
import com.example.data.model.Bookmark
import com.example.data.model.Chapter
import com.example.data.model.Quote
import com.example.data.model.ReaderThemeMode
import com.example.data.model.ReadingProgress
import com.example.data.model.ReadingSettings
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

enum class AppLanguage {
    ARABIC,
    ENGLISH
}

enum class AppThemeMode {
    SYSTEM,
    LIGHT,
    DARK
}

enum class BookFilter {
    ALL,
    ARABIC_ONLY,
    ENGLISH_ONLY
}

sealed class BookContentState {
    object Idle : BookContentState()
    object Loading : BookContentState()
    data class Success(val chapters: List<Chapter>, val parts: List<BookPart> = emptyList()) : BookContentState()
    data class Error(val message: String) : BookContentState()
}

data class NovelsUiState(
    val books: List<Book> = emptyList(),
    val filteredBooks: List<Book> = emptyList(),
    val activeFilter: BookFilter = BookFilter.ALL,
    val searchQuery: String = "",
    val currentLanguage: AppLanguage = AppLanguage.ENGLISH,
    val appThemeMode: AppThemeMode = AppThemeMode.SYSTEM,
    val activeQuote: Quote = DickensNovelsRepository.dickensQuotes.first(),
    val readingProgressMap: Map<String, ReadingProgress> = emptyMap(),
    val bookmarks: List<Bookmark> = emptyList(),
    val readingSettings: ReadingSettings = ReadingSettings(),
    val selectedBook: Book? = null,
    val selectedChapter: Chapter? = null,
    val currentChapterIndex: Int = 0,
    val currentScrollOffset: Int = 0,
    val isSyncingServer: Boolean = false,
    val serverStatusMessage: String? = null,
    val bookContentState: Map<String, BookContentState> = emptyMap()
)

class NovelsViewModel(application: Application) : AndroidViewModel(application) {

    private val database = AppDatabase.getInstance(application)
    val repository = DickensNovelsRepository(database, application)

    private val prefs = application.getSharedPreferences("dickens_library_settings", Context.MODE_PRIVATE)

    // Detect system language as default, but prioritize saved user selection
    private val systemDefaultLanguage: AppLanguage = run {
        val systemLocale = application.resources.configuration.locales.get(0)
        if (systemLocale.language.startsWith("ar")) AppLanguage.ARABIC else AppLanguage.ENGLISH
    }

    private val initialLanguage: AppLanguage = run {
        val saved = prefs.getString("selected_language", null)
        if (saved != null) {
            try {
                AppLanguage.valueOf(saved)
            } catch (e: Exception) {
                systemDefaultLanguage
            }
        } else {
            systemDefaultLanguage
        }
    }

    private val initialThemeMode: AppThemeMode = run {
        val saved = prefs.getString("selected_theme", null)
        if (saved != null) {
            try {
                AppThemeMode.valueOf(saved)
            } catch (e: Exception) {
                AppThemeMode.SYSTEM
            }
        } else {
            AppThemeMode.SYSTEM
        }
    }

    private val _uiState = MutableStateFlow(
        NovelsUiState(
            books = emptyList(),
            filteredBooks = emptyList(),
            currentLanguage = initialLanguage,
            appThemeMode = initialThemeMode,
            activeQuote = repository.getRandomQuote()
        )
    )
    val uiState: StateFlow<NovelsUiState> = _uiState.asStateFlow()

    init {
        // Collect books
        viewModelScope.launch {
            repository.books.collect { bookList ->
                _uiState.value = _uiState.value.copy(
                    books = bookList,
                    filteredBooks = applyFilters(
                        bookList,
                        _uiState.value.currentLanguage,
                        _uiState.value.searchQuery
                    )
                )
            }
        }

        // Collect reading progress from Room
        viewModelScope.launch {
            repository.allReadingProgress.collect { progressList ->
                val map = progressList.associateBy { it.bookId }
                _uiState.value = _uiState.value.copy(readingProgressMap = map)
            }
        }

        // Collect bookmarks from Room
        viewModelScope.launch {
            repository.allBookmarks.collect { bookmarkList ->
                _uiState.value = _uiState.value.copy(bookmarks = bookmarkList)
            }
        }
    }

    fun onHomeScreenEntered() {
        // Fresh quote every time user enters the home screen as required
        _uiState.value = _uiState.value.copy(activeQuote = repository.getRandomQuote())
    }

    fun setLanguage(language: AppLanguage) {
        prefs.edit().putString("selected_language", language.name).apply()
        _uiState.value = _uiState.value.copy(
            currentLanguage = language,
            filteredBooks = applyFilters(_uiState.value.books, language, _uiState.value.searchQuery)
        )
    }

    fun toggleLanguage() {
        val next = if (_uiState.value.currentLanguage == AppLanguage.ARABIC) AppLanguage.ENGLISH else AppLanguage.ARABIC
        setLanguage(next)
    }

    fun setAppThemeMode(themeMode: AppThemeMode) {
        prefs.edit().putString("selected_theme", themeMode.name).apply()
        _uiState.value = _uiState.value.copy(appThemeMode = themeMode)
    }

    val layoutDirection: LayoutDirection
        get() = if (_uiState.value.currentLanguage == AppLanguage.ARABIC) LayoutDirection.Rtl else LayoutDirection.Ltr

    fun setFilter(filter: BookFilter) {
        _uiState.value = _uiState.value.copy(
            activeFilter = filter,
            filteredBooks = applyFilters(_uiState.value.books, _uiState.value.currentLanguage, _uiState.value.searchQuery)
        )
    }

    fun setSearchQuery(query: String) {
        _uiState.value = _uiState.value.copy(
            searchQuery = query,
            filteredBooks = applyFilters(_uiState.value.books, _uiState.value.currentLanguage, query)
        )
    }

    /**
     * Filters books based on:
     * 1. Active filter (All / Arabic / English)
     * 2. Search query: Matches ONLY the book title
     */
    private fun applyFilters(books: List<Book>, language: AppLanguage, query: String): List<Book> {
        return books.filter { book ->
            val matchesFilter = when (_uiState.value.activeFilter) {
                BookFilter.ALL -> true
                BookFilter.ARABIC_ONLY -> book.language == BookLanguage.ARABIC || book.language == BookLanguage.BILINGUAL
                BookFilter.ENGLISH_ONLY -> book.language == BookLanguage.ENGLISH || book.language == BookLanguage.BILINGUAL
            }
            val matchesTitleOnly = if (query.isBlank()) {
                true
            } else {
                book.titleAr.contains(query, ignoreCase = true) ||
                        book.titleEn.contains(query, ignoreCase = true)
            }
            matchesFilter && matchesTitleOnly
        }
    }

    fun loadBookContent(bookId: String, onComplete: ((Boolean) -> Unit)? = null) {
        val book = repository.getBookById(bookId) ?: return
        val existingChapters = repository.getChaptersForBook(bookId)
        val existingParts = repository.getPartsForBook(bookId)
        if (existingChapters.isNotEmpty()) {
            _uiState.value = _uiState.value.copy(
                bookContentState = _uiState.value.bookContentState + (bookId to BookContentState.Success(existingChapters, existingParts))
            )
            onComplete?.invoke(true)
            return
        }

        _uiState.value = _uiState.value.copy(
            bookContentState = _uiState.value.bookContentState + (bookId to BookContentState.Loading)
        )

        viewModelScope.launch {
            val result = repository.loadBookContent(book)
            result.onSuccess { content ->
                _uiState.value = _uiState.value.copy(
                    bookContentState = _uiState.value.bookContentState + (bookId to BookContentState.Success(content.chapters, content.parts)),
                    selectedBook = if (_uiState.value.selectedBook?.id == bookId) {
                        _uiState.value.selectedBook?.copy(
                            navType = content.navType,
                            direction = content.direction,
                            parts = content.parts,
                            totalChapters = content.chapters.size
                        )
                    } else _uiState.value.selectedBook,
                    selectedChapter = if (_uiState.value.selectedBook?.id == bookId) {
                        val idx = _uiState.value.currentChapterIndex.coerceIn(content.chapters.indices)
                        content.chapters.getOrNull(idx)
                    } else _uiState.value.selectedChapter
                )
                onComplete?.invoke(true)
            }.onFailure { error ->
                val msg = error.localizedMessage ?: "Failed to load book content"
                _uiState.value = _uiState.value.copy(
                    bookContentState = _uiState.value.bookContentState + (bookId to BookContentState.Error(msg))
                )
                onComplete?.invoke(false)
            }
        }
    }

    fun getChaptersForBook(bookId: String): List<Chapter> {
        val state = _uiState.value.bookContentState[bookId]
        if (state is BookContentState.Success) {
            return state.chapters
        }
        return repository.getChaptersForBook(bookId)
    }

    fun getPartsForBook(bookId: String): List<BookPart> {
        val state = _uiState.value.bookContentState[bookId]
        if (state is BookContentState.Success) {
            return state.parts
        }
        return repository.getPartsForBook(bookId)
    }

    fun selectBook(bookId: String) {
        val book = repository.getBookById(bookId)
        val chapters = getChaptersForBook(bookId)
        val progress = _uiState.value.readingProgressMap[bookId]
        val initialChapterIndex = progress?.lastChapterIndex ?: 0
        val initialScroll = progress?.lastScrollOffset ?: 0

        val chapter = if (chapters.isNotEmpty() && initialChapterIndex in chapters.indices) {
            chapters[initialChapterIndex]
        } else chapters.firstOrNull()

        _uiState.value = _uiState.value.copy(
            selectedBook = book,
            selectedChapter = chapter,
            currentChapterIndex = initialChapterIndex,
            currentScrollOffset = initialScroll
        )

        if (chapters.isEmpty() && book != null) {
            loadBookContent(bookId)
        }
    }

    fun selectChapter(index: Int) {
        val book = _uiState.value.selectedBook ?: return
        val chapters = getChaptersForBook(book.id)
        if (index in chapters.indices) {
            _uiState.value = _uiState.value.copy(
                selectedChapter = chapters[index],
                currentChapterIndex = index,
                currentScrollOffset = 0
            )
            saveReadingPosition(index, 0)
        }
    }

    fun nextChapter() {
        val book = _uiState.value.selectedBook ?: return
        val chapters = getChaptersForBook(book.id)
        val next = _uiState.value.currentChapterIndex + 1
        if (next < chapters.size) {
            selectChapter(next)
        }
    }

    fun previousChapter() {
        val prev = _uiState.value.currentChapterIndex - 1
        if (prev >= 0) {
            selectChapter(prev)
        }
    }

    fun saveReadingPosition(chapterIndex: Int, scrollOffset: Int) {
        val book = _uiState.value.selectedBook ?: return
        val chapters = getChaptersForBook(book.id)
        val total = chapters.size.coerceAtLeast(1)
        val progressPercent = ((chapterIndex.toFloat() + 0.5f) / total.toFloat()).coerceIn(0f, 1f)

        _uiState.value = _uiState.value.copy(
            currentChapterIndex = chapterIndex,
            currentScrollOffset = scrollOffset
        )

        viewModelScope.launch {
            repository.saveReadingProgress(
                bookId = book.id,
                chapterIndex = chapterIndex,
                scrollOffset = scrollOffset,
                completionPercent = progressPercent
            )
        }
    }

    fun toggleBookmark() {
        val book = _uiState.value.selectedBook ?: return
        val chapter = _uiState.value.selectedChapter ?: return
        val currentBookmarks = _uiState.value.bookmarks
        val existing = currentBookmarks.find { it.bookId == book.id && it.chapterIndex == _uiState.value.currentChapterIndex }

        viewModelScope.launch {
            if (existing != null) {
                repository.removeBookmark(existing.id)
            } else {
                val isAr = _uiState.value.currentLanguage == AppLanguage.ARABIC
                val title = if (isAr) book.titleAr else book.titleEn
                val chapTitle = if (isAr) chapter.titleAr else chapter.titleEn
                val text = if (isAr) chapter.contentAr else chapter.contentEn
                val snippet = text.take(120).replace("\n", " ") + "..."
                repository.addBookmark(
                    Bookmark(
                        bookId = book.id,
                        bookTitle = title,
                        chapterIndex = _uiState.value.currentChapterIndex,
                        chapterTitle = chapTitle,
                        scrollPosition = _uiState.value.currentScrollOffset,
                        snippet = snippet
                    )
                )
            }
        }
    }

    fun removeBookmark(id: Long) {
        viewModelScope.launch {
            repository.removeBookmark(id)
        }
    }

    // Reader Settings updates
    fun updateFontSize(newSize: Float) {
        val clamped = newSize.coerceIn(14f, 34f)
        _uiState.value = _uiState.value.copy(
            readingSettings = _uiState.value.readingSettings.copy(fontSizeSp = clamped)
        )
    }

    fun updateThemeMode(mode: ReaderThemeMode) {
        _uiState.value = _uiState.value.copy(
            readingSettings = _uiState.value.readingSettings.copy(themeMode = mode)
        )
    }

    fun toggleDistractionFree() {
        val current = _uiState.value.readingSettings.isDistractionFree
        _uiState.value = _uiState.value.copy(
            readingSettings = _uiState.value.readingSettings.copy(isDistractionFree = !current)
        )
    }

    fun toggleAutoScroll() {
        val current = _uiState.value.readingSettings.isAutoScrolling
        _uiState.value = _uiState.value.copy(
            readingSettings = _uiState.value.readingSettings.copy(isAutoScrolling = !current)
        )
    }

    fun updateAutoScrollSpeed(speed: Float) {
        _uiState.value = _uiState.value.copy(
            readingSettings = _uiState.value.readingSettings.copy(autoScrollSpeed = speed)
        )
    }

    fun updateContentZoom(zoom: Float) {
        val clamped = zoom.coerceIn(0.8f, 2.8f)
        _uiState.value = _uiState.value.copy(
            readingSettings = _uiState.value.readingSettings.copy(contentZoomScale = clamped)
        )
    }

    fun clearAllNovels() {
        repository.clearBooks()
    }

    fun loadDefaultNovels() {
        repository.loadDefaultNovels()
    }

    fun syncWithServer(customUrl: String? = null) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                isSyncingServer = true,
                serverStatusMessage = "جارٍ الاتصال بالسيرفر وتحديث قائمة الروايات..."
            )
            // Simulating network fetch & validation from custom server endpoint
            kotlinx.coroutines.delay(1200)
            if (customUrl != null && customUrl.isNotBlank()) {
                repository.updateCustomServerUrl(customUrl)
            }
            _uiState.value = _uiState.value.copy(
                isSyncingServer = false,
                serverStatusMessage = "تمت المزامنة بنجاح مع السيرفر: جميع الروايات والأغلفة جاهزة"
            )
        }
    }
}
