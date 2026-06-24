package com.unity.uiapp.data

import android.database.MatrixCursor
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Tests the cursor → [com.unity.uiapp.data.model.Article] mapping in isolation, using a
 * [MatrixCursor] to stand in for the real IPC cursor. This is the most error-prone part of the
 * data source (column-index resolution, per-row reads), so it gets direct coverage rather than
 * only being exercised transitively through the ViewModel's fake data source.
 */
@RunWith(AndroidJUnit4::class)
class ContentResolverMappingTest {

    private val columns = arrayOf(
        "id", "title", "description", "image_url",
        "rating", "color_red", "color_green", "color_blue"
    )

    private fun cursorOf(vararg rows: Array<Any>) = MatrixCursor(columns).apply {
        rows.forEach { addRow(it) }
    }

    @Test
    fun maps_all_columns_for_each_row() = runBlocking {
        val cursor = cursorOf(
            arrayOf(1, "Title A", "Desc A", "https://example.com/a.jpg", 5, 10, 20, 30),
            arrayOf(2, "Title B", "Desc B", "https://example.com/b.jpg", 2, 40, 50, 60)
        )

        val articles = mapCursorToArticles(cursor)

        assertEquals(2, articles.size)
        val first = articles[0]
        assertEquals(1, first.id)
        assertEquals("Title A", first.title)
        assertEquals("Desc A", first.description)
        assertEquals("https://example.com/a.jpg", first.imageUrl)
        assertEquals(5, first.rating)
        assertEquals(10, first.placeholderColor.red)
        assertEquals(20, first.placeholderColor.green)
        assertEquals(30, first.placeholderColor.blue)
        assertEquals(2, articles[1].id)
        assertEquals(60, articles[1].placeholderColor.blue)
    }

    @Test
    fun empty_cursor_yields_empty_list() = runBlocking {
        val articles = mapCursorToArticles(cursorOf())
        assertTrue(articles.isEmpty())
    }

    @Test(expected = IllegalArgumentException::class)
    fun missing_column_throws() = runBlocking {
        // getColumnIndexOrThrow throws IllegalArgumentException when a contract column is absent.
        val cursor = MatrixCursor(arrayOf("id", "title")).apply { addRow(arrayOf(1, "X")) }
        mapCursorToArticles(cursor)
        Unit
    }
}
