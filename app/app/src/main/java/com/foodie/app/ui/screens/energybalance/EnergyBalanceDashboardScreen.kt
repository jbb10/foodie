package com.foodie.app.ui.screens.energybalance

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.foodie.app.data.local.healthconnect.HealthConnectManager
import com.foodie.app.domain.model.EnergyBalance
import com.foodie.app.ui.theme.FoodieTheme
import timber.log.Timber
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

/**
 * Energy Balance Dashboard screen showing daily calories in vs calories out.
 *
 * Displays comprehensive energy balance tracking:
 * - Hero card: Deficit/surplus with color coding (green deficit, red surplus)
 * - Daily summary: Calories In (from meals), Calories Out (TDEE)
 * - TDEE breakdown: BMR + NEAT + Active formula
 * - Pull-to-refresh for manual Health Connect sync
 * - Empty state when no meals logged
 * - Error state with retry or open settings action
 *
 * **Real-Time Updates (AC #7):**
 * UI automatically updates when:
 * - Meal logged via capture flow → Calories In increases
 * - Steps sync from Health Connect → NEAT updates
 * - Workout syncs from Garmin → Active updates
 * - User profile changes → BMR recalculates
 *
 * **Color Coding (AC #4, #5):**
 * - Deficit (TDEE > Calories In): Green card, "-X kcal deficit"
 * - Surplus (TDEE < Calories In): Red card, "+X kcal surplus"
 *
 * Architecture:
 * - Stateful screen: Collects StateFlow from ViewModel
 * - Stateless content: EnergyBalanceContent renders data only
 * - MVVM pattern: ViewModel manages state, UI observes
 *
 * @param healthConnectManager HealthConnectManager for permission requests
 * @param onNavigateToSettings Callback to navigate to Settings screen (for ProfileNotConfiguredError)
 * @param viewModel EnergyBalanceDashboardViewModel instance (Hilt-injected)
 * @param modifier Optional modifier for the screen
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EnergyBalanceDashboardScreen(
    healthConnectManager: HealthConnectManager,
    onNavigateToSettings: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: EnergyBalanceDashboardViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsState()

    // Register Health Connect permission launcher
    val requestHealthConnectPermissions = rememberLauncherForActivityResult(
        healthConnectManager.createPermissionRequestContract(),
    ) { grantedPermissions ->
        Timber.i("Health Connect permission result: granted=${grantedPermissions.size}, required=${HealthConnectManager.REQUIRED_PERMISSIONS.size}")
        viewModel.onRetryAfterError()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Energy Balance") },
            )
        },
        modifier = modifier,
    ) { paddingValues ->
        PullToRefreshBox(
            isRefreshing = state.isLoading,
            onRefresh = viewModel::refresh,
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
        ) {
            when {
                state.error != null -> {
                    // Error state - request permissions if showSettingsButton is true
                    ErrorState(
                        errorMessage = state.error ?: "Unknown error",
                        showSettingsButton = state.showSettingsButton,
                        onRetry = {
                            // Request permissions if this is a permission error (showSettingsButton = true)
                            if (state.showSettingsButton) {
                                requestHealthConnectPermissions.launch(HealthConnectManager.REQUIRED_PERMISSIONS)
                            } else {
                                viewModel.onRetryAfterError()
                            }
                        },
                        onOpenSettings = onNavigateToSettings,
                    )
                }
                state.energyBalance == null && !state.isLoading -> {
                    // Empty state (AC #8)
                    Column {
                        DateNavigationRow(
                            selectedDate = state.selectedDate,
                            onPreviousDay = viewModel::onPreviousDay,
                            onNextDay = viewModel::onNextDay,
                            onTodayClicked = viewModel::onTodayClicked,
                        )
                        EmptyState(
                            isHistoricalDate = state.selectedDate < LocalDate.now(),
                        )
                    }
                }
                state.energyBalance != null -> {
                    // Success state - show dashboard
                    Column {
                        DateNavigationRow(
                            selectedDate = state.selectedDate,
                            onPreviousDay = viewModel::onPreviousDay,
                            onNextDay = viewModel::onNextDay,
                            onTodayClicked = viewModel::onTodayClicked,
                        )
                        EnergyBalanceContent(
                            energyBalance = state.energyBalance!!,
                            lastUpdated = state.lastUpdated,
                        )
                    }
                }
                else -> {
                    // Initial loading state
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center,
                    ) {
                        CircularProgressIndicator()
                    }
                }
            }
        }
    }
}

/**
 * Stateless Energy Balance Dashboard content.
 *
 * Renders complete dashboard with all cards:
 * - Last updated timestamp
 * - Deficit/surplus hero card (AC #3, #4, #5)
 * - Calories summary card (AC #1, #2)
 * - TDEE breakdown card
 *
 * @param energyBalance Complete energy balance data to display
 * @param lastUpdated Timestamp of last successful data fetch (null if first load)
 */
