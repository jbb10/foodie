package com.foodie.app.ui.sample

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.foodie.app.domain.model.MealEntry
import com.foodie.app.ui.theme.FoodieTheme
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/**
 * Sample screen demonstrating the complete MVVM architecture pattern.
 *
 * This screen shows:
 * - Hilt ViewModel injection with hiltViewModel()
 * - StateFlow collection with collectAsStateWithLifecycle()
 * - Handling loading, success, and error states
 * - User interaction triggering ViewModel actions
 * - Material Design 3 components
 *
 * Architecture flow demonstrated:
 * 1. Screen is composed with injected ViewModel
 * 2. State is collected from ViewModel's StateFlow
 * 3. UI renders based on state (loading/data/error)
 * 4. User taps button → calls ViewModel method
 * 5. ViewModel updates state → UI automatically recomposes
 */
@Composable
fun SampleScreen(
    viewModel: SampleViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    // Show error in snackbar when error state is set
    LaunchedEffect(state.error) {
        state.error?.let { errorMessage ->
            snackbarHostState.showSnackbar(errorMessage)
            viewModel.clearError()
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { paddingValues ->
        SampleScreenContent(
            state = state,
            onLoadDataClick = { viewModel.loadSampleData() },
            modifier = Modifier.padding(paddingValues),
        )
    }
}

@Composable
private fun SampleScreenContent(
    state: SampleState,
    onLoadDataClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        when {
            state.isLoading -> {
                LoadingContent()
            }
            state.meals.isEmpty() -> {
                EmptyContent(onLoadDataClick = onLoadDataClick)
            }
            else -> {
                MealListContent(
                    meals = state.meals,
                    onLoadDataClick = onLoadDataClick,
                )
            }
        }
    }
}

@Composable
private fun LoadingContent() {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        CircularProgressIndicator()
        Text(
            text = "Loading meal history...",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun EmptyContent(onLoadDataClick: () -> Unit) {
    Column(
        modifier = Modifier.padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text(
            text = "MVVM Pattern Demo",
            style = MaterialTheme.typography.headlineMedium,
            textAlign = TextAlign.Center,
        )
        Text(
            text = "This screen demonstrates the complete MVVM architecture with:\n\n" +
                "• Hilt dependency injection\n" +
                "• Repository pattern\n" +
                "• StateFlow reactive state\n" +
                "• Result wrapper error handling\n" +
                "• Health Connect integration",
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Button(
            onClick = onLoadDataClick,
            modifier = Modifier.padding(top = 16.dp),
        ) {
            Text("Load Sample Data")
        }
    }
}

@Composable
private fun MealListContent(
    meals: List<MealEntry>,
    onLoadDataClick: () -> Unit,
) {
    Column(modifier = Modifier.fillMaxSize()) {
        // Header with reload button
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = "Meal History",
                style = MaterialTheme.typography.headlineMedium,
            )
            Text(
                text = "${meals.size} meals loaded from Health Connect",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            TextButton(onClick = onLoadDataClick) {
                Text("Reload")
            }
        }

        // Meal list
        LazyColumn(
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            items(meals, key = { it.id }) { meal ->
                MealCard(meal = meal)
            }
        }
    }
}

@Composable
private fun MealCard(meal: MealEntry) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(
                text = meal.description,
                style = MaterialTheme.typography.titleMedium,
            )
            Text(
                text = "${meal.calories} kcal",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.primary,
            )
            Text(
                text = formatTimestamp(meal.timestamp),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

private fun formatTimestamp(instant: Instant): String {
    val formatter = DateTimeFormatter.ofPattern("MMM dd, yyyy 'at' hh:mm a")
        .withZone(ZoneId.systemDefault())
    return formatter.format(instant)
}

@Preview(showBackground = true)
@Composable
private fun EmptyContentPreview() {
    FoodieTheme {
        SampleScreenContent(
            state = SampleState(),
            onLoadDataClick = {},
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun LoadingContentPreview() {
    FoodieTheme {
        SampleScreenContent(
            state = SampleState(isLoading = true),
            onLoadDataClick = {},
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun MealListPreview() {
    FoodieTheme {
        val sampleMeals = listOf(
            MealEntry(
                id = "1",
                timestamp = Instant.now(),
                description = "Chicken salad with avocado",
                calories = 450,
            ),
            MealEntry(
                id = "2",
                timestamp = Instant.now().minusSeconds(3600),
                description = "Protein shake",
                calories = 250,
            ),
            MealEntry(
                id = "3",
                timestamp = Instant.now().minusSeconds(7200),
                description = "Oatmeal with berries",
                calories = 350,
            ),
        )
        SampleScreenContent(
            state = SampleState(meals = sampleMeals),
            onLoadDataClick = {},
        )
    }
}
