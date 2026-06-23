package com.unity.uiapp.data

import com.unity.uiapp.data.model.Article
import com.unity.uiapp.data.model.ArticleFilter

/**
 * Abstraction over the article data source (BackendApp via ContentProvider in production).
 *
 * The filter is a single [ArticleFilter] value object so that new filter types can be added
 * without changing this signature — see [ArticleFilter].
 */
interface ArticlesDataSource {
    suspend fun fetchArticles(filter: ArticleFilter): List<Article>
}