@Composable
internal fun EnergyBalanceContent(
    energyBalance: EnergyBalance,
    lastUpdated: Instant?,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        // Last updated timestamp
        lastUpdated?.let {
            val minutesAgo = Duration.between(it, Instant.now()).toMinutes()
            val timeText = when {
                minutesAgo < 1 -> "just now"
                minutesAgo == 1L -> "1 minute ago"
                minutesAgo < 60 -> "$minutesAgo minutes ago"
                else -> {
                    val hours = minutesAgo / 60
                    if (hours == 1L) "1 hour ago" else "$hours hours ago"
                }
            }
            Text(
                text = "Last updated: $timeText",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        // Hero card: Deficit or Surplus (AC #3, #4, #5)
        DeficitSurplusCard(
            deficitSurplus = energyBalance.deficitSurplus,
            isDeficit = energyBalance.isDeficit,
            formattedDeficitSurplus = energyBalance.formattedDeficitSurplus,
        )

        // Calories summary: In vs Out (AC #1, #2)
        CaloriesSummaryCard(
            caloriesIn = energyBalance.caloriesIn,
            caloriesOut = energyBalance.tdee,
        )

        // TDEE breakdown: BMR + NEAT + Active
        TDEEBreakdownCard(
            bmr = energyBalance.bmr,
            neat = energyBalance.neat,
            activeCalories = energyBalance.activeCalories,
            tdee = energyBalance.tdee,
        )
    }
}

/**
 * Deficit/Surplus hero card with color coding (AC #3, #4, #5).
 *
 * **Color Scheme:**
 * - Deficit (positive deficitSurplus): Green primaryContainer
 * - Surplus (negative deficitSurplus): Red errorContainer
 *
 * **Text Format:**
 * - Deficit: "-500 kcal deficit"
 * - Surplus: "+200 kcal surplus"
 *
 * @param deficitSurplus Raw deficit/surplus value in kcal (positive = deficit, negative = surplus)
 * @param isDeficit True if in deficit (burning more than consuming)
 * @param formattedDeficitSurplus Human-readable string from EnergyBalance.formattedDeficitSurplus
 */
@Composable
internal fun DeficitSurplusCard(
    deficitSurplus: Double,
    isDeficit: Boolean,
    formattedDeficitSurplus: String,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (isDeficit) {
                MaterialTheme.colorScheme.primaryContainer
            } else {
                MaterialTheme.colorScheme.errorContainer
            },
        ),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = if (isDeficit) "Caloric Deficit" else "Caloric Surplus",
                style = MaterialTheme.typography.titleMedium,
                color = if (isDeficit) {
                    MaterialTheme.colorScheme.onPrimaryContainer
                } else {
                    MaterialTheme.colorScheme.onErrorContainer
                },
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = formattedDeficitSurplus,
                style = MaterialTheme.typography.displayMedium,
                fontWeight = FontWeight.Bold,
                color = if (isDeficit) {
                    MaterialTheme.colorScheme.onPrimaryContainer
                } else {
                    MaterialTheme.colorScheme.onErrorContainer
                },
            )
        }
    }
}

/**
 * Calories summary card showing Calories In and Calories Out (AC #1, #2).
 *
 * Displays:
 * - Calories In: X kcal (from today's NutritionRecords) with Restaurant icon
 * - Calories Out: Y kcal (TDEE) with DirectionsRun icon
 *
 * @param caloriesIn Total calories consumed today in kcal
 * @param caloriesOut Total daily energy expenditure (TDEE) in kcal
 */
