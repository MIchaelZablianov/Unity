package com.unity.backendapp.di

import android.content.Context
import androidx.room.Room
import com.unity.backendapp.data.db.ArticleDatabase
import com.unity.backendapp.data.db.ArticleDao
import dagger.Module
import dagger.Provides
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    /**
     * The process-wide Room database. Hilt's `@Singleton` guarantees a single instance, which the
     * ContentProvider reaches through a Hilt `EntryPoint` — so provider IPC and the app's own
     * dashboard share one database without relying on provider initialization order.
     */
    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): ArticleDatabase =
        Room.databaseBuilder(context, ArticleDatabase::class.java, "articles.db")
            .fallbackToDestructiveMigration()
            .build()

    @Provides
    @Singleton
    fun provideArticleDao(db: ArticleDatabase): ArticleDao = db.articleDao()
}
