package com.unity.backendapp.data.db

import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.sqlite.db.SimpleSQLiteQuery
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Instrumented test for [ArticleDao] against an in-memory Room database.
 *
 * Exercises the storage layer directly (not through the ContentProvider) so the @RawQuery filter
 * path, insertAll, and count are validated in isolation from IPC.
 */
@RunWith(AndroidJUnit4::class)
class ArticleDaoTest {

    private lateinit var database: ArticleDatabase
    private lateinit var dao: ArticleDao

    private val sample = listOf(
        entity(title = "Alpha News", rating = 5, red = 10),
        entity(title = "alpha sport", rating = 3, red = 20),
        entity(title = "Beta Report", rating = 1, red = 30),
        entity(title = "Gamma alpha daily", rating = 4, red = 40)
    )

    @Before
    fun setUp() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        database = Room.inMemoryDatabaseBuilder(context, ArticleDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        dao = database.articleDao()
        dao.insertAll(sample)
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun count_returns_all_inserted_rows() {
        assertEquals(sample.size, dao.count())
    }

    @Test
    fun rawQuery_select_all_returns_all_rows() {
        val cursor = dao.queryArticles(SimpleSQLiteQuery("SELECT * FROM articles ORDER BY title ASC"))
        cursor.use {
            assertEquals(sample.size, it.count)
        }
    }

    @Test
    fun rawQuery_title_like_is_case_insensitive() {
        // "alpha" should match "Alpha News", "alpha sport", "Gamma alpha daily" → 3 rows.
        val cursor = dao.queryArticles(
            SimpleSQLiteQuery(
                "SELECT * FROM articles WHERE title LIKE ? COLLATE NOCASE ORDER BY title ASC",
                arrayOf<Any>("%alpha%")
            )
        )
        cursor.use {
            assertEquals(3, it.count)
        }
    }

    @Test
    fun rawQuery_rating_min_filters() {
        // rating >= 4 → Alpha News (5), Gamma alpha daily (4) → 2 rows.
        val cursor = dao.queryArticles(
            SimpleSQLiteQuery(
                "SELECT * FROM articles WHERE rating >= ? ORDER BY title ASC",
                arrayOf<Any>(4)
            )
        )
        cursor.use {
            assertEquals(2, it.count)
            assertTrue(it.moveToFirst())
            // Ordered by title: "Alpha News" (5) then "Gamma alpha daily" (4).
            val titleIdx = it.getColumnIndexOrThrow("title")
            val ratingIdx = it.getColumnIndexOrThrow("rating")
            assertEquals("Alpha News", it.getString(titleIdx))
            assertEquals(5, it.getInt(ratingIdx))
        }
    }

    @Test
    fun rawQuery_exposes_expected_column_names() {
        val cursor = dao.queryArticles(SimpleSQLiteQuery("SELECT * FROM articles LIMIT 1"))
        cursor.use {
            it.moveToFirst()
            val columns = it.columnNames.toSet()
            assertTrue("title", "title" in columns)
            assertTrue("image_url", "image_url" in columns)
            assertTrue("color_red", "color_red" in columns)
            assertTrue("id", "id" in columns)
        }
    }

    private fun entity(title: String, rating: Int, red: Int) = ArticleEntity(
        title = title,
        description = "desc",
        imageUrl = "https://example.com/$title.jpg",
        rating = rating,
        colorRed = red,
        colorGreen = 0,
        colorBlue = 0
    )
}
