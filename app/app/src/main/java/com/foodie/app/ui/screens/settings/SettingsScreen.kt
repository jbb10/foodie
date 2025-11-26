package com.foodie.app.ui.screens.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.foodie.app.R
import com.foodie.app.domain.model.TestConnectionResult
import com.foodie.app.domain.model.UserProfile
import com.foodie.app.ui.theme.FoodieTheme
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter

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
    modifier: Modifier = Modifier,
) {
    val state by viewModel.state.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val focusManager = LocalFocusManager.current

    // Local state for editing (before save)
    var editedApiKey by remember { mutableStateOf("") }
    var editedEndpoint by remember { mutableStateOf("") }
    var editedModel by remember { mutableStateOf("") }

    // Initialize and handle side effects
    InitializeEditStateFromViewModel(state, editedApiKey) { key, endpoint, model ->
        editedApiKey = key
        editedEndpoint = endpoint
        editedModel = model
    }

    HandleSnackbarMessages(state, snackbarHostState, viewModel)

    SettingsScaffold(
        onNavigateBack = onNavigateBack,
        snackbarHostState = snackbarHostState,
        modifier = modifier,
    ) { paddingValues ->
        val apiConfigState = ApiConfigEditState(
            editedApiKey = editedApiKey,
            editedEndpoint = editedEndpoint,
            editedModel = editedModel,
            onApiKeyChange = { editedApiKey = it },
            onEndpointChange = { editedEndpoint = it },
            onModelChange = { editedModel = it },
        )

        SettingsContent(
            state = state,
            apiConfigState = apiConfigState,
            onSaveClick = {
                focusManager.clearFocus()
                viewModel.saveApiConfiguration(editedApiKey, editedEndpoint, editedModel)
            },
            onTestClick = {
                focusManager.clearFocus()
                viewModel.testConnection(
                    apiKey = state.apiKey,
                    endpoint = state.apiEndpoint,
                    modelName = state.modelName,
                )
            },
            onThemeChange = { viewModel.updateThemeMode(it) },
            onSexChanged = { viewModel.onSexChanged(it) },
            onBirthDateChanged = { viewModel.onBirthDateChanged(it) },
            onWeightChanged = { viewModel.onWeightChanged(it) },
            onHeightChanged = { viewModel.onHeightChanged(it) },
            onSaveProfile = { viewModel.saveUserProfile() },
            paddingValues = paddingValues,
        )
    }
}

/**
 * Data class to group API configuration editing state and reduce parameter count
 */
private data class ApiConfigEditState(
    val editedApiKey: String,
    val editedEndpoint: String,
    val editedModel: String,
    val onApiKeyChange: (String) -> Unit,
    val onEndpointChange: (String) -> Unit,
    val onModelChange: (String) -> Unit,
)

@Composable
private fun InitializeEditStateFromViewModel(
    state: SettingsState,
    editedApiKey: String,
    onInitialize: (String, String, String) -> Unit,
) {
    LaunchedEffect(state.apiKey, state.apiEndpoint, state.modelName) {
        if (editedApiKey.isEmpty()) {
            onInitialize(state.apiKey, state.apiEndpoint, state.modelName)
        }
    }
}

@Composable
private fun HandleSnackbarMessages(
    state: SettingsState,
    snackbarHostState: SnackbarHostState,
    viewModel: SettingsViewModel,
) {
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
                duration = SnackbarDuration.Short,
            )
            viewModel.clearSaveSuccess()
        }
    }

    // Show errors
    state.error?.let { error ->
        LaunchedEffect(error) {
            snackbarHostState.showSnackbar(
                message = error,
                duration = SnackbarDuration.Long,
            )
            viewModel.clearError()
        }
    }

    // Story 6.1: Show profile validation errors
    state.profileValidationError?.let { error ->
        LaunchedEffect(error) {
            snackbarHostState.showSnackbar(
                message = error,
                duration = SnackbarDuration.Long,
            )
        }
    }

    // Story 6.1: Show profile save success
    LaunchedEffect(state.profileSaveSuccess) {
        if (state.profileSaveSuccess) {
            snackbarHostState.showSnackbar(
                message = "Profile updated",
                duration = SnackbarDuration.Short,
            )
            viewModel.clearProfileSaveSuccess()
        }
    }

    // Story 6.1: Show Health Connect permission error
    LaunchedEffect(state.showProfilePermissionError) {
        if (state.showProfilePermissionError) {
            snackbarHostState.showSnackbar(
                message = "Grant Health Connect permissions to save weight and height",
                actionLabel = "OK",
                duration = SnackbarDuration.Long,
            )
            viewModel.clearProfilePermissionError()
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SettingsScaffold(
    onNavigateBack: () -> Unit,
    snackbarHostState: SnackbarHostState,
    modifier: Modifier = Modifier,
    content: @Composable (androidx.compose.foundation.layout.PaddingValues) -> Unit,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                        )
                    }
                },
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        modifier = modifier,
        content = content,
    )
}

