package com.unity.backendapp.data.model

import android.net.Uri

/**
 * Filter parameters for an articles query.
 *
 * This is the single place that owns the filter contract. Adding a new filter
 * type is a localized change:
 *  1. add a nullable field here,
 *  2. append its SQL clause in [toSqlQuery],
 *  3. (if callers can supply it) read its URI param in [fromUri].
 *
 * No provider / repository / DAO signature changes are required, so new filters
 * do not force a structural rewrite on either side of the IPC boundary.
 *
 * @param titleQuery case-insensitive substring match on the title, or null for "any".
 * @param ratingMin  inclusive lower bound on the integer rating, or null for "any".
 */
data class ArticleFilter(
    val titleQuery: String? = null,
    val ratingMin: Int? = null
) {

    /**
     * Renders this filter to a parameterized `SELECT ... FROM articles ... ORDER BY` query.
     *
     * User-supplied values are always bound (the `?` placeholders), never string-interpolated,
     * so the result is injection-safe. Only column/table names are literal, and those are
     * compile-time constants owned by this module.
     */
    fun toSqlQuery(): ArticleSqlQuery {
        val conditions = mutableListOf<String>()
        val args = mutableListOf<Any>()

        titleQuery?.takeIf { it.isNotBlank() }?.let {
            conditions.add("title LIKE ? COLLATE NOCASE")
            args.add("%$it%")
        }
        ratingMin?.let {
            conditions.add("rating >= ?")
            args.add(it)
        }

        val sql = buildString {
            append("SELECT ")
            append(ARTICLE_COLUMNS.joinToString(", "))
            append(" FROM articles")
            if (conditions.isNotEmpty()) {
                append(" WHERE ")
                append(conditions.joinToString(" AND "))
            }
            append(" ORDER BY title ASC")
        }
        return ArticleSqlQuery(sql, args)
    }

    companion object {
        /** URI query-parameter names — the IPC contract between the two apps. */
        const val PARAM_TITLE = "titleQuery"
        const val PARAM_RATING_MIN = "ratingMin"

        val ARTICLE_COLUMNS = listOf(
            "id",
            "title",
            "description",
            "image_url",
            "rating",
            "color_red",
            "color_green",
            "color_blue"
        )

        /**
         * Parses a filter from a ContentProvider URI's query parameters.
         * Missing/blank params yield nulls (i.e. "no filter on this field").
         */
        fun fromUri(uri: Uri): ArticleFilter = ArticleFilter(
            titleQuery = uri.getQueryParameter(PARAM_TITLE)?.takeIf { it.isNotBlank() },
            ratingMin = uri.getQueryParameter(PARAM_RATING_MIN)?.toIntOrNull()
        )
    }
}

/** A fully-rendered, parameterized SQL statement plus its bind arguments. */
data class ArticleSqlQuery(val sql: String, val args: List<Any>)
