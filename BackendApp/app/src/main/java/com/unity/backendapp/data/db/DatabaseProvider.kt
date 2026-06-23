package com.unity.backendapp.data.db

object DatabaseProvider {
    @Volatile
    var instance: ArticleDatabase? = null
}
