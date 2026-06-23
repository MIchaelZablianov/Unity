package com.unity.backendapp.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class PlaceholderColor(
    val red: Int,
    val green: Int,
    val blue: Int
)

@Serializable
data class Article(
    val title: String,
    val description: String,
    @SerialName("image_url") val imageUrl: String,
    val rating: Int,
    val placeholderColor: PlaceholderColor
)
