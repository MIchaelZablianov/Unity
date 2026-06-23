package com.unity.backendapp.data.model

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Tests the SQL rendering of [ArticleFilter]. This is the backend's filter-contract brain, so it
 * deserves focused JVM coverage (no Android/device needed — `toSqlQuery()` is pure Kotlin).
 *
 * URI parsing ([fromUri]) depends on `android.net.Uri` and is covered by the instrumented
 * ArticleContentProviderTest instead.
 */
class ArticleFilterTest {

    @Test
    fun `no filter renders bare select ordered by title`() {
        val q = ArticleFilter().toSqlQuery()
        assertEquals("SELECT * FROM articles ORDER BY title ASC", q.sql)
        assertEquals(emptyList<Any>(), q.args)
    }

    @Test
    fun `title filter uses NOCASE like with wildcard arg`() {
        val q = ArticleFilter(titleQuery = "news").toSqlQuery()
        assertEquals(
            "SELECT * FROM articles WHERE title LIKE ? COLLATE NOCASE ORDER BY title ASC",
            q.sql
        )
        assertEquals(listOf<Any>("%news%"), q.args)
    }

    @Test
    fun `blank title is treated as no filter`() {
        val q = ArticleFilter(titleQuery = "   ").toSqlQuery()
        assertEquals("SELECT * FROM articles ORDER BY title ASC", q.sql)
        assertEquals(emptyList<Any>(), q.args)
    }

    @Test
    fun `rating min filter binds the integer`() {
        val q = ArticleFilter(ratingMin = 4).toSqlQuery()
        assertEquals(
            "SELECT * FROM articles WHERE rating >= ? ORDER BY title ASC",
            q.sql
        )
        assertEquals(listOf<Any>(4), q.args)
    }

    @Test
    fun `title and rating combine with AND and preserve arg order`() {
        val q = ArticleFilter(titleQuery = "sport", ratingMin = 3).toSqlQuery()
        assertEquals(
            "SELECT * FROM articles WHERE title LIKE ? COLLATE NOCASE AND rating >= ? ORDER BY title ASC",
            q.sql
        )
        assertEquals(listOf<Any>("%sport%", 3), q.args)
    }

    @Test
    fun `user input is bound never interpolated`() {
        // A hostile query must end up as a single bind arg, not spliced into the SQL.
        val hostile = "'; DROP TABLE articles; --"
        val q = ArticleFilter(titleQuery = hostile).toSqlQuery()
        assertEquals(listOf<Any>("%$hostile%"), q.args)
        // The SQL itself contains no trace of the payload — only the placeholder.
        assertEquals(
            "SELECT * FROM articles WHERE title LIKE ? COLLATE NOCASE ORDER BY title ASC",
            q.sql
        )
    }
}
