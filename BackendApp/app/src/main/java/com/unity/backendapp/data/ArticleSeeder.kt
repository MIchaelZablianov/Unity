package com.unity.backendapp.data

import android.content.Context
import android.util.Log
import com.unity.backendapp.data.db.ArticleDao
import com.unity.backendapp.data.model.ArticlesResponse
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.serialization.json.Json
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Seeds the Room database from the bundled `assets/raw/articles.json` exactly once.
 *
 * Seeding is centralized here (rather than living in the ContentProvider) so that *every* entry
 * point into the data — the IPC [com.unity.backendapp.provider.ArticleContentProvider] and the
 * backend app's own dashboard — observes a populated database. Previously only an inbound IPC
 * query triggered seeding, so launching the backend app standalone reported zero articles.
 *
 * [ensureSeeded] is idempotent and thread-safe: the `seeded` fast-path avoids locking once done,
 * the `count() > 0` check makes it safe across process restarts (Room persists to disk), and the
 * `synchronized` block guarantees concurrent first-callers can never observe a half-seeded table.
 * A failed attempt is not marked complete, so a later call retries.
 */
@Singleton
class ArticleSeeder @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val dao: ArticleDao
) {
    @Volatile
    private var seeded = false
    private val lock = Any()

    fun ensureSeeded() {
        if (seeded) return
        synchronized(lock) {
            if (seeded) return
            if (dao.count() > 0) {
                seeded = true
                return
            }

            val json = try {
                context.assets.open(ASSET_PATH).bufferedReader().use { it.readText() }
            } catch (e: Exception) {
                // Leave `seeded` false so a later call retries; a missing asset is a build error.
                Log.e(TAG, "ensureSeeded: failed to read $ASSET_PATH", e)
                return
            }

            val response = try {
                jsonParser.decodeFromString<ArticlesResponse>(json)
            } catch (e: Exception) {
                Log.e(TAG, "ensureSeeded: failed to parse $ASSET_PATH", e)
                return
            }

            val entities = response.toEntities()
            dao.insertAll(entities)
            seeded = true
            Log.d(TAG, "ensureSeeded: seeded ${entities.size} articles")
        }
    }

    companion object {
        private const val TAG = "ArticleSeeder"
        private const val ASSET_PATH = "raw/articles.json"
        private val jsonParser = Json { ignoreUnknownKeys = true }
    }
}
