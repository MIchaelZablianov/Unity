package com.unity.uiapp.di

import com.unity.uiapp.core.AndroidLogger
import com.unity.uiapp.core.Logger
import com.unity.uiapp.data.ArticlesDataSource
import com.unity.uiapp.data.ContentResolverDataSource
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import javax.inject.Qualifier
import javax.inject.Singleton

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class IoDispatcher

@Module
@InstallIn(SingletonComponent::class)
abstract class AppModule {
    @Binds
    @Singleton
    abstract fun bindArticlesDataSource(
        impl: ContentResolverDataSource
    ): ArticlesDataSource

    @Binds
    @Singleton
    abstract fun bindLogger(impl: AndroidLogger): Logger
}

@Module
@InstallIn(SingletonComponent::class)
object DispatchersModule {
    @IoDispatcher
    @Provides
    @Singleton
    fun provideIoDispatcher(): CoroutineDispatcher = Dispatchers.IO
}
