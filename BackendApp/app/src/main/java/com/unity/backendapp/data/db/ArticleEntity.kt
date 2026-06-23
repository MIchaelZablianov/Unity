package com.unity.backendapp.data.db

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "articles")
data class ArticleEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val title: String,
    val description: String,
    @ColumnInfo(name = "image_url") val imageUrl: String,
    val rating: Int,
    @ColumnInfo(name = "color_red") val colorRed: Int,
    @ColumnInfo(name = "color_green") val colorGreen: Int,
    @ColumnInfo(name = "color_blue") val colorBlue: Int
)
