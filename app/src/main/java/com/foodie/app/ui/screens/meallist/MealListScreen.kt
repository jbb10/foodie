package com.foodie.app.ui.screens.meallist

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.foodie.app.R
import com.foodie.app.domain.model.MealEntry
import com.google.accompanist.swiperefresh.SwipeRefresh
import com.google.accompanist.swiperefresh.rememberSwipeRefreshState
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MealListScreen(
    viewModel: MealListViewModel = hiltViewModel(),
    onNavigateToDetail: (String) -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val swipeRefreshState = rememberSwipeRefreshState(isRefreshing = uiState.isRefreshing)

    Scaffold(
        topBar = {
            TopAppBar(title = { Text(stringResource(id = R.string.meal_history_title)) })
        }
    ) { paddingValues ->
        SwipeRefresh(
            state = swipeRefreshState,
            onRefresh = viewModel::refresh,
            modifier = Modifier.padding(paddingValues)
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                if (uiState.isLoading) {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                } else if (uiState.emptyStateVisible) {
                    Text(
                        text = stringResource(id = R.string.no_meals_logged),
                        style = MaterialTheme.typography.bodyLarge,
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp)
                    )
                } else if (uiState.error != null) {
                    Text(
                        text = uiState.error!!,
                        color = MaterialTheme.colorScheme.error,
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp)
                    )
                } else {
                    MealList(
                        mealsByDate = uiState.mealsByDate,
                        onItemClick = { onNavigateToDetail(it.id) }
                    )
                }
            }
        }
    }
}

@Composable
fun MealList(
    mealsByDate: Map<LocalDate, List<MealEntry>>,
    onItemClick: (MealEntry) -> Unit
) {
    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        mealsByDate.forEach { (date, meals) ->
            item {
                DateHeader(date = date)
            }
            items(meals) { meal ->
                MealEntryCard(meal = meal, onClick = { onItemClick(meal) })
            }
        }
    }
}

@Composable
fun DateHeader(date: LocalDate) {
    val today = LocalDate.now()
    val yesterday = today.minusDays(1)
    val headerText = when (date) {
        today -> stringResource(id = R.string.date_header_today)
        yesterday -> stringResource(id = R.string.date_header_yesterday)
        else -> date.format(DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM))
    }
    Text(
        text = headerText,
        style = MaterialTheme.typography.headlineSmall,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MealEntryCard(meal: MealEntry, onClick: () -> Unit) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(text = meal.name, style = MaterialTheme.typography.titleMedium)
            Text(
                text = stringResource(
                    id = R.string.calories_format,
                    meal.calories.toInt()
                ),
                style = MaterialTheme.typography.bodyMedium
            )
            Text(
                text = meal.timestamp.atZone(java.time.ZoneId.systemDefault())
                    .toLocalTime()
                    .format(DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT)),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
