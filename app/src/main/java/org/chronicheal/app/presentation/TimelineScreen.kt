package org.chronicheal.app.presentation

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import org.chronicheal.app.domain.model.HealthEntry
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.time.format.TextStyle
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TimelineScreen(
    onAddEntryClick: () -> Unit,
    onCalendarClick: () -> Unit,
    onSettingsClick: () -> Unit,
    onAnalyticsClick: () -> Unit,
    onBodyScanClick: () -> Unit,
    viewModel: TimelineViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    val timelineItems = remember(uiState.entries) {
        buildTimelineItems(uiState.entries)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("ChronicHeal") },
                actions = {
                    IconButton(onClick = onBodyScanClick) {
                        Icon(Icons.Default.Accessibility, contentDescription = "Body Scan")
                    }
                    IconButton(onClick = onAnalyticsClick) {
                        Icon(Icons.Default.ShowChart, contentDescription = "Analytics")
                    }
                    IconButton(onClick = onCalendarClick) {
                        Icon(Icons.Default.CalendarMonth, contentDescription = "Calendar")
                    }
                    IconButton(onClick = onSettingsClick) {
                        Icon(Icons.Default.Settings, contentDescription = "Settings")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = onAddEntryClick) {
                Icon(Icons.Default.Add, contentDescription = "Add Entry")
            }
        }
    ) { innerPadding ->
        if (uiState.entries.isEmpty()) {
            Text(
                text = "No entries yet. Tap + to start tracking.",
                modifier = Modifier
                    .padding(innerPadding)
                    .padding(16.dp)
            )
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                items(timelineItems) { item ->
                    when (item) {
                        is TimelineItem.YearHeader -> YearHeader(item.year)
                        is TimelineItem.MonthHeader -> MonthHeader(item.month)
                        is TimelineItem.DayHeader -> DayHeader(item.day)
                        is TimelineItem.Entry -> EntryItem(
                            entry = item.entry,
                            onDeleteClick = { viewModel.deleteEntry(item.entry) }
                        )
                    }
                }
            }
        }
    }
}

sealed class TimelineItem {
    data class YearHeader(val year: Int) : TimelineItem()
    data class MonthHeader(val month: String) : TimelineItem()
    data class DayHeader(val day: String) : TimelineItem()
    data class Entry(val entry: HealthEntry) : TimelineItem()
}

fun buildTimelineItems(entries: List<HealthEntry>): List<TimelineItem> {
    val items = mutableListOf<TimelineItem>()
    var lastYear = -1
    var lastMonth = -1
    var lastDay = -1

    val zoneId = ZoneId.systemDefault()

    entries.forEach { entry ->
        val dateTime = entry.timestamp.atZone(zoneId)
        val year = dateTime.year
        val month = dateTime.monthValue
        val day = dateTime.dayOfMonth

        if (year != lastYear) {
            items.add(TimelineItem.YearHeader(year))
            lastYear = year
            lastMonth = -1
            lastDay = -1
        }
        if (month != lastMonth) {
            items.add(TimelineItem.MonthHeader(dateTime.month.getDisplayName(TextStyle.FULL, Locale.getDefault())))
            lastMonth = month
            lastDay = -1
        }
        if (day != lastDay) {
            items.add(TimelineItem.DayHeader(dateTime.format(DateTimeFormatter.ofPattern("EEEE d"))))
            lastDay = day
        }
        items.add(TimelineItem.Entry(entry))
    }
    return items
}

@Composable
fun YearHeader(year: Int) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 16.dp, bottom = 8.dp, start = 16.dp, end = 16.dp)
    ) {
        Text(
            text = year.toString(),
            style = MaterialTheme.typography.headlineLarge,
            fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
    }
}

@Composable
fun MonthHeader(month: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 8.dp, bottom = 4.dp, start = 16.dp, end = 16.dp)
    ) {
        Text(
            text = month,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.secondary
        )
    }
}

@Composable
fun DayHeader(day: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 4.dp, bottom = 4.dp, start = 16.dp, end = 16.dp)
    ) {
        Text(
            text = day,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = androidx.compose.ui.text.font.FontWeight.Medium,
            color = MaterialTheme.colorScheme.outline
        )
    }
}

@Composable
fun EntryItem(
    entry: HealthEntry,
    onDeleteClick: () -> Unit
) {
    val formatter = DateTimeFormatter
        .ofLocalizedTime(FormatStyle.SHORT)
        .withZone(ZoneId.systemDefault())

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = entry.type.name.replace("_", " ").lowercase()
                            .replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() },
                        style = MaterialTheme.typography.titleMedium
                    )
                    Text(
                        text = formatter.format(entry.timestamp),
                        style = MaterialTheme.typography.bodySmall
                    )
                }
                IconButton(onClick = onDeleteClick) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Delete Entry",
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            }
            
            if (entry.note.isNotEmpty()) {
                Text(
                    text = entry.note,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }
            entry.intensity?.let {
                Text(text = "Intensity: $it/10", style = MaterialTheme.typography.bodyMedium)
            }
            entry.name?.let {
                Text(text = "Name: $it", style = MaterialTheme.typography.bodyMedium)
            }
            entry.location?.let {
                Text(text = "Location: $it", style = MaterialTheme.typography.bodyMedium)
            }
            entry.value?.let {
                Text(text = "Value: $it ${entry.unit ?: ""}", style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}
