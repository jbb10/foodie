package com.foodie.app.ui.screens.meallist

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.foodie.app.domain.model.MealEntry
import com.foodie.app.ui.theme.FoodieTheme
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/**
 * Meal list screen (home screen) displaying all meal entries.
 *
 * This is a placeholder implementation for Epic 1 Story 1.3. Future stories will add:
 * - ViewModel with real data loading from Health Connect (Epic 3)
 * - Pull-to-refresh for syncing data
 * - Search and filter capabilities
 * - Meal entry deletion
 *
 * Current implementation shows temporary hardcoded test data for UI validation.
 *
 * @param onMealClick Callback invoked when a meal is tapped (passes meal ID)
 * @param onSettingsClick Callback invoked when settings button is tapped
 * @param modifier Optional modifier for the screen
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MealListScreen(
    onMealClick: (String) -> Unit,
    onSettingsClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    // Temporary test data - will be replaced with ViewModel in Epic 3
    val testMeals = listOf(
        MealEntry(
            id = "meal-001",
            timestamp = Instant.now().minusSeconds(3600),
            description = "Grilled chicken with quinoa and vegetables",
            calories = 450
        ),
        MealEntry(
            id = "meal-002",
            timestamp = Instant.now().minusSeconds(7200),
            description = "Greek yogurt with berries and granola",
            calories = 280
        ),
        MealEntry(
            id = "meal-003",
            timestamp = Instant.now().minusSeconds(14400),
            description = "Salmon salad with avocado",
            calories = 520
        ),
        MealEntry(
            id = "meal-004",
            timestamp = Instant.now().minusSeconds(21600),
            description = "Oatmeal with banana and almonds",
            calories = 340
        )
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Foodie") },
                actions = {
                    IconButton(onClick = onSettingsClick) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = "Settings"
                        )
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { /* Future: Add meal entry */ }) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Add meal"
                )
            }
        },
        modifier = modifier
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(testMeals) { meal ->
                MealListItem(
                    meal = meal,
                    onClick = { onMealClick(meal.id) }
                )
            }
        }
    }
}

/**
 * Individual meal item card in the list.
 *
 * Displays meal description, timestamp, and calories in a tappable card.
 *
 * @param meal The meal entry to display
 * @param onClick Callback invoked when the card is tapped
 */
@Composable
private fun MealListItem(
    meal: MealEntry,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = meal.description,
                style = MaterialTheme.typography.bodyLarge
            )
            Text(
                text = formatTimestamp(meal.timestamp),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = "${meal.calories} kcal",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}

/**
 * Formats a timestamp for display (e.g., "Today at 2:30 PM").
 *
 * @param timestamp The instant to format
 * @return Formatted timestamp string
 */
private fun formatTimestamp(timestamp: Instant): String {
    val formatter = DateTimeFormatter.ofPattern("MMM dd, yyyy 'at' h:mm a")
        .withZone(ZoneId.systemDefault())
    return formatter.format(timestamp)
}

@Preview(showBackground = true)
@Composable
private fun MealListScreenPreview() {
    FoodieTheme {
        MealListScreen(
            onMealClick = {},
            onSettingsClick = {}
        )
    }
}
