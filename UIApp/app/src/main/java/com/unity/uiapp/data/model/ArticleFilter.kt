package com.unity.uiapp.data.model

import android.net.Uri

/**
 * Filter parameters for an articles query — the UI-side mirror of BackendApp's contract.
 *
 * Keeping filters as a single value object (rather than a fixed-arity method signature) is
 * what lets a *new* filter type be added without a structural rewrite: add a field here and
 * in [toUriParams], render one new control, and read it in the ViewModel. Nothing downstream
 * (the data source, the IPC encoding) changes shape.
 *
 * @param titleQuery case-insensitive substring match on the title, or null for "any".
 * @param ratingMin  inclusive lower bound on the integer rating, or null for "any".
 */
data class ArticleFilter(
    val titleQuery: String? = null,
    val ratingMin: Int? = null
) {

    /**
     * Appends this filter's values onto [base] as URI query parameters.
     * Null/blank fields are skipped so the backend treats them as "no filter".
     */
    fun toUri(base: Uri): Uri = base.buildUpon().apply {
        titleQuery?.takeIf { it.isNotBlank() }?.let { appendQueryParameter(PARAM_TITLE, it) }
        ratingMin?.let { appendQueryParameter(PARAM_RATING_MIN, it.toString()) }
    }.build()

    companion object {
        /** URI query-parameter names — must match BackendApp's `ArticleFilter`. */
        const val PARAM_TITLE = "titleQuery"
        const val PARAM_RATING_MIN = "ratingMin"
    }
}
