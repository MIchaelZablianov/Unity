package com.unity.uiapp.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.unity.uiapp.data.model.Article
import com.unity.uiapp.data.model.PlaceholderColor

@Composable
fun ArticleCard(
    article: Article,
    modifier: Modifier = Modifier
) {
    val placeholderColor = Color(
        red = article.placeholderColor.red,
        green = article.placeholderColor.green,
        blue = article.placeholderColor.blue,
        alpha = 255
    )

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.semantics(mergeDescendants = true) {}) {
            Box {
                AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(article.imageUrl)
                        .crossfade(true)
                        .build(),
                    contentDescription = article.title,
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(16f / 9f)
                        .background(placeholderColor),
                    contentScale = ContentScale.Crop
                )
                RatingBadge(article.rating)
            }

            Column(modifier = Modifier.padding(12.dp)) {
                Text(
                    text = article.title,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = article.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
private fun RatingBadge(
    rating: Int,
    modifier: Modifier = Modifier
) {
    val color = when {
        rating >= 5 -> Color(0xFF4CAF50)
        rating >= 4 -> Color(0xFF8BC34A)
        rating >= 3 -> Color(0xFFFFC107)
        rating >= 2 -> Color(0xFFFF9800)
        else -> Color(0xFFF44336)
    }

    Box(
        modifier = modifier
            .padding(8.dp)
            .size(36.dp)
            .clip(CircleShape)
            .background(color),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = rating.toString(),
            color = Color.White,
            style = MaterialTheme.typography.labelMedium
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun ArticleCardPreview() {
    com.unity.uiapp.ui.theme.UIAppTheme {
        ArticleCard(
            article = Article(
                id = 1,
                title = "Breaking News",
                description = "Something important happened today.",
                imageUrl = "https://example.com/image.jpg",
                rating = 5,
                placeholderColor = PlaceholderColor(100, 200, 150)
            )
        )
    }
}
