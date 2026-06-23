package com.unity.backendapp.data.model

import com.unity.backendapp.data.db.ArticleEntity
import kotlinx.serialization.Serializable

@Serializable
data class ArticlesResponse(val articles: List<Article>) {

    /** Maps the wire model to the persisted entity (flattening placeholderColor). */
    fun toEntities(): List<ArticleEntity> = articles.map { it.toEntity() }
}

/** DTO ↔ entity mapping. Centralized here so seeding and any future source share one path. */
fun Article.toEntity(): ArticleEntity = ArticleEntity(
    title = title,
    description = description,
    imageUrl = imageUrl,
    rating = rating,
    colorRed = placeholderColor.red,
    colorGreen = placeholderColor.green,
    colorBlue = placeholderColor.blue
)
