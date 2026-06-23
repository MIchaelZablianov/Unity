package com.unity.uiapp.ui.viewmodel

import com.unity.uiapp.data.ArticlesDataSource
import com.unity.uiapp.data.model.Article
import com.unity.uiapp.data.model.ArticleFilter
import com.unity.uiapp.data.model.PlaceholderColor
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ArticleViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var fakeDataSource: FakeArticlesDataSource
    private lateinit var viewModel: ArticleViewModel

    private val testArticles = listOf(
        Article(id = 1, title = "Title A", description = "Desc A", imageUrl = "https://example.com/a.jpg", rating = 5, placeholderColor = PlaceholderColor(100, 100, 100)),
        Article(id = 2, title = "Title B", description = "Desc B", imageUrl = "https://example.com/b.jpg", rating = 3, placeholderColor = PlaceholderColor(100, 100, 100)),
        Article(id = 3, title = "Title C", description = "Desc C", imageUrl = "https://example.com/c.jpg", rating = 1, placeholderColor = PlaceholderColor(100, 100, 100))
    )

    @Before
    fun setUp() {
        fakeDataSource = FakeArticlesDataSource(testArticles)
        viewModel = ArticleViewModel(fakeDataSource, ioDispatcher = testDispatcher)
    }

    @Test
    fun initialState_isLoading() {
        assertTrue(viewModel.state.value.isLoading)
    }

    @Test
    fun afterInit_articlesLoaded() = runTest(testDispatcher) {
        testDispatcher.scheduler.advanceUntilIdle()
        val state = viewModel.state.value
        assertEquals(3, state.articles.size)
        assertFalse(state.isLoading)
    }

    @Test
    fun updateTitleQuery_updatesState() = runTest(testDispatcher) {
        viewModel.updateTitleQuery("Title A")
        assertEquals("Title A", viewModel.state.value.titleQuery)
    }

    @Test
    fun updateTitleQuery_doesNotTriggerFilter() = runTest(testDispatcher) {
        viewModel.updateTitleQuery("Nonexistent")
        viewModel.applyFilters()
        testDispatcher.scheduler.advanceUntilIdle()
        val state = viewModel.state.value
        assertTrue(state.articles.isEmpty())
    }

    @Test
    fun updateRatingMin_clampsToRange() = runTest(testDispatcher) {
        viewModel.updateRatingMin(6)
        assertEquals(5, viewModel.state.value.ratingMin)
    }

    @Test
    fun applyFilters_returnsFilteredResults() = runTest(testDispatcher) {
        viewModel.updateTitleQuery("Title A")
        viewModel.applyFilters()
        testDispatcher.scheduler.advanceUntilIdle()
        val state = viewModel.state.value
        assertEquals(1, state.articles.size)
        assertEquals("Title A", state.articles.first().title)
    }

    @Test
    fun applyFilters_emptyQuery_returnsAll() = runTest(testDispatcher) {
        testDispatcher.scheduler.advanceUntilIdle()
        viewModel.updateTitleQuery("")
        viewModel.applyFilters()
        testDispatcher.scheduler.advanceUntilIdle()
        val state = viewModel.state.value
        assertEquals(3, state.articles.size)
    }

    @Test
    fun applyFilters_ratingMin() = runTest(testDispatcher) {
        testDispatcher.scheduler.advanceUntilIdle()
        viewModel.updateRatingMin(4)
        viewModel.applyFilters()
        testDispatcher.scheduler.advanceUntilIdle()
        val state = viewModel.state.value
        assertEquals(1, state.articles.size)
    }

    @Test
    fun applyFilters_emptyResult_setsIsEmpty() = runTest(testDispatcher) {
        viewModel.updateTitleQuery("Nonexistent")
        viewModel.applyFilters()
        testDispatcher.scheduler.advanceUntilIdle()
        val state = viewModel.state.value
        assertEquals(0, state.articles.size)
        assertTrue(state.isEmpty)
    }

    @Test
    fun applyFilters_error_setsError() = runTest(testDispatcher) {
        val failing = FakeArticlesDataSource(testArticles).also { it.shouldThrow = true }
        val vm = ArticleViewModel(failing, ioDispatcher = testDispatcher)
        testDispatcher.scheduler.advanceUntilIdle()
        val state = vm.state.value
        assertFalse(state.isLoading)
        assertNotNull(state.error)
        assertTrue(state.articles.isEmpty())
        // The defining distinction from "no matches": a failure must NOT be reported as empty.
        assertFalse("error must not set isEmpty", state.isEmpty)
    }
}

private class FakeArticlesDataSource(
    private val articles: List<Article>
) : ArticlesDataSource {
    var shouldThrow: Boolean = false

    override suspend fun fetchArticles(filter: ArticleFilter): List<Article> {
        if (shouldThrow) throw RuntimeException("Simulated error")
        return articles.filter { a ->
            (filter.titleQuery == null || a.title.contains(filter.titleQuery, ignoreCase = true)) &&
                (filter.ratingMin == null || a.rating >= filter.ratingMin)
        }
    }
}
