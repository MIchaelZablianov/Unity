package com.unity.backendapp.data.db

import android.database.Cursor
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.RawQuery
import androidx.sqlite.db.SupportSQLiteQuery

@Dao
interface ArticleDao {
    @RawQuery
    fun queryArticles(query: SupportSQLiteQuery): Cursor

    @Insert
    fun insertAll(articles: List<ArticleEntity>)

    @Query("SELECT COUNT(*) FROM articles")
    fun count(): Int
}
