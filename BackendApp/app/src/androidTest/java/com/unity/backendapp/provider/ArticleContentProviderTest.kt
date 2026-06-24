package com.unity.backendapp.provider

import android.content.ContentProviderClient
import android.net.Uri
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ArticleContentProviderTest {

    private lateinit var providerClient: ContentProviderClient
    private val baseUri = Uri.parse("content://com.unity.backendapp.provider/articles")

    @Before
    fun setUp() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        providerClient = context.contentResolver
            .acquireContentProviderClient("com.unity.backendapp.provider")
            ?: throw IllegalStateException("ContentProvider not available")
        // Seeding runs synchronously inside the first query() (ArticleSeeder.ensureSeeded), so the
        // queries below see a populated database immediately — no retry/poll loop needed.
    }

    @Test
    fun provider_is_available() {
        assertNotNull(providerClient)
    }

    @Test
    fun get_all_articles_returns_full_list() {
        val cursor = providerClient.query(baseUri, null, null, null, null)
        assertNotNull(cursor)
        cursor?.use {
            assertTrue(it.count > 100)
        }
    }

    @Test
    fun filter_by_title() {
        val uri = baseUri.buildUpon()
            .appendQueryParameter("titleQuery", "biden")
            .build()
        val cursor = providerClient.query(uri, null, null, null, null)
        cursor?.use {
            assertTrue("expected at least 1 match for 'biden'", it.count > 0)
            while (it.moveToNext()) {
                val title = it.getString(it.getColumnIndexOrThrow("title"))
                assertTrue(
                    "title '$title' should contain 'biden' (case-insensitive)",
                    title.contains("biden", ignoreCase = true)
                )
            }
        }
    }

    @Test
    fun filter_by_rating_min() {
        val uri = baseUri.buildUpon()
            .appendQueryParameter("ratingMin", "4")
            .build()
        val cursor = providerClient.query(uri, null, null, null, null)
        cursor?.use {
            assertTrue("expected at least 1 article with rating >= 4", it.count > 0)
            while (it.moveToNext()) {
                val rating = it.getInt(it.getColumnIndexOrThrow("rating"))
                assertTrue("rating $rating should be >= 4", rating >= 4)
            }
        }
    }

    @Test
    fun filter_by_title_and_rating() {
        val uri = baseUri.buildUpon()
            .appendQueryParameter("titleQuery", "biden")
            .appendQueryParameter("ratingMin", "4")
            .build()
        val cursor = providerClient.query(uri, null, null, null, null)
        cursor?.use {
            assertTrue("expected at least 1 match for 'biden' with rating >= 4", it.count > 0)
            while (it.moveToNext()) {
                val title = it.getString(it.getColumnIndexOrThrow("title"))
                val rating = it.getInt(it.getColumnIndexOrThrow("rating"))
                assertTrue(
                    "title '$title' should contain 'biden'",
                    title.contains("biden", ignoreCase = true)
                )
                assertTrue("rating $rating should be >= 4", rating >= 4)
            }
        }
    }

    @Test
    fun no_filter_returns_all_articles() {
        val cursor = providerClient.query(baseUri, null, null, null, null)
        cursor?.use {
            assertTrue(it.count > 100)
        }
    }

    @Test
    fun cursor_has_expected_columns() {
        val cursor = providerClient.query(baseUri, null, null, null, null)
        cursor?.use {
            assertTrue(it.moveToFirst())
            val columns = it.columnNames.toList()
            assertEquals(
                listOf(
                    "id",
                    "title",
                    "description",
                    "image_url",
                    "rating",
                    "color_red",
                    "color_green",
                    "color_blue"
                ),
                columns
            )
        }
    }

    @Test
    fun unknown_uri_returns_null() {
        val unknownUri = Uri.parse("content://com.unity.backendapp.provider/unknown")
        val cursor = providerClient.query(unknownUri, null, null, null, null)
        assertNull(cursor)
    }
}