@Composable
private fun SettingsContent(
    state: SettingsState,
    apiConfigState: ApiConfigEditState,
    onSaveClick: () -> Unit,
    onTestClick: () -> Unit,
    onThemeChange: (com.foodie.app.domain.model.ThemeMode) -> Unit,
    onSexChanged: (UserProfile.Sex) -> Unit,
    onBirthDateChanged: (LocalDate) -> Unit,
    onWeightChanged: (String) -> Unit,
    onHeightChanged: (String) -> Unit,
    onSaveProfile: () -> Unit,
    paddingValues: androidx.compose.foundation.layout.PaddingValues,
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(paddingValues),
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
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            )
        }
        item(key = "api_key") {
            ApiKeyPreference(
                value = apiConfigState.editedApiKey,
                onValueChange = apiConfigState.onApiKeyChange,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            )
        }
        item(key = "api_endpoint") {
            EditTextPreference(
                title = "Azure OpenAI Endpoint",
                value = apiConfigState.editedEndpoint,
                onValueChange = apiConfigState.onEndpointChange,
                hint = "https://your-resource.openai.azure.com",
                keyboardType = KeyboardType.Uri,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            )
        }
        item(key = "api_model") {
            EditTextPreference(
                title = "Model Deployment Name",
                value = apiConfigState.editedModel,
                onValueChange = apiConfigState.onModelChange,
                hint = "gpt-4.1",
                keyboardType = KeyboardType.Text,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            )
        }
        item(key = "api_model_description") {
            Text(
                text = stringResource(R.string.settings_model_description),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
            )
        }
        item(key = "api_save_button") {
            Button(
                onClick = onSaveClick,
                enabled = !state.isLoading && !state.isTestingConnection,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
            ) {
                if (state.isLoading) {
                    CircularProgressIndicator(
                        color = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.padding(end = 8.dp),
                    )
                }
                Text("Save Configuration")
            }
        }
        item(key = "api_test_button") {
            Button(
                onClick = onTestClick,
                enabled = !state.isLoading && !state.isTestingConnection,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
            ) {
                if (state.isTestingConnection) {
                    CircularProgressIndicator(
                        color = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.size(20.dp),
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                }
                Text("Test Connection")
            }
        }
        item(key = "api_divider") {
            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
        }

        // User Profile category (Story 6.1)
        item(key = "user_profile_header") {
            PreferenceCategoryHeader(title = "User Profile")
        }
        item(key = "user_profile_help_text") {
            Text(
                text = "Configure your demographic profile for accurate BMR calculation",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            )
        }
        item(key = "user_profile_sex") {
            SexPreference(
                value = state.editableSex,
                onValueChange = onSexChanged,
            )
        }
        item(key = "user_profile_birthdate") {
            BirthDatePreference(
                value = state.editableBirthDate,
                onValueChange = onBirthDateChanged,
            )
        }
        item(key = "user_profile_weight") {
            OutlinedTextField(
                value = state.editableWeight,
                onValueChange = onWeightChanged,
                label = { Text("Weight (kg)") },
                placeholder = { Text("e.g., 75.5") },
                supportingText = {
                    Text(
                        if (state.weightSourcedFromHC && !state.isEditingProfile) {
                            "Synced from Health Connect"
                        } else if (state.isEditingProfile && !state.weightSourcedFromHC) {
                            "Will sync to Health Connect"
                        } else {
                            "Used for BMR calculation"
                        },
                    )
                },
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Decimal,
                    imeAction = ImeAction.Next,
                ),
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
            )
        }
        item(key = "user_profile_height") {
            OutlinedTextField(
                value = state.editableHeight,
                onValueChange = onHeightChanged,
                label = { Text("Height (cm)") },
                placeholder = { Text("e.g., 178") },
                supportingText = {
                    Text(
                        if (state.heightSourcedFromHC && !state.isEditingProfile) {
                            "Synced from Health Connect"
                        } else if (state.isEditingProfile && !state.heightSourcedFromHC) {
                            "Will sync to Health Connect"
                        } else {
                            "Used for BMR calculation"
                        },
                    )
                },
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Decimal,
                    imeAction = ImeAction.Done,
                ),
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
            )
        }
        item(key = "user_profile_save_button") {
            Button(
                onClick = onSaveProfile,
                enabled = state.isEditingProfile && !state.isLoading,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
            ) {
                Text("Save Profile")
            }
        }
        item(key = "user_profile_divider") {
            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
        }

        // Appearance category
        item(key = "appearance_header") {
            PreferenceCategoryHeader(title = "Appearance")
        }
        item(key = "appearance_theme") {
            ThemePreference(
                currentTheme = state.themeMode,
                onThemeSelected = { theme ->
                    val themeMode = when (theme) {
                        "light" -> com.foodie.app.domain.model.ThemeMode.LIGHT
                        "dark" -> com.foodie.app.domain.model.ThemeMode.DARK
                        else -> com.foodie.app.domain.model.ThemeMode.SYSTEM_DEFAULT
                    }
                    onThemeChange(themeMode)
                },
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            )
        }
        item(key = "appearance_theme_description") {
            Text(
                text = stringResource(R.string.settings_theme_description),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
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
                summary = "1.0.0 (MVP)",
            )
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
    modifier: Modifier = Modifier,
) {
    Text(
        text = title,
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.primary,
        modifier = modifier.padding(horizontal = 16.dp, vertical = 12.dp),
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
    modifier: Modifier = Modifier,
) {
    androidx.compose.material3.ListItem(
        headlineContent = { Text(title) },
        supportingContent = { Text(summary) },
        modifier = modifier.semantics {
            contentDescription = "$title, $summary"
        },
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
    modifier: Modifier = Modifier,
) {
    val displayValue = if (value.isNotBlank() && value.length > 4) {
        "••••${value.takeLast(4)}"
    } else {
        value
    }

    Column(modifier = modifier) {
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            label = { Text("Azure OpenAI API Key") },
            placeholder = { Text("sk-...") },
            visualTransformation = PasswordVisualTransformation(),
            supportingText = {
                if (value.isNotBlank()) {
                    Text("Configured: $displayValue")
                }
            },
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Password,
                imeAction = ImeAction.Next,
            ),
            singleLine = true,
            modifier = Modifier
                .fillMaxWidth()
                .semantics { contentDescription = "api_key_field" },
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
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier) {
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            label = { Text(title) },
            placeholder = { Text(hint) },
            keyboardOptions = KeyboardOptions(
                keyboardType = keyboardType,
                imeAction = ImeAction.Next,
            ),
            singleLine = true,
            modifier = Modifier
                .fillMaxWidth()
                .semantics { contentDescription = "${title.lowercase().replace(" ", "_")}_field" },
        )
    }
}