@Composable
internal fun CaloriesSummaryCard(
    caloriesIn: Double,
    caloriesOut: Double,
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = "Daily Summary",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
            )

            // Calories In row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "Calories In:",
                        style = MaterialTheme.typography.bodyLarge,
                    )
                }
                Text(
                    text = "${caloriesIn.toInt()} kcal",
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Bold,
                )
            }

            // Calories Out row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "Calories Out:",
                        style = MaterialTheme.typography.bodyLarge,
                    )
                }
                Text(
                    text = "${caloriesOut.toInt()} kcal",
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Bold,
                )
            }
        }
    }
}

/**
 * TDEE breakdown card showing formula: BMR + NEAT + Active = TDEE.
 *
 * Displays each TDEE component with icon:
 * - BMR (Basal Metabolic Rate): Hotel icon
 * - Passive (NEAT): DirectionsWalk icon
 * - Active: FitnessCenter icon
 * - Formula: "BMR + Passive + Active = TDEE"
 *
 * @param bmr Basal Metabolic Rate in kcal/day
 * @param neat Non-Exercise Activity Thermogenesis in kcal/day
 * @param activeCalories Active exercise calories in kcal/day
 * @param tdee Total Daily Energy Expenditure in kcal/day
 */
@Composable
internal fun TDEEBreakdownCard(
    bmr: Double,
    neat: Double,
    activeCalories: Double,
    tdee: Double,
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = "TDEE Breakdown",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
            )

            // BMR row
            BreakdownRow(
                label = "BMR:",
                value = bmr.toInt(),
            )

            // Passive (NEAT) row
            BreakdownRow(
                label = "Passive:",
                value = neat.toInt(),
            )

            // Active row
            BreakdownRow(
                label = "Active:",
                value = activeCalories.toInt(),
            )

            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

            // Formula
            Text(
                text = "BMR + Passive + Active = ${tdee.toInt()} kcal",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

/**
 * Single breakdown row showing icon, label, and value.
 *
 * @param icon Material icon to display
 * @param label Text label (e.g., "BMR:", "Passive:")
 * @param value Calorie value in kcal (integer)
 */
@Composable
private fun BreakdownRow(
    label: String,
    value: Int,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge,
        )
        Text(
            text = "$value kcal",
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.Bold,
        )
    }
}

/**
 * Empty state displayed when no meals logged today (AC #8).
 *
 * Shows SsidChart icon with "Log your first meal to start tracking" message.
 */
/**
 * Date navigation row with previous/next day buttons and date label.
 *
 * **Layout:**
 * - Left: Previous Day button (arrow left)
 * - Center: Date label ("Today", "Yesterday", or formatted date)
 * - Right: Next Day button (arrow right, disabled if viewing today) + "Today" button (visible if not today)
 *
 * **Date Formatting:**
 * - Today: "Today"
 * - Yesterday: "Yesterday"
 * - Other dates: "Wednesday, Nov 27, 2025" (medium format)
 *
 * @param selectedDate Currently selected date
 * @param onPreviousDay Callback when Previous Day button tapped
 * @param onNextDay Callback when Next Day button tapped
 * @param onTodayClicked Callback when "Today" button tapped
 */
@Composable
internal fun DateNavigationRow(
    selectedDate: LocalDate,
    onPreviousDay: () -> Unit,
    onNextDay: () -> Unit,
    onTodayClicked: () -> Unit,
) {
    val today = LocalDate.now()
    val yesterday = today.minusDays(1)

    // Format date label
    val dateLabel = when (selectedDate) {
        today -> "Today"
        yesterday -> "Yesterday"
        else -> selectedDate.format(DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM))
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // Previous Day button (always enabled)
        IconButton(
            onClick = onPreviousDay,
            modifier = Modifier.testTag("previous_day_button"),
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "Previous Day",
            )
        }

        // Date label
        Text(
            text = dateLabel,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Medium,
        )

        // Right side: Next Day + Today buttons
        Row(
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // Next Day button (disabled if viewing today)
            IconButton(
                onClick = onNextDay,
                enabled = selectedDate < today,
                modifier = Modifier.testTag("next_day_button"),
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                    contentDescription = "Next Day",
                )
            }

            // "Today" button (visible only if not viewing today)
            if (selectedDate != today) {
                TextButton(onClick = onTodayClicked) {
                    Text("Today")
                }
            }
        }
    }
}

