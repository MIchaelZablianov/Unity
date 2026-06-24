package com.unity.backendapp.ui.viewmodel

import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.unity.backendapp.data.ArticlesRepository
import com.unity.backendapp.di.IoDispatcher
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@Immutable
data class DashboardUiState(
    val articleCount: Int = 0,
    val isLoading: Boolean = true,
    val error: String? = null
)

@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val repository: ArticlesRepository,
    @param:IoDispatcher private val ioDispatcher: CoroutineDispatcher
) : ViewModel() {

    private val _state = MutableStateFlow(DashboardUiState())
    val state: StateFlow<DashboardUiState> = _state.asStateFlow()

    init {
        loadArticleCount()
    }

    private fun loadArticleCount() {
        viewModelScope.launch(ioDispatcher) {
            try {
                // Ensure the dataset is seeded even when the backend app is launched standalone,
                // i.e. before any inbound IPC query has triggered seeding.
                repository.ensureSeeded()
                val count = repository.count()
                _state.update {
                    it.copy(articleCount = count, isLoading = false, error = null)
                }
            } catch (e: Exception) {
                _state.update {
                    it.copy(
                        isLoading = false,
                        error = "Failed to load article count: ${e.message ?: e::class.simpleName}"
                    )
                }
            }
        }
    }
}
