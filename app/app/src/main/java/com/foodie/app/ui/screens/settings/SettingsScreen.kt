package com.foodie.app.ui.screens.settings

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
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
import com.foodie.app.ui.theme.FoodieTheme

/**
 * Settings screen for app configuration and preferences.
 *
 * Displays organized preference categories:
 * - API Configuration: Azure OpenAI endpoint, model selection (Stories 5.2, 5.3)
 * - Appearance: Theme mode, accessibility options (Stories 5.4, 5.5)
 * - About: App version, licenses
 *
 * Architecture:
 * - Stateless composable for optimal animation performance
 * - Compose LazyColumn for scrollable preference list
 * - Material Design 3 components for consistent styling
 * - No state observation for Story 5.1 (static placeholder list)
 *
 * Performance optimization:
 * - Removed ViewModel observation to prevent recomposition during navigation animations
 * - Future stories (5.2+) will add ViewModel when interactive preferences are needed
 * - LazyColumn keys ensure stable item identity during any future recomposition
 *
 * Current implementation (Story 5.1):
 * - Foundation with category headers and visual structure
 * - PreferencesRepository integration for persistence (will be used in Story 5.2+)
 * - Navigation integration with MealListScreen
 *
 * Future stories will add:
 * - Editable preference items (EditText, Switch, RadioButton)
 * - API key configuration with masked input (Story 5.2)
 * - Theme toggle (Story 5.4)
 * - Test connection button (Story 5.2)
 *
 * @param onNavigateBack Callback invoked when back navigation is requested
 * @param modifier Optional modifier for the screen
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings") },
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
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // API Configuration category
            item(key = "api_config_header") {
                PreferenceCategoryHeader(title = "API Configuration")
            }
            item(key = "api_endpoint") {
                PreferencePlaceholder(
                    title = "Azure OpenAI Endpoint",
                    summary = "Configure in Story 5.2"
                )
            }
            item(key = "api_model") {
                PreferencePlaceholder(
                    title = "Model Selection",
                    summary = "Configure in Story 5.3"
                )
            }
            item(key = "api_divider") {
                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
            }

            // Appearance category
            item(key = "appearance_header") {
                PreferenceCategoryHeader(title = "Appearance")
            }
            item(key = "appearance_theme") {
                PreferencePlaceholder(
                    title = "Theme",
                    summary = "Configure in Story 5.4"
                )
            }
            item(key = "appearance_accessibility") {
                PreferencePlaceholder(
                    title = "Accessibility",
                    summary = "Configure in Story 5.5"
                )
            }
            item(key = "appearance_divider") {
                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
            }

            // About category
            item(key = "about_header") {
                PreferenceCategoryHeader(title = "About")
            }
            item(key = "about_version") {
                PreferencePlaceholder(
                    title = "Version",
                    summary = "1.0.0 (MVP)"
                )
            }
        }
    }
}

/**
 * Category header for grouping related preferences.
 *
 * Uses Material Design 3 typography (labelLarge) for consistent styling.
 *
 * @param title Category name (e.g., "API Configuration")
 * @param modifier Optional modifier
 */
@Composable
private fun PreferenceCategoryHeader(
    title: String,
    modifier: Modifier = Modifier
) {
    Text(
        text = title,
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.primary,
        modifier = modifier.padding(horizontal = 16.dp, vertical = 12.dp)
    )
}

/**
 * Placeholder preference item.
 *
 * Used in Story 5.1 to show preference structure.
 * Future stories will replace with interactive preference components.
 *
 * @param title Preference name
 * @param summary Current value or description
 * @param modifier Optional modifier
 */
@Composable
private fun PreferencePlaceholder(
    title: String,
    summary: String,
    modifier: Modifier = Modifier
) {
    androidx.compose.material3.ListItem(
        headlineContent = { Text(title) },
        supportingContent = { Text(summary) },
        modifier = modifier
    )
}

@Preview(showBackground = true)
@Composable
private fun SettingsScreenPreview() {
    FoodieTheme {
        SettingsScreen(
            onNavigateBack = {}
        )
    }
}
