package com.unity.uiapp.ui.viewmodel

import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.unity.uiapp.core.Logger
import com.unity.uiapp.data.ArticlesDataSource
import com.unity.uiapp.data.model.Article
import com.unity.uiapp.data.model.ArticleFilter
import com.unity.uiapp.di.IoDispatcher
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Job
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Editable filter form shown in the UI. Lives in [ArticleUiState] so Compose can bind to it
 * directly; converted to an immutable [ArticleFilter] when the user applies filters.
 */
@Immutable
data class ArticleUiState(
    val articles: List<Article> = emptyList(),
    val isLoading: Boolean = false,
    val isEmpty: Boolean = false,
    val titleQuery: String = "",
    val ratingMin: Int = 1,
    val error: String? = null
) {
    /**
     * Renders the form to the filter value object that crosses the data-source boundary.
     * Adding a new filter field is: add the field above + one line here.
     */
    fun toFilter(): ArticleFilter = ArticleFilter(
        titleQuery = titleQuery.ifBlank { null },
        ratingMin = ratingMin
    )
}

@HiltViewModel
class ArticleViewModel @Inject constructor(
    private val dataSource: ArticlesDataSource,
    @param:IoDispatcher private val ioDispatcher: CoroutineDispatcher,
    private val logger: Logger
) : ViewModel() {

    private val tag = "ArticleViewModel"
    private val _state = MutableStateFlow(ArticleUiState(isLoading = true))
    val state: StateFlow<ArticleUiState> = _state.asStateFlow()

    private var fetchJob: Job? = null

    init {
        logger.d(tag, "init: calling applyFilters()")
        applyFilters()
    }

    fun updateTitleQuery(query: String) {
        logger.d(tag, "updateTitleQuery: $query")
        _state.update { it.copy(titleQuery = query) }
    }

    fun updateRatingMin(min: Int) {
        val clamped = min.coerceIn(RATING_MIN, RATING_MAX)
        logger.d(tag, "updateRatingMin: $min -> clamped to $clamped")
        _state.update { it.copy(ratingMin = clamped) }
    }

    /**
     * Runs the current filter against the backend. Cancels any in-flight fetch first, so a rapid
     * second apply can't overwrite the latest result out of order: the cancelled coroutine throws
     * at its next suspension point / `ensureActive()` and never reaches the state update.
     */
    fun applyFilters() {
        fetchJob?.cancel()
        val filter = _state.value.toFilter()
        _state.update { it.copy(isLoading = true, error = null, isEmpty = false) }
        fetchJob = viewModelScope.launch(ioDispatcher) {
            logger.d(tag, "applyFilters: filter=$filter")
            try {
                val articles = dataSource.fetchArticles(filter)
                ensureActive()
                _state.update {
                    it.copy(
                        articles = articles,
                        isLoading = false,
                        isEmpty = articles.isEmpty(),
                        error = null
                    )
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                logger.e(tag, "applyFilters: exception", e)
                _state.update {
                    it.copy(
                        isLoading = false,
                        error = "Failed to load articles: ${e.message ?: e::class.simpleName}"
                    )
                }
            }
        }
    }

    companion object {
        const val RATING_MIN = 1
        const val RATING_MAX = 5
    }
}
