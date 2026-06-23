package com.unity.uiapp.data.model

import android.net.Uri
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Tests that [ArticleFilter.toUri] encodes only non-null/non-blank fields onto the base URI.
 *
 * This is the UI-side half of the IPC filter contract, so it lives in androidTest (it depends on
 * `android.net.Uri`). The other half — the backend's ArticleFilter.fromUri → SQL — is covered by
 * ArticleContentProviderTest end to end.
 */
@RunWith(AndroidJUnit4::class)
class ArticleFilterUriTest {

    private val base = Uri.parse("content://com.unity.backendapp.provider/articles")

    @Test
    fun empty_filter_yields_bare_uri() {
        val uri = ArticleFilter().toUri(base)
        assertEquals(base.toString(), uri.toString())
    }

    @Test
    fun title_only_is_appended() {
        val uri = ArticleFilter(titleQuery = "news").toUri(base)
        assertEquals("$base?titleQuery=news", uri.toString())
        assertNull(uri.getQueryParameter(ArticleFilter.PARAM_RATING_MIN))
    }

    @Test
    fun rating_only_is_appended() {
        val uri = ArticleFilter(ratingMin = 4).toUri(base)
        assertEquals("$base?ratingMin=4", uri.toString())
        assertNull(uri.getQueryParameter(ArticleFilter.PARAM_TITLE))
    }

    @Test
    fun both_fields_appended_together() {
        val uri = ArticleFilter(titleQuery = "sport", ratingMin = 3).toUri(base)
        assertEquals("$base?titleQuery=sport&ratingMin=3", uri.toString())
    }

    @Test
    fun blank_title_is_skipped() {
        val uri = ArticleFilter(titleQuery = "   ", ratingMin = 2).toUri(base)
        assertEquals("$base?ratingMin=2", uri.toString())
    }
}
