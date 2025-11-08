package com.foodie.app.ui.screens.mealdetail

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.foodie.app.ui.theme.FoodieTheme

/**
 * Meal detail/edit screen for viewing and editing a specific meal entry.
 *
 * This is a placeholder implementation for Epic 1 Story 1.3. Future stories will add:
 * - ViewModel with real data loading from Health Connect (Epic 3)
 * - Editable fields for meal description and calories
 * - Save functionality to update Health Connect entry
 * - Delete functionality
 *
 * Current implementation displays the meal ID for navigation testing purposes.
 *
 * @param mealId The unique identifier of the meal to display/edit
 * @param onNavigateBack Callback invoked when back navigation is requested
 * @param modifier Optional modifier for the screen
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MealDetailScreen(
    mealId: String,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Edit Meal") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                }
            )
        },
        modifier = modifier
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "Editing meal: $mealId",
                style = MaterialTheme.typography.bodyLarge
            )
            Text(
                text = "Meal editing functionality will be implemented in Epic 3",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun MealDetailScreenPreview() {
    FoodieTheme {
        MealDetailScreen(
            mealId = "sample-meal-123",
            onNavigateBack = {}
        )
    }
}
