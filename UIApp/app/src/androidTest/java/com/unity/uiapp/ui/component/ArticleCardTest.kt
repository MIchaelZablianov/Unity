package com.unity.uiapp.ui.component

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import com.unity.uiapp.data.model.Article
import com.unity.uiapp.data.model.PlaceholderColor
import com.unity.uiapp.ui.theme.UIAppTheme
import org.junit.Rule
import org.junit.Test

class ArticleCardTest {

    @get:Rule
    val composeRule = createComposeRule()

    private val testArticle = Article(
        id = 1,
        title = "Breaking News",
        description = "Something important happened today.",
        imageUrl = "https://example.com/image.jpg",
        rating = 5,
        placeholderColor = PlaceholderColor(100, 200, 150)
    )

    @Test
    fun displays_title_and_description() {
        composeRule.setContent {
            UIAppTheme {
                ArticleCard(article = testArticle)
            }
        }

        composeRule.onNodeWithText("Breaking News").assertIsDisplayed()
        composeRule.onNodeWithText("Something important happened today.").assertIsDisplayed()
    }

    @Test
    fun displays_rating() {
        composeRule.setContent {
            UIAppTheme {
                ArticleCard(article = testArticle)
            }
        }

        composeRule.onNodeWithText("5").assertIsDisplayed()
    }

    @Test
    fun displays_low_rating() {
        val lowRated = testArticle.copy(rating = 1)

        composeRule.setContent {
            UIAppTheme {
                ArticleCard(article = lowRated)
            }
        }

        composeRule.onNodeWithText("1").assertIsDisplayed()
    }
}
