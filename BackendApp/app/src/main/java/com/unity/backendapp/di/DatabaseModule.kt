package com.unity.backendapp.di

import com.unity.backendapp.data.db.ArticleDao
import com.unity.backendapp.data.db.ArticleDatabase
import com.unity.backendapp.data.db.DatabaseProvider
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    /**
     * Returns the Room database built by the ContentProvider at process start.
     *
     * The provider runs before any Activity is created, so by the time Hilt satisfies
     * an injection the instance is already published to [DatabaseProvider]. This avoids
     * building a second Room instance and keeps a single write path during seeding.
     */
    @Provides
    @Singleton
    fun provideDatabase(): ArticleDatabase =
        DatabaseProvider.instance ?: error("Database not initialized")

    @Provides
    @Singleton
    fun provideArticleDao(db: ArticleDatabase): ArticleDao = db.articleDao()
}