/**
 * Empty state displayed when no meals are logged.
 *
 * Shows different message based on whether viewing historical date or today:
 * - Historical date: "No meals logged on this day"
 * - Today: "Log your first meal to start tracking"
 *
 * @param isHistoricalDate True if viewing a historical date (not today)
 */
@Composable
internal fun EmptyState(
    isHistoricalDate: Boolean = false,
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Icon(
                imageVector = Icons.Default.Info,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(64.dp),
            )
            Text(
                text = if (isHistoricalDate) {
                    "No meals logged on this day"
                } else {
                    "Log your first meal to start tracking"
                },
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
        }
    }
}

/**
 * Error state displayed when data fetch fails.
 *
 * Shows ErrorOutline icon, error message, and retry button.
 * If showSettingsButton=true (ProfileNotConfiguredError), also shows "Open Settings" button.
 *
 * @param errorMessage Error message to display
 * @param showSettingsButton True to show "Open Settings" button (for ProfileNotConfiguredError)
 * @param onRetry Callback when retry button clicked
 * @param onOpenSettings Callback when open settings button clicked
 */
@Composable
internal fun ErrorState(
    errorMessage: String,
    showSettingsButton: Boolean,
    onRetry: () -> Unit,
    onOpenSettings: () -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Icon(
                imageVector = Icons.Default.Warning,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.error,
                modifier = Modifier.size(64.dp),
            )
            Text(
                text = errorMessage,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )

            Button(onClick = onRetry) {
                Text(if (showSettingsButton) "Grant Access" else "Retry")
            }

            if (showSettingsButton) {
                Button(onClick = onOpenSettings) {
                    Text("Open Settings")
                }
            }
        }
    }
}

// ========== Previews ==========

@Preview(showBackground = true)
@Composable
private fun DeficitCardPreview() {
    FoodieTheme {
        DeficitSurplusCard(
            deficitSurplus = 500.0,
            isDeficit = true,
            formattedDeficitSurplus = "-500 kcal deficit",
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun SurplusCardPreview() {
    FoodieTheme {
        DeficitSurplusCard(
            deficitSurplus = -200.0,
            isDeficit = false,
            formattedDeficitSurplus = "+200 kcal surplus",
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun CaloriesSummaryCardPreview() {
    FoodieTheme {
        CaloriesSummaryCard(
            caloriesIn = 1800.0,
            caloriesOut = 2500.0,
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun TDEEBreakdownCardPreview() {
    FoodieTheme {
        TDEEBreakdownCard(
            bmr = 1650.0,
            neat = 400.0,
            activeCalories = 450.0,
            tdee = 2500.0,
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun EmptyStatePreview() {
    FoodieTheme {
        EmptyState()
    }
}

@Preview(showBackground = true)
@Composable
private fun ErrorStatePreview() {
    FoodieTheme {
        ErrorState(
            errorMessage = "Failed to load energy balance data",
            showSettingsButton = false,
            onRetry = {},
            onOpenSettings = {},
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun ErrorStateWithSettingsPreview() {
    FoodieTheme {
        ErrorState(
            errorMessage = "User profile not configured",
            showSettingsButton = true,
            onRetry = {},
            onOpenSettings = {},
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun EnergyBalanceContentPreview() {
    FoodieTheme {
        EnergyBalanceContent(
            energyBalance = EnergyBalance(
                bmr = 1650.0,
                neat = 400.0,
                activeCalories = 450.0,
                tdee = 2500.0,
                caloriesIn = 2000.0,
                deficitSurplus = 500.0,
            ),
            lastUpdated = Instant.now().minusSeconds(300), // 5 minutes ago
        )
    }
}