/**
 * Theme preference with radio button list for theme selection.
 *
 * Displays three options: System Default, Light, Dark.
 * User selection triggers theme change immediately.
 *
 * @param currentTheme Current theme value ("system", "light", or "dark")
 * @param onThemeSelected Callback when theme is selected
 * @param modifier Optional modifier
 *
 * Story 5.4: Dark Mode Support (AC-7)
 */
@Composable
private fun ThemePreference(
    currentTheme: String,
    onThemeSelected: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier) {
        Text(
            text = "Theme",
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(bottom = 8.dp),
        )

        val themeOptions = listOf(
            "system" to stringResource(R.string.theme_system_default),
            "light" to stringResource(R.string.theme_light),
            "dark" to stringResource(R.string.theme_dark),
        )

        themeOptions.forEach { (value, label) ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onThemeSelected(value) }
                    .semantics {
                        contentDescription = if (currentTheme == value) {
                            "$label, selected"
                        } else {
                            "$label, not selected"
                        }
                    }
                    .padding(vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                RadioButton(
                    selected = currentTheme == value,
                    onClick = { onThemeSelected(value) },
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = label,
                    style = MaterialTheme.typography.bodyLarge,
                )
            }
        }
    }
}

/**
 * Sex preference with dialog for selection.
 *
 * Displays current selection or "Not set" if null.
 * Tapping opens AlertDialog with Male/Female radio buttons.
 *
 * Story 6.1: User Profile Settings (AC-4)
 *
 * @param value Currently selected sex (MALE/FEMALE) or null
 * @param onValueChange Callback when sex is selected
 * @param modifier Optional modifier
 */
