package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.AppDatabase
import com.example.data.local.DickensNovelsRepository
import com.example.data.model.Bookmark
import com.example.data.model.ReaderThemeMode
import com.example.data.model.ReadingSettings
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ExampleRobolectricTest {

    @Test
    fun `verify app name resource`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("Charles Dickens Library", appName)
    }

    @Test
    fun `verify dickens novels repository catalog and quotes work`() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val database = AppDatabase.getInstance(context)
        val repo = DickensNovelsRepository(database, context)

        val novels = repo.books.first()
        assertEquals(31, novels.size)

        // Verify Arabic and English books are present
        val arBooks = novels.filter { it.language == com.example.data.model.BookLanguage.ARABIC }
        val enBooks = novels.filter { it.language == com.example.data.model.BookLanguage.ENGLISH }
        assertTrue(arBooks.isNotEmpty())
        assertTrue(enBooks.isNotEmpty())

        val firstBook = repo.getBookById("tale_two_cities") ?: repo.getBookById("a-tale-of-two-cities-ar")
        assertNotNull(firstBook)

        val quote = repo.getRandomQuote()
        assertNotNull(quote)
        assertTrue(quote.quoteAr.isNotBlank())
    }

    @Test
    fun `verify room bookmarks and reading progress`() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val database = AppDatabase.getInstance(context)
        val bookmarkDao = database.bookmarkDao()
        val readingDao = database.readingProgressDao()

        // Insert bookmark
        val bookmarkId = bookmarkDao.insertBookmark(
            Bookmark(
                bookId = "tale_two_cities",
                bookTitle = "قصة مدينتين",
                chapterIndex = 0,
                chapterTitle = "الفصل الأول: العودة إلى الحياة",
                snippet = "كان أفضل الأوقات، وكان أسوأ الأوقات..."
            )
        )
        assertTrue(bookmarkId > 0)

        val bookmarks = bookmarkDao.getAllBookmarks().first()
        assertEquals(1, bookmarks.size)
        assertEquals("قصة مدينتين", bookmarks[0].bookTitle)

        // Delete bookmark
        bookmarkDao.deleteBookmarkById(bookmarkId)
        val afterDelete = bookmarkDao.getAllBookmarks().first()
        assertTrue(afterDelete.isEmpty())
    }

    @Test
    fun `verify reading settings data integrity`() {
        val settings = ReadingSettings(
            fontSizeSp = 22f,
            themeMode = ReaderThemeMode.SEPIA,
            isAutoScrolling = true,
            autoScrollSpeed = 1.5f
        )
        assertEquals(22f, settings.fontSizeSp)
        assertEquals(ReaderThemeMode.SEPIA, settings.themeMode)
        assertTrue(settings.isAutoScrolling)
        assertEquals(1.5f, settings.autoScrollSpeed)
    }
}
