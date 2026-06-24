package com.unity.backendapp.provider

import android.content.ContentProvider
import android.content.ContentValues
import android.database.Cursor
import android.net.Uri
import android.os.Bundle
import android.util.Log
import com.unity.backendapp.data.ArticlesRepository
import com.unity.backendapp.data.model.ArticleFilter
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent

/**
 * IPC surface for the backend app.
 *
 * This class is intentionally thin — a translator from a `content://` URI to an [ArticleFilter],
 * which it hands to [ArticlesRepository]. All data/filter logic lives in the repository and
 * [ArticleFilter] so it stays unit-testable without a device; the provider only knows IPC plumbing.
 *
 * Collaborators are obtained from Hilt via [ProviderEntryPoint] rather than constructed by hand.
 * The lookup is deferred to [query]/[getType] (not [onCreate]) because a `ContentProvider` is
 * created before `Application.onCreate()`, where the Hilt component is not yet ready. By the time
 * any query arrives the app is fully initialized, so the `by lazy` access is safe.
 */
class ArticleContentProvider : ContentProvider() {

    @EntryPoint
    @InstallIn(SingletonComponent::class)
    interface ProviderEntryPoint {
        fun articlesRepository(): ArticlesRepository
    }

    private val repository: ArticlesRepository by lazy {
        EntryPointAccessors
            .fromApplication(context!!.applicationContext, ProviderEntryPoint::class.java)
            .articlesRepository()
    }

    override fun onCreate(): Boolean = true

    override fun query(
        uri: Uri,
        projection: Array<String>?,
        selection: String?,
        selectionArgs: Array<String>?,
        sortOrder: String?
    ): Cursor? = when (uriMatcher.match(uri)) {
        ARTICLES -> {
            // Seed lazily on first real query so process start isn't blocked by JSON I/O.
            repository.ensureSeeded()
            // Only the URI is consulted; `projection`/`selection`/`sortOrder` are ignored because
            // the filter contract travels via URI query parameters. This keeps the contract
            // explicit and prevents callers from injecting arbitrary SQL.
            repository.query(ArticleFilter.fromUri(uri))
        }
        else -> {
            Log.w(TAG, "query: unknown uri $uri")
            null
        }
    }

    override fun getType(uri: Uri): String? = when (uriMatcher.match(uri)) {
        ARTICLES -> MIME_ARTICLES
        else -> null
    }

    // Read-only provider: mutations are not part of the contract.
    override fun insert(uri: Uri, values: ContentValues?): Uri? = null
    override fun delete(uri: Uri, selection: String?, selectionArgs: Array<String>?): Int = 0
    override fun update(
        uri: Uri,
        values: ContentValues?,
        selection: String?,
        selectionArgs: Array<String>?
    ): Int = 0
    override fun call(method: String, arg: String?, extras: Bundle?): Bundle? = null

    companion object {
        private const val TAG = "BackendProvider"
        const val AUTHORITY = "com.unity.backendapp.provider"
        private const val PATH_ARTICLES = "articles"
        private const val ARTICLES = 1

        /** Vendor-specific MIME type for a directory (list) of articles. */
        private const val MIME_ARTICLES = "vnd.android.cursor.dir/vnd.$AUTHORITY.article"

        /** Base URI for the articles endpoint; shared shape with the UI app. */
        val ARTICLES_URI: Uri = Uri.parse("content://$AUTHORITY/$PATH_ARTICLES")

        private val uriMatcher = android.content.UriMatcher(android.content.UriMatcher.NO_MATCH).apply {
            addURI(AUTHORITY, PATH_ARTICLES, ARTICLES)
        }
    }
}