@Composable
private fun SexPreference(
    value: UserProfile.Sex?,
    onValueChange: (UserProfile.Sex) -> Unit,
    modifier: Modifier = Modifier,
) {
    var showDialog by remember { mutableStateOf(false) }

    ListItem(
        headlineContent = { Text("Sex") },
        supportingContent = { Text("Used for BMR calculation") },
        trailingContent = {
            Text(
                text = value?.name?.lowercase()?.replaceFirstChar { it.uppercase() } ?: "Not set",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        },
        modifier = modifier.clickable { showDialog = true },
    )

    if (showDialog) {
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { showDialog = false },
            title = { Text("Select Sex") },
            text = {
                Column {
                    UserProfile.Sex.values().forEach { sex ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    onValueChange(sex)
                                    showDialog = false
                                }
                                .padding(vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            RadioButton(
                                selected = value == sex,
                                onClick = {
                                    onValueChange(sex)
                                    showDialog = false
                                },
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = sex.name.lowercase().replaceFirstChar { it.uppercase() },
                                style = MaterialTheme.typography.bodyLarge,
                            )
                        }
                    }
                }
            },
            confirmButton = {
                androidx.compose.material3.TextButton(onClick = { showDialog = false }) {
                    Text("Cancel")
                }
            },
        )
    }
}

/**
 * Story 6.1: User Profile Settings - Birthday Preference
 *
 * @param value Currently selected birth date or null
 * @param onValueChange Callback when birth date is selected
 * @param modifier Optional modifier
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun BirthDatePreference(
    value: LocalDate?,
    onValueChange: (LocalDate) -> Unit,
    modifier: Modifier = Modifier,
) {
    var showDialog by remember { mutableStateOf(false) }
    val datePickerState = rememberDatePickerState(
        initialSelectedDateMillis = value?.atStartOfDay(ZoneId.systemDefault())?.toInstant()?.toEpochMilli(),
    )

    // Calculate age from birth date
    val age = value?.let {
        val now = LocalDate.now()
        var years = now.year - it.year
        if (now.monthValue < it.monthValue || (now.monthValue == it.monthValue && now.dayOfMonth < it.dayOfMonth)) {
            years--
        }
        years
    }

    ListItem(
        headlineContent = { Text("Birthday") },
        supportingContent = {
            Text(
                if (value != null && age != null) {
                    "Age: $age years"
                } else {
                    "Required for BMR calculation"
                },
            )
        },
        trailingContent = {
            Text(
                text = value?.format(DateTimeFormatter.ofPattern("MMM d, yyyy")) ?: "Not set",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        },
        modifier = modifier.clickable { showDialog = true },
    )

    if (showDialog) {
        DatePickerDialog(
            onDismissRequest = { showDialog = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        datePickerState.selectedDateMillis?.let { millis ->
                            val localDate = Instant.ofEpochMilli(millis)
                                .atZone(ZoneId.systemDefault())
                                .toLocalDate()
                            onValueChange(localDate)
                        }
                        showDialog = false
                    },
                ) {
                    Text("OK")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDialog = false }) {
                    Text("Cancel")
                }
            },
        ) {
            DatePicker(state = datePickerState)
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun SettingsScreenPreview() {
    FoodieTheme {
        SettingsScreen(
            onNavigateBack = {},
        )
    }
}
