package com.unity.backendapp.provider

import android.content.ContentProvider
import android.content.ContentValues
import android.content.Context
import android.content.UriMatcher
import android.database.Cursor
import android.net.Uri
import android.util.Log
import androidx.room.Room
import com.unity.backendapp.data.ArticlesRepository
import com.unity.backendapp.data.db.ArticleDatabase
import com.unity.backendapp.data.db.DatabaseProvider
import com.unity.backendapp.data.model.ArticleFilter
import com.unity.backendapp.data.model.ArticlesResponse
import kotlinx.serialization.json.Json
import java.util.concurrent.atomic.AtomicBoolean

/**
 * IPC surface for the backend app.
 *
 * Responsibilities are intentionally minimal — this class is a translator:
 *  - `onCreate()` builds Room and seeds it from the bundled JSON,
 *  - `query()` maps a URI to an [ArticleFilter] and hands it to [ArticlesRepository],
 *    which owns the filter→SQL logic and storage access.
 *
 * All data/filter logic lives in `ArticlesRepository` / `ArticleFilter` so it is unit-testable
 * without an Android device. The provider itself only knows about IPC plumbing.
 */
class ArticleContentProvider : ContentProvider() {

    companion object {
        private const val TAG = "BackendProvider"
        const val AUTHORITY = "com.unity.backendapp.provider"
        private const val PATH_ARTICLES = "articles"
        private const val ARTICLES = 1

        /** Base URI for the articles endpoint; shared shape with the UI app. */
        val ARTICLES_URI: Uri = Uri.parse("content://$AUTHORITY/$PATH_ARTICLES")

        private val uriMatcher = UriMatcher(UriMatcher.NO_MATCH).apply {
            addURI(AUTHORITY, PATH_ARTICLES, ARTICLES)
        }
    }

    private var database: ArticleDatabase? = null
    private var repository: ArticlesRepository? = null
    private val seeded = AtomicBoolean(false)

    override fun onCreate(): Boolean {
        Log.d(TAG, "onCreate: initializing Room database")
        val context = context ?: return false

        // Build Room synchronously. This is cheap (file open + sqlite init; no I/O on the
        // dataset yet) and runs once on the provider's main thread at process start — the
        // documented contract. Seeding happens lazily on the first query() (see query()) so a
        // slow JSON read never blocks process creation. This removes the previous race where
        // query() returned null until a background Thread finished.
        database = Room.databaseBuilder(
            context.applicationContext,
            ArticleDatabase::class.java,
            "articles.db"
        ).fallbackToDestructiveMigration().build()

        DatabaseProvider.instance = database
        repository = ArticlesRepository(database!!.articleDao())
        return true
    }

    /**
     * Seeds the database from the bundled JSON on the first query that needs data.
     * Idempotent via [seeded]; safe to call from every query() because it returns immediately
     * once the first call has completed.
     */
    private fun seedOnce(context: Context) {
        if (!seeded.compareAndSet(false, true)) return
        val repo = repository ?: return
        if (repo.count() > 0) {
            Log.d(TAG, "seedOnce: database already has data, skipping seed")
            return
        }

        Log.d(TAG, "seedOnce: database empty, seeding from JSON")
        val jsonString = try {
            context.assets.open("raw/articles.json")
                .bufferedReader().use { it.readText() }
        } catch (e: Exception) {
            Log.e(TAG, "seedOnce: failed to read articles.json", e)
            return
        }

        val json = Json { ignoreUnknownKeys = true }
        val response: ArticlesResponse = try {
            json.decodeFromString(jsonString)
        } catch (e: Exception) {
            Log.e(TAG, "seedOnce: failed to parse JSON", e)
            return
        }
        val entities = response.toEntities()
        database!!.articleDao().insertAll(entities)
        Log.d(TAG, "seedOnce: seeded ${entities.size} articles into database")
    }

    override fun query(
        uri: Uri,
        projection: Array<String>?,
        selection: String?,
        selectionArgs: Array<String>?,
        sortOrder: String?
    ): Cursor? {
        Log.d(TAG, "query: uri=$uri")
        val repo = repository ?: return null
        val context = context ?: return null

        when (uriMatcher.match(uri)) {
            ARTICLES -> {
                // Seed lazily on first real query so process start isn't blocked by JSON I/O.
                seedOnce(context)
                // Only the URI is consulted; `projection`/`selection`/`sortOrder` are ignored
                // because the filter contract travels via URI query parameters. This keeps the
                // contract explicit and prevents callers from injecting arbitrary SQL.
                val filter = ArticleFilter.fromUri(uri)
                return repo.query(filter)
            }
            else -> {
                Log.w(TAG, "query: unknown uri $uri")
                return null
            }
        }
    }

    override fun getType(uri: Uri): String? = null
    override fun insert(uri: Uri, values: ContentValues?): Uri? = null
    override fun delete(uri: Uri, selection: String?, selectionArgs: Array<String>?): Int = 0
    override fun update(
        uri: Uri,
        values: ContentValues?,
        selection: String?,
        selectionArgs: Array<String>?
    ): Int = 0

    override fun call(method: String, arg: String?, extras: android.os.Bundle?): android.os.Bundle? = null
}
