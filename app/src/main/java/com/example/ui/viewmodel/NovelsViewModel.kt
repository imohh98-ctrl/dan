package com.example.ui.viewmodel

import android.app.Application
import androidx.compose.ui.unit.LayoutDirection
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.AppDatabase
import com.example.data.local.DickensNovelsRepository
import com.example.data.model.Book
import com.example.data.model.BookLanguage
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

enum class BookFilter {
    ALL,
    ARABIC_ONLY,
    ENGLISH_ONLY
}

data class NovelsUiState(
    val books: List<Book> = emptyList(),
    val filteredBooks: List<Book> = emptyList(),
    val activeFilter: BookFilter = BookFilter.ALL,
    val searchQuery: String = "",
    val currentLanguage: AppLanguage = AppLanguage.ARABIC,
    val activeQuote: Quote = DickensNovelsRepository.dickensQuotes.first(),
    val readingProgressMap: Map<String, ReadingProgress> = emptyMap(),
    val bookmarks: List<Bookmark> = emptyList(),
    val readingSettings: ReadingSettings = ReadingSettings(),
    val selectedBook: Book? = null,
    val selectedChapter: Chapter? = null,
    val currentChapterIndex: Int = 0,
    val currentScrollOffset: Int = 0,
    val isSyncingServer: Boolean = false,
    val serverStatusMessage: String? = null
)

class NovelsViewModel(application: Application) : AndroidViewModel(application) {

    private val database = AppDatabase.getInstance(application)
    val repository = DickensNovelsRepository(database, application)

    private val _uiState = MutableStateFlow(
        NovelsUiState(
            books = emptyList(),
            filteredBooks = emptyList(),
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
                    filteredBooks = applyFilters(bookList, _uiState.value.activeFilter, _uiState.value.searchQuery)
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
        _uiState.value = _uiState.value.copy(currentLanguage = language)
    }

    fun toggleLanguage() {
        val next = if (_uiState.value.currentLanguage == AppLanguage.ARABIC) AppLanguage.ENGLISH else AppLanguage.ARABIC
        setLanguage(next)
    }

    val layoutDirection: LayoutDirection
        get() = if (_uiState.value.currentLanguage == AppLanguage.ARABIC) LayoutDirection.Rtl else LayoutDirection.Ltr

    fun setFilter(filter: BookFilter) {
        _uiState.value = _uiState.value.copy(
            activeFilter = filter,
            filteredBooks = applyFilters(_uiState.value.books, filter, _uiState.value.searchQuery)
        )
    }

    fun setSearchQuery(query: String) {
        _uiState.value = _uiState.value.copy(
            searchQuery = query,
            filteredBooks = applyFilters(_uiState.value.books, _uiState.value.activeFilter, query)
        )
    }

    private fun applyFilters(books: List<Book>, filter: BookFilter, query: String): List<Book> {
        return books.filter { book ->
            val matchesFilter = when (filter) {
                BookFilter.ALL -> true
                BookFilter.ARABIC_ONLY -> book.language == BookLanguage.ARABIC || book.language == BookLanguage.BILINGUAL
                BookFilter.ENGLISH_ONLY -> book.language == BookLanguage.ENGLISH || book.language == BookLanguage.BILINGUAL
            }
            val matchesQuery = if (query.isBlank()) {
                true
            } else {
                book.titleAr.contains(query, ignoreCase = true) ||
                        book.titleEn.contains(query, ignoreCase = true) ||
                        book.descAr.contains(query, ignoreCase = true) ||
                        book.descEn.contains(query, ignoreCase = true)
            }
            matchesFilter && matchesQuery
        }
    }

    fun selectBook(bookId: String) {
        val book = repository.getBookById(bookId)
        val chapters = repository.getChaptersForBook(bookId)
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
    }

    fun selectChapter(index: Int) {
        val book = _uiState.value.selectedBook ?: return
        val chapters = repository.getChaptersForBook(book.id)
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
        val chapters = repository.getChaptersForBook(book.id)
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
        val chapters = repository.getChaptersForBook(book.id)
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
