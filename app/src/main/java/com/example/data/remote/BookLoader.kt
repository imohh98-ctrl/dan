package com.example.data.remote

import android.content.Context
import com.example.data.model.Book
import com.example.data.model.BookLanguage
import com.example.data.model.BookPart
import com.example.data.model.Chapter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.io.File
import java.io.IOException
import java.util.concurrent.TimeUnit

data class BookContentResult(
    val bookId: String,
    val navType: String,
    val direction: String,
    val parts: List<BookPart>,
    val chapters: List<Chapter>
)

class BookLoader(private val context: Context) {

    private val client: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .build()
    }

    private val cacheDir: File by lazy {
        File(context.cacheDir, "books_cache").apply { mkdirs() }
    }

    suspend fun loadBookContent(book: Book): Result<BookContentResult> = withContext(Dispatchers.IO) {
        val url = book.jsonUrl
        if (url.isNullOrBlank()) {
            return@withContext Result.failure(IllegalArgumentException("Missing JSON URL for book ${book.id}"))
        }

        val cacheFile = File(cacheDir, "${book.id}.json")

        // 1. Try reading from disk cache if valid
        if (cacheFile.exists() && cacheFile.length() > 50) {
            try {
                val jsonString = cacheFile.readText(Charsets.UTF_8)
                val parsed = parseBookJson(book, jsonString)
                if (parsed.chapters.isNotEmpty()) {
                    return@withContext Result.success(parsed)
                }
            } catch (e: Exception) {
                // If disk cache is corrupted, delete and continue to network fetch
                cacheFile.delete()
            }
        }

        // 2. Fetch from network
        try {
            val jsonString = fetchAndSave(url, cacheFile)
            val parsed = parseBookJson(book, jsonString)
            Result.success(parsed)
        } catch (e: Exception) {
            // Cleanup incomplete or corrupted cache file on error
            if (cacheFile.exists() && cacheFile.length() == 0L) {
                cacheFile.delete()
            }
            Result.failure(e)
        }
    }

    private fun fetchAndSave(url: String, cacheFile: File): String {
        val request = Request.Builder()
            .url(url)
            .header("User-Agent", "DickensLibraryApp/1.0")
            .build()

        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                throw IOException("Server returned HTTP error ${response.code} for URL: $url")
            }
            val body = response.body?.string() ?: throw IOException("Empty server response for URL: $url")
            val tempFile = File(cacheDir, "${cacheFile.name}.tmp")
            tempFile.writeText(body, Charsets.UTF_8)
            if (!tempFile.renameTo(cacheFile)) {
                cacheFile.writeText(body, Charsets.UTF_8)
                tempFile.delete()
            }
            return body
        }
    }

    fun parseBookJson(book: Book, jsonStr: String): BookContentResult {
        val json = JSONObject(jsonStr)
        val id = json.optString("id", book.id)
        val lang = json.optString("language", if (book.language == BookLanguage.ARABIC) "ar" else "en")
        val isAr = lang == "ar"
        val direction = json.optString("direction", if (isAr) "rtl" else "ltr")

        val navObj = json.optJSONObject("navigation")
        val navType = navObj?.optString("type", book.navType) ?: book.navType

        val partsList = mutableListOf<BookPart>()
        val partsArr = json.optJSONArray("parts")
        if (partsArr != null) {
            for (i in 0 until partsArr.length()) {
                val pObj = partsArr.getJSONObject(i)
                partsList.add(
                    BookPart(
                        id = pObj.optString("id", "part-$i"),
                        title = pObj.optString("title", ""),
                        chapterCount = pObj.optInt("chapter_count", 0)
                    )
                )
            }
        }

        val chaptersList = mutableListOf<Chapter>()
        val chapsArr = json.optJSONArray("chapters")
        if (chapsArr != null) {
            for (i in 0 until chapsArr.length()) {
                val cObj = chapsArr.getJSONObject(i)
                val cId = cObj.optString("id", "chapter-${i + 1}")
                val cNum = if (cObj.has("chapter_number")) cObj.optInt("chapter_number") else (i + 1)
                val cTitle = cObj.optString("title", "")
                val cTocTitle = cObj.optString("toc_title", cTitle)
                val cContent = cObj.optString("content", "")
                val partId = if (cObj.has("part_id")) cObj.optString("part_id") else null
                val partTitle = if (cObj.has("part_title")) cObj.optString("part_title") else null

                chaptersList.add(
                    Chapter(
                        id = cId,
                        bookId = id,
                        chapterNumber = cNum,
                        titleAr = if (isAr) (cTocTitle.ifBlank { cTitle }) else cTitle,
                        titleEn = if (!isAr) (cTocTitle.ifBlank { cTitle }) else cTitle,
                        contentAr = if (isAr) cContent else "",
                        contentEn = if (!isAr) cContent else "",
                        isPdfFormatted = false,
                        tocTitle = cTocTitle.ifBlank { cTitle },
                        partId = partId,
                        partTitle = partTitle
                    )
                )
            }
        }

        return BookContentResult(
            bookId = id,
            navType = navType,
            direction = direction,
            parts = partsList,
            chapters = chaptersList
        )
    }
}
