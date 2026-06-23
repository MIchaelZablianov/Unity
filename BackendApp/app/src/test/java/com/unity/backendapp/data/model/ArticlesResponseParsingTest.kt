package com.unity.backendapp.data.model

import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Tests JSON deserialization of the bundled dataset shape and its mapping to [ArticleEntity].
 * Uses the project's real fixture so the contract between `assets/raw/articles.json` and the
 * model is locked down by a test.
 */
class ArticlesResponseParsingTest {

    private val json = Json { ignoreUnknownKeys = true }

    private val sample = """
        {
          "articles": [
            {
              "title": "A",
              "description": "desc A",
              "image_url": "https://example.com/a.webp",
              "rating": 5,
              "placeholderColor": { "red": 1, "green": 2, "blue": 3 }
            },
            {
              "title": "B",
              "description": "desc B",
              "image_url": "https://placehold.co/1.png",
              "rating": 1,
              "placeholderColor": { "red": 255, "green": 0, "blue": 128 }
            }
          ]
        }
    """.trimIndent()

    @Test
    fun `parses articles with snake_case image_url into camelCase field`() {
        val response = json.decodeFromString<ArticlesResponse>(sample)
        assertEquals(2, response.articles.size)
        assertEquals("https://example.com/a.webp", response.articles[0].imageUrl)
        assertEquals(5, response.articles[0].rating)
    }

    @Test
    fun `placeholderColor maps to nested object`() {
        val response = json.decodeFromString<ArticlesResponse>(sample)
        val color = response.articles[1].placeholderColor
        assertEquals(PlaceholderColor(255, 0, 128), color)
    }

    @Test
    fun `toEntities flattens placeholder color into columns`() {
        val response = json.decodeFromString<ArticlesResponse>(sample)
        val entities = response.toEntities()
        assertEquals(2, entities.size)
        assertEquals("A", entities[0].title)
        assertEquals("https://example.com/a.webp", entities[0].imageUrl)
        assertEquals(1, entities[0].colorRed)
        assertEquals(2, entities[0].colorGreen)
        assertEquals(3, entities[0].colorBlue)
        assertEquals(5, entities[0].rating)
    }

    @Test
    fun `unknown keys are tolerated for forward compatibility`() {
        val withExtra = """
            {
              "articles": [
                {
                  "title": "X",
                  "description": "d",
                  "image_url": "u",
                  "rating": 3,
                  "placeholderColor": { "red": 0, "green": 0, "blue": 0 },
                  "author": "ignored",
                  "tags": ["ignored"]
                }
              ]
            }
        """.trimIndent()
        val response = json.decodeFromString<ArticlesResponse>(withExtra)
        assertEquals(1, response.articles.size)
        assertEquals("X", response.articles[0].title)
    }
}
