package org.chronicheal.app.presentation

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ShowChart
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import kotlinx.coroutines.launch
import org.chronicheal.app.domain.model.EntryType
import org.chronicheal.app.domain.model.HealthEntry
import org.chronicheal.app.ui.theme.*
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.time.format.TextStyle
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun TimelineScreen(
    navController: NavController,
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
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    
    var isSearchVisible by rememberSaveable { mutableStateOf(false) }
    var hasPerformedInitialScroll by rememberSaveable { mutableStateOf(false) }

    val timelineItems = remember(uiState.entries) {
        buildTimelineItems(uiState.entries)
    }

    val todayIndex = remember(timelineItems) {
        timelineItems.indexOfFirst { it is TimelineItem.DayHeader && it.isToday }
    }

    LaunchedEffect(todayIndex) {
        if (!hasPerformedInitialScroll && todayIndex != -1) {
            listState.scrollToItem(todayIndex)
            hasPerformedInitialScroll = true
        }
    }

    // Observe messages from SavedStateHandle (e.g. "Edition canceled", "Entry updated")
    val savedStateHandle = navController.currentBackStackEntry?.savedStateHandle
    val navMessage = savedStateHandle?.getStateFlow<String?>("message", null)?.collectAsState()

    LaunchedEffect(navMessage?.value) {
        navMessage?.value?.let { msg ->
            snackbarHostState.showSnackbar(msg)
            savedStateHandle?.remove<String>("message")
        }
    }

    LaunchedEffect(uiState.message) {
        uiState.message?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearMessage()
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            Column {
                TopAppBar(
                    title = { Text("ChronicHeal") },
                    actions = {
                        IconButton(onClick = { 
                            isSearchVisible = !isSearchVisible 
                            if (!isSearchVisible) {
                                viewModel.setSearchQuery("")
                            }
                        }) {
                            Icon(
                                imageVector = if (isSearchVisible) Icons.Default.SearchOff else Icons.Default.Search, 
                                contentDescription = "Toggle Search"
                            )
                        }
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
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = HeaderBlue,
                        titleContentColor = Color.Black,
                        actionIconContentColor = Color.Black
                    )
                )
                AnimatedVisibility(
                    visible = isSearchVisible,
                    enter = expandVertically() + fadeIn(),
                    exit = shrinkVertically() + fadeOut()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(HeaderBlue)
                            .padding(horizontal = 16.dp, vertical = 8.dp)
                    ) {
                        OutlinedTextField(
                            value = uiState.searchQuery,
                            onValueChange = viewModel::setSearchQuery,
                            modifier = Modifier.fillMaxWidth(),
                            placeholder = { Text("Search by name, location or note...") },
                            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                            trailingIcon = {
                                if (uiState.searchQuery.isNotEmpty()) {
                                    IconButton(onClick = { viewModel.setSearchQuery("") }) {
                                        Icon(Icons.Default.Clear, contentDescription = "Clear")
                                    }
                                }
                            },
                            shape = RoundedCornerShape(24.dp),
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedContainerColor = Color.White,
                                unfocusedContainerColor = Color.White,
                                focusedBorderColor = Color.Transparent,
                                unfocusedBorderColor = Color.Transparent
                            )
                        )
                        Spacer(Modifier.height(8.dp))
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            items(EntryType.entries) { type ->
                                FilterChip(
                                    selected = type in uiState.selectedTypes,
                                    onClick = { viewModel.toggleTypeFilter(type) },
                                    label = { Text("${type.emoji} ${type.name.lowercase().replaceFirstChar { it.uppercase() }}") },
                                    leadingIcon = if (type in uiState.selectedTypes) {
                                        { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp)) }
                                    } else null
                                )
                            }
                        }
                    }
                }
            }
        },
        floatingActionButton = {
            FloatingActionButton(onClick = onAddEntryClick) {
                Icon(Icons.Default.Add, contentDescription = "Add Entry")
            }
        }
    ) { innerPadding ->
        if (uiState.entries.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize().padding(innerPadding), contentAlignment = Alignment.Center) {
                Text(
                    text = if (uiState.searchQuery.isNotEmpty() || uiState.selectedTypes.isNotEmpty()) 
                        "No matches found." else "No entries yet. Tap + to start tracking.",
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                    modifier = Modifier.padding(32.dp)
                )
            }
        } else {
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                timelineItems.forEachIndexed { index, item ->
                    when (item) {
                        is TimelineItem.YearHeader -> {
                            item(key = "year_${item.year}_$index") {
                                YearHeader(item.year)
                            }
                        }
                        is TimelineItem.MonthHeader -> {
                            item(key = "month_${item.month}_$index") {
                                MonthHeader(item.month)
                            }
                        }
                        is TimelineItem.DayHeader -> {
                            stickyHeader(key = "day_${item.day}_$index") {
                                DayHeader(item.day, item.isToday)
                            }
                        }
                        is TimelineItem.Entry -> {
                            item(key = "entry_${item.entry.id}") {
                                SwipeableEntryItem(
                                    entry = item.entry,
                                    onDelete = {
                                        viewModel.deleteEntry(item.entry)
                                        scope.launch {
                                            val result = snackbarHostState.showSnackbar(
                                                message = "Entry deleted",
                                                actionLabel = "Undo",
                                                duration = SnackbarDuration.Short
                                            )
                                            if (result == SnackbarResult.ActionPerformed) {
                                                viewModel.restoreDeletedEntry()
                                            }
                                        }
                                    },
                                    onClick = { onEntryClick(item.entry) }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SwipeableEntryItem(
    entry: HealthEntry,
    onDelete: () -> Unit,
    onClick: () -> Unit
) {
    val dismissState = rememberSwipeToDismissBoxState(
        confirmValueChange = { value ->
            if (value == SwipeToDismissBoxValue.EndToStart) {
                onDelete()
                true
            } else {
                false
            }
        }
    )

    SwipeToDismissBox(
        state = dismissState,
        enableDismissFromStartToEnd = false,
        backgroundContent = {
            val color by animateColorAsState(
                if (dismissState.targetValue == SwipeToDismissBoxValue.EndToStart) {
                    MaterialTheme.colorScheme.errorContainer
                } else {
                    Color.Transparent
                }, label = "swipe_color"
            )
            val icon = if (dismissState.targetValue == SwipeToDismissBoxValue.EndToStart) Icons.Default.Delete else null

            Box(
                Modifier
                    .fillMaxSize()
                    .padding(horizontal = 8.dp, vertical = 4.dp)
                    .clip(RoundedCornerShape(24.dp))
                    .background(color)
                    .padding(horizontal = 20.dp),
                contentAlignment = Alignment.CenterEnd
            ) {
                if (icon != null) {
                    Icon(icon, contentDescription = null)
                }
            }
        },
        content = {
            EntryItem(
                entry = entry,
                modifier = Modifier.clickable { onClick() }
            )
        }
    )
}

sealed class TimelineItem {
    data class YearHeader(val year: Int) : TimelineItem()
    data class MonthHeader(val month: String) : TimelineItem()
    data class DayHeader(val day: String, val isToday: Boolean = false) : TimelineItem()
    data class Entry(val entry: HealthEntry) : TimelineItem()
}

fun buildTimelineItems(entries: List<HealthEntry>): List<TimelineItem> {
    val items = mutableListOf<TimelineItem>()
    val zoneId = ZoneId.systemDefault()
    val today = LocalDate.now()

    // Group entries by date
    val entriesByDate = entries.groupBy {
        it.timestamp.atZone(zoneId).toLocalDate()
    }

    // Include today in the set of dates to display, even if it has no entries
    val allDates = (entriesByDate.keys + today).sortedDescending()

    var lastYear = -1
    var lastMonth = -1

    allDates.forEach { date ->
        if (date.year != lastYear) {
            items.add(TimelineItem.YearHeader(date.year))
            lastYear = date.year
            lastMonth = -1
        }
        if (date.monthValue != lastMonth) {
            items.add(TimelineItem.MonthHeader(date.month.getDisplayName(TextStyle.FULL, Locale.getDefault())))
            lastMonth = date.monthValue
        }
        
        val isToday = date == today
        // Use full localized date format for the header
        val fullDate = date.format(DateTimeFormatter.ofLocalizedDate(FormatStyle.FULL))
        items.add(TimelineItem.DayHeader(fullDate, isToday))
        
        entriesByDate[date]?.forEach { entry ->
            items.add(TimelineItem.Entry(entry))
        }
    }
    return items
}

@Composable
fun YearHeader(year: Int) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant,
        modifier = Modifier.fillMaxWidth()
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            Text(
                text = year.toString(),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}

@Composable
fun MonthHeader(month: String) {
    Surface(
        color = MaterialTheme.colorScheme.secondaryContainer,
        modifier = Modifier.fillMaxWidth()
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 4.dp)
        ) {
            Text(
                text = month,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.secondary
            )
        }
    }
}

@Composable
fun DayHeader(day: String, isToday: Boolean) {
    Surface(
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 4.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = day,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = if (isToday) FontWeight.ExtraBold else FontWeight.Bold,
                    color = if (isToday) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
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
}

@Composable
fun EntryItem(
    entry: HealthEntry,
    modifier: Modifier = Modifier
) {
    val formatter = remember {
        DateTimeFormatter
            .ofLocalizedTime(FormatStyle.SHORT)
            .withZone(ZoneId.systemDefault())
    }

    val stripeColor = remember(entry.type.category) {
        when (entry.type.category) {
            EntryType.Category.OCCURRENCE -> PrimaryOrange
            EntryType.Category.MANAGEMENT -> HeaderBlue
        }
    }

    val mainParameter = entry.name ?: entry.location ?: ""

    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 4.dp),
        shape = RoundedCornerShape(24.dp), // Semi-stadium shape to reflect swipability
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface,
            contentColor = MaterialTheme.colorScheme.onSurface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(modifier = Modifier.height(IntrinsicSize.Min)) {
            // Category color stripe
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .width(8.dp)
                    .background(stripeColor)
            )
            
            Column(
                modifier = Modifier
                    .padding(16.dp)
                    .weight(1f)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = "${entry.type.emoji} ${entry.type.name.replace("_", " ").lowercase()
                                    .replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() }}",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            
                            if (mainParameter.isNotEmpty()) {
                                Text(
                                    text = "• $mainParameter",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Medium,
                                    color = MaterialTheme.colorScheme.primary,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.weight(1f, fill = false)
                                )
                            }

                            if (entry.hasReminder) {
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
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                
                if (entry.note.isNotEmpty()) {
                    Text(
                        text = entry.note,
                        modifier = Modifier.padding(top = 8.dp),
                        style = MaterialTheme.typography.bodyMedium
                    )
                }

                // If both name and location exist, we already showed one in the header. 
                // Show the other one here if it wasn't the one highlighted.
                if (entry.name != null && entry.location != null) {
                    Text(text = "Location: ${entry.location}", style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(top = 4.dp))
                }
                
                entry.value?.let {
                    Text(text = "Value: $it ${entry.unit ?: ""}", style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(top = 4.dp))
                }
                
                entry.durationMinutes?.let { duration ->
                    if (duration > 0) {
                        val hours = duration / 60
                        val mins = duration % 60
                        val durationText = when {
                            hours > 0 && mins > 0 -> "${hours}h ${mins}min"
                            hours > 0 -> "${hours}h"
                            else -> "${mins}min"
                        }
                        Text(
                            text = "Duration: $durationText", 
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }
                }
            }

            // Vertical intensity gauge on the right
            entry.intensity?.let { intensity ->
                val maxVal = if (entry.type == EntryType.SLEEP) 5 else 10
                VerticalIntensityGauge(
                    intensity = intensity,
                    maxVal = maxVal,
                    color = stripeColor,
                    label = if (entry.type == EntryType.SLEEP) "QUAL" else "INT"
                )
            }
        }
    }
}
