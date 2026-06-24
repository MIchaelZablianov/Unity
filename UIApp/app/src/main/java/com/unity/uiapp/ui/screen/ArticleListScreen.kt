package com.unity.uiapp.ui.screen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.unity.uiapp.ui.component.ArticleCard
import com.unity.uiapp.ui.viewmodel.ArticleUiState
import com.unity.uiapp.ui.viewmodel.ArticleViewModel

@Composable
fun ArticleListRoute(
    viewModel: ArticleViewModel,
    modifier: Modifier = Modifier
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    ArticleListScreen(
        state = state,
        onTitleQueryChange = viewModel::updateTitleQuery,
        onRatingMinChange = viewModel::updateRatingMin,
        onApplyFilters = viewModel::applyFilters,
        modifier = modifier
    )
}

@Composable
fun ArticleListScreen(
    state: ArticleUiState,
    onTitleQueryChange: (String) -> Unit,
    onRatingMinChange: (Int) -> Unit,
    onApplyFilters: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxSize()
    ) {
        FilterBar(
            titleQuery = state.titleQuery,
            ratingMin = state.ratingMin,
            onTitleQueryChange = onTitleQueryChange,
            onRatingMinChange = onRatingMinChange,
            onApply = onApplyFilters
        )

        when {
            state.isLoading -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }
            state.error != null -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = state.error ?: "",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
            state.isEmpty -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No articles found",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            else -> {
                LazyColumn(
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(state.articles, key = { it.id }) { article ->
                        ArticleCard(article = article)
                    }
                }
            }
        }
    }
}

@Composable
private fun FilterBar(
    titleQuery: String,
    ratingMin: Int,
    onTitleQueryChange: (String) -> Unit,
    onRatingMinChange: (Int) -> Unit,
    onApply: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp)
    ) {
        OutlinedTextField(
            value = titleQuery,
            onValueChange = onTitleQueryChange,
            label = { Text("Search by title") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        Spacer(modifier = Modifier.height(12.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Minimum rating: $ratingMin", style = MaterialTheme.typography.labelMedium)
            Spacer(modifier = Modifier.width(8.dp))
            Slider(
                value = ratingMin.toFloat(),
                onValueChange = { onRatingMinChange(it.toInt()) },
                valueRange = 1f..5f,
                steps = 3,
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        Button(
            onClick = onApply,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Apply Filters")
        }
    }
}
