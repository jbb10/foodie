package com.foodie.app.ui.screens.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.foodie.app.R
import com.foodie.app.domain.model.TestConnectionResult
import com.foodie.app.ui.theme.FoodieTheme

/**
 * Settings screen for app configuration and preferences.
 *
 * Displays organized preference categories:
 * - API Configuration: Azure OpenAI API key, endpoint, model (Stories 5.2, 5.3)
 * - Appearance: Theme mode, accessibility options (Stories 5.4, 5.5)
 * - About: App version, licenses
 *
 * Architecture:
 * - Observes SettingsViewModel for reactive state updates
 * - Compose LazyColumn for scrollable preference list
 * - Material Design 3 components for consistent styling
 * - SnackbarHost for test connection results and errors
 *
 * Story 5.2 Implementation:
 * - API key input with masked display (shows last 4 characters only)
 * - Endpoint and model configuration with validation
 * - Test connection button with loading states
 * - Clear error/success feedback via Snackbar
 *
 * @param onNavigateBack Callback invoked when back navigation is requested
 * @param viewModel SettingsViewModel instance (injected by Hilt)
 * @param modifier Optional modifier for the screen
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onNavigateBack: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel(),
    modifier: Modifier = Modifier
) {
    val state by viewModel.state.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val focusManager = LocalFocusManager.current

    // Local state for editing (before save)
    var editedApiKey by remember { mutableStateOf("") }
    var editedEndpoint by remember { mutableStateOf("") }
    var editedModel by remember { mutableStateOf("") }

    // Initialize local state from ViewModel state
    LaunchedEffect(state.apiKey, state.apiEndpoint, state.modelName) {
        if (editedApiKey.isEmpty()) editedApiKey = state.apiKey
        if (editedEndpoint.isEmpty()) editedEndpoint = state.apiEndpoint
        if (editedModel.isEmpty()) editedModel = state.modelName
    }

    // Show test connection result
    LaunchedEffect(state.testConnectionResult) {
        state.testConnectionResult?.let { result ->
            val message = when (result) {
                is TestConnectionResult.Success -> "API configuration valid ✓"
                is TestConnectionResult.Failure -> "Error: ${result.errorMessage}"
            }
            snackbarHostState.showSnackbar(message)
            viewModel.clearTestResult()
        }
    }

    // Show save success message
    state.saveSuccessMessage?.let { message ->
        LaunchedEffect(message) {
            snackbarHostState.showSnackbar(
                message = message,
                duration = SnackbarDuration.Short
            )
            viewModel.clearSaveSuccess()
        }
    }

    // Show errors
    state.error?.let { error ->
        LaunchedEffect(error) {
            snackbarHostState.showSnackbar(
                message = error,
                duration = SnackbarDuration.Long
            )
            viewModel.clearError()
        }
    }

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
        snackbarHost = { SnackbarHost(snackbarHostState) },
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
            item(key = "api_help_text") {
                Text(
                    text = stringResource(R.string.settings_api_help_text),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                )
            }
            item(key = "api_key") {
                ApiKeyPreference(
                    value = editedApiKey,
                    onValueChange = { editedApiKey = it },
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                )
            }
            item(key = "api_endpoint") {
                EditTextPreference(
                    title = "Azure OpenAI Endpoint",
                    value = editedEndpoint,
                    onValueChange = { editedEndpoint = it },
                    hint = "https://your-resource.openai.azure.com",
                    keyboardType = KeyboardType.Uri,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                )
            }
            item(key = "api_model") {
                EditTextPreference(
                    title = "Model Deployment Name",
                    value = editedModel,
                    onValueChange = { editedModel = it },
                    hint = "gpt-4.1",
                    keyboardType = KeyboardType.Text,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                )
            }
            item(key = "api_model_description") {
                Text(
                    text = stringResource(R.string.settings_model_description),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                )
            }
            item(key = "api_save_button") {
                Button(
                    onClick = {
                        focusManager.clearFocus()
                        viewModel.saveApiConfiguration(editedApiKey, editedEndpoint, editedModel)
                    },
                    enabled = !state.isLoading && !state.isTestingConnection,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    if (state.isLoading) {
                        CircularProgressIndicator(
                            color = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.padding(end = 8.dp)
                        )
                    }
                    Text("Save Configuration")
                }
            }
            item(key = "api_test_button") {
                Button(
                    onClick = {
                        focusManager.clearFocus()
                        viewModel.testConnection(
                            apiKey = state.apiKey,
                            endpoint = state.apiEndpoint,
                            modelName = state.modelName
                        )
                    },
                    enabled = !state.isLoading && !state.isTestingConnection,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    if (state.isTestingConnection) {
                        CircularProgressIndicator(
                            color = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                    }
                    Text("Test Connection")
                }
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

/**
 * API key preference with masked display.
 *
 * Displays last 4 characters only when configured (e.g., "••••1234").
 * Shows full input during editing with password transformation.
 *
 * Story 5.2: Azure OpenAI API Key Configuration
 *
 * @param value Current API key value
 * @param onValueChange Callback when value changes
 * @param modifier Optional modifier
 */
@Composable
private fun ApiKeyPreference(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val displayValue = if (value.isNotBlank() && value.length > 4) {
        "••••${value.takeLast(4)}"
    } else {
        value
    }

    Column(modifier = modifier) {
        Text(
            text = "API Key",
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(bottom = 4.dp)
        )
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            label = { Text("Azure OpenAI API Key") },
            placeholder = { Text("API Key") },
            visualTransformation = PasswordVisualTransformation(),
            supportingText = {
                if (value.isNotBlank()) {
                    Text("Configured: $displayValue")
                }
            },
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Password,
                imeAction = ImeAction.Next
            ),
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

/**
 * Editable text preference.
 *
 * Generic text input field for endpoint, model name, etc.
 *
 * Story 5.2: Azure OpenAI Endpoint and Model Configuration
 *
 * @param title Preference title
 * @param value Current value
 * @param onValueChange Callback when value changes
 * @param hint Placeholder text
 * @param keyboardType Keyboard type for input
 * @param modifier Optional modifier
 */
@Composable
private fun EditTextPreference(
    title: String,
    value: String,
    onValueChange: (String) -> Unit,
    hint: String,
    keyboardType: KeyboardType = KeyboardType.Text,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(bottom = 4.dp)
        )
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            label = { Text(title) },
            placeholder = { Text(hint) },
            keyboardOptions = KeyboardOptions(
                keyboardType = keyboardType,
                imeAction = ImeAction.Next
            ),
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )
    }
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
