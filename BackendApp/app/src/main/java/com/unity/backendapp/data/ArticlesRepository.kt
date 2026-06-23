package com.unity.backendapp.data

import android.database.Cursor
import com.unity.backendapp.data.db.ArticleDao
import com.unity.backendapp.data.model.ArticleFilter
import com.unity.backendapp.data.model.ArticleSqlQuery
import androidx.sqlite.db.SimpleSQLiteQuery
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Domain layer between the IPC surface (ContentProvider) and the storage layer (Room).
 *
 * The provider is responsible only for translating IPC ↔ [ArticleFilter]; this class
 * owns how a filter becomes a [Cursor]. It is pure JVM-testable code (no Android
 * Activity/Context deps) — the [ArticleDao] abstraction is what makes that possible.
 */
@Singleton
class ArticlesRepository @Inject constructor(
    private val dao: ArticleDao
) {
    /** Runs [filter] against the dataset and returns a live Cursor over the matches. */
    fun query(filter: ArticleFilter): Cursor {
        val rendered: ArticleSqlQuery = filter.toSqlQuery()
        return dao.queryArticles(SimpleSQLiteQuery(rendered.sql, rendered.args.toTypedArray()))
    }

    /** Number of stored articles; used by the dashboard. */
    fun count(): Int = dao.count()
}
