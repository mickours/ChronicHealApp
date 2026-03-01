package org.chronicheal.app.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ShowChart
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import org.chronicheal.app.domain.model.HealthEntry
import java.time.LocalDate
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
    onEntryClick: (HealthEntry) -> Unit,
    viewModel: TimelineViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val listState = rememberLazyListState()

    val timelineItems = remember(uiState.entries) {
        buildTimelineItems(uiState.entries)
    }

    val todayIndex = remember(timelineItems) {
        timelineItems.indexOfFirst { it is TimelineItem.DayHeader && it.isToday }
    }

    LaunchedEffect(todayIndex) {
        if (todayIndex != -1) {
            listState.scrollToItem(todayIndex)
        }
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
                        Icon(Icons.AutoMirrored.Filled.ShowChart, contentDescription = "Analytics")
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
                state = listState,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                itemsIndexed(timelineItems) { _, item ->
                    when (item) {
                        is TimelineItem.YearHeader -> YearHeader(item.year)
                        is TimelineItem.MonthHeader -> MonthHeader(item.month)
                        is TimelineItem.DayHeader -> DayHeader(item.day, item.isToday)
                        is TimelineItem.Entry -> EntryItem(
                            entry = item.entry,
                            onDeleteClick = { viewModel.deleteEntry(item.entry) },
                            modifier = Modifier.clickable { onEntryClick(item.entry) }
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
    data class DayHeader(val day: String, val isToday: Boolean = false) : TimelineItem()
    data class Entry(val entry: HealthEntry) : TimelineItem()
}

fun buildTimelineItems(entries: List<HealthEntry>): List<TimelineItem> {
    val items = mutableListOf<TimelineItem>()
    var lastYear = -1
    var lastMonth = -1
    var lastDay = -1

    val zoneId = ZoneId.systemDefault()
    val today = LocalDate.now()

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
            val isToday = dateTime.toLocalDate() == today
            items.add(TimelineItem.DayHeader(dateTime.format(DateTimeFormatter.ofPattern("EEEE d")), isToday))
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
            fontWeight = FontWeight.Bold,
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
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.secondary
        )
    }
}

@Composable
fun DayHeader(day: String, isToday: Boolean) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 4.dp, bottom = 4.dp, start = 16.dp, end = 16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = day,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = if (isToday) FontWeight.ExtraBold else FontWeight.Medium,
                color = if (isToday) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
            )
            if (isToday) {
                Spacer(Modifier.width(8.dp))
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .background(MaterialTheme.colorScheme.primaryContainer)
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = "TODAY",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }
        }
    }
}

@Composable
fun EntryItem(
    entry: HealthEntry,
    onDeleteClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val formatter = DateTimeFormatter
        .ofLocalizedTime(FormatStyle.SHORT)
        .withZone(ZoneId.systemDefault())

    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = entry.type.name.replace("_", " ").lowercase()
                                .replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() },
                            style = MaterialTheme.typography.titleMedium
                        )
                        if (entry.hasReminder) {
                            Spacer(Modifier.width(8.dp))
                            Icon(
                                imageVector = Icons.Default.NotificationsActive,
                                contentDescription = "Reminder set",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
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
