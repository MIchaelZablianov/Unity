package com.unity.uiapp.data.model

import androidx.compose.runtime.Immutable

@Immutable
data class PlaceholderColor(
    val red: Int,
    val green: Int,
    val blue: Int
)

@Immutable
data class Article(
    val id: Int = 0,
    val title: String,
    val description: String,
    val imageUrl: String,
    val rating: Int,
    val placeholderColor: PlaceholderColor
)
