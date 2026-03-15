package org.chronicheal.app.presentation

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ShowChart
import androidx.compose.material.icons.automirrored.filled.TrendingDown
import androidx.compose.material.icons.automirrored.filled.TrendingFlat
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AssignmentTurnedIn
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SearchOff
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import coil.compose.AsyncImage
import org.chronicheal.app.R
import org.chronicheal.app.domain.model.EntryType
import org.chronicheal.app.domain.model.HealthEntry
import org.chronicheal.app.presentation.navigation.Screen
import org.chronicheal.app.ui.theme.HeaderBlue
import org.chronicheal.app.ui.theme.PrimaryOrange
import java.time.LocalDate
import java.time.YearMonth
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
    onCompleteEntryClick: () -> Unit,
    onVoiceLoggingClick: () -> Unit,
    onEntryTypeClick: (EntryType) -> Unit,
    onEntryClick: (HealthEntry) -> Unit,
    viewModel: TimelineViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val listState = rememberLazyListState()
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current
    
    var isSearchVisible by rememberSaveable { mutableStateOf(false) }
    var hasPerformedInitialScroll by rememberSaveable { mutableStateOf(false) }

    val timelineItems = remember(uiState.entries, uiState.weeklyStats) {
        buildTimelineItems(uiState.entries, uiState.weeklyStats)
    }

    val todayIndex = remember(timelineItems) {
        timelineItems.indexOfFirst { it is TimelineItem.DayHeader && it.isToday }
    }

    LaunchedEffect(todayIndex) {
        if (!hasPerformedInitialScroll && todayIndex != -1) {
            listState.scrollToItem(todayIndex)
        }
    }

    // Observe messages from SavedStateHandle (e.g. "Edition canceled", "Entry updated")
    val savedStateHandle = navController.currentBackStackEntry?.savedStateHandle
    val navMessage = savedStateHandle?.getStateFlow<String?>("message", null)?.collectAsState()

    LaunchedEffect(navMessage?.value) {
        navMessage?.value?.let { msg ->
            // Localization fix: Map message keys to localized strings
            val localizedMsg = when(msg) {
                "Edition canceled" -> context.getString(R.string.edition_canceled)
                "Entry updated" -> context.getString(R.string.entry_updated)
                "Entry saved" -> context.getString(R.string.entry_saved)
                else -> msg
            }
            snackbarHostState.showSnackbar(localizedMsg)
            savedStateHandle.remove<String>("message")
        }
    }

    LaunchedEffect(uiState.message) {
        uiState.message?.let { msg ->
            val localizedMsg = when(msg) {
                "Entry deleted" -> context.getString(R.string.entry_deleted)
                "Entry updated" -> context.getString(R.string.entry_updated)
                "Entry added" -> context.getString(R.string.entry_saved)
                "Deletion undone" -> context.getString(R.string.undo_deletion)
                "Addition undone" -> context.getString(R.string.undo_addition)
                "Changes undone" -> context.getString(R.string.undo_changes)
                else -> msg
            }
            snackbarHostState.showSnackbar(localizedMsg)
            viewModel.clearMessage()
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            Column {
                TopAppBar(
                    title = {
                        AsyncImage(
                            model = "file:///android_asset/logo.svg",
                            contentDescription = "ChronicHeal Logo",
                            modifier = Modifier.size(40.dp)
                        )
                    },
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
                        IconButton(onClick = onCompleteEntryClick) {
                            Icon(Icons.Default.AssignmentTurnedIn, contentDescription = stringResource(R.string.complete_checkin))
                        }
                        IconButton(onClick = onAnalyticsClick) {
                            Icon(Icons.AutoMirrored.Filled.ShowChart, contentDescription = "Analytics")
                        }
                        IconButton(onClick = onCalendarClick) {
                            Icon(Icons.Default.CalendarMonth, contentDescription = "Calendar")
                        }
                        IconButton(onClick = onSettingsClick) {
                            Icon(Icons.Default.Settings, contentDescription = stringResource(R.string.action_settings))
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
                            placeholder = { Text(stringResource(R.string.search_placeholder)) },
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
                                unfocusedBorderColor = Color.Transparent,
                                focusedTextColor = Color.Black,
                                unfocusedTextColor = Color.Black,
                                focusedPlaceholderColor = Color.Gray,
                                unfocusedPlaceholderColor = Color.Gray,
                                focusedLeadingIconColor = Color.Black,
                                unfocusedLeadingIconColor = Color.Black,
                                focusedTrailingIconColor = Color.Black,
                                unfocusedTrailingIconColor = Color.Black
                            )
                        )
                        Spacer(Modifier.height(8.dp))
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            items(EntryType.entries) { type ->
                                val isSelected = type in uiState.selectedTypes
                                FilterChip(
                                    selected = isSelected,
                                    onClick = { viewModel.toggleTypeFilter(type) },
                                    label = { Text("${type.emoji} ${stringResource(type.displayRes)}") },
                                    leadingIcon = if (isSelected) {
                                        { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp)) }
                                    } else null,
                                    colors = FilterChipDefaults.filterChipColors(
                                        containerColor = Color.White.copy(alpha = 0.7f),
                                        labelColor = Color.Black,
                                        selectedContainerColor = Color.White,
                                        selectedLabelColor = Color.Black,
                                        selectedLeadingIconColor = Color.Black
                                    ),
                                    border = FilterChipDefaults.filterChipBorder(
                                        enabled = true,
                                        selected = isSelected,
                                        borderColor = Color.Transparent,
                                        selectedBorderColor = MaterialTheme.colorScheme.primary
                                    )
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
        },
        bottomBar = {
            val quickAddFavorites = uiState.favorites.filter { it != EntryType.VOICE_LOGGING }.toList()
            if (quickAddFavorites.isNotEmpty()) {
                QuickAddBar(
                    favorites = quickAddFavorites,
                    onEntryTypeClick = onEntryTypeClick
                )
            }
        }
    ) { innerPadding ->
        if (uiState.entries.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize().padding(innerPadding), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    AsyncImage(
                        model = "file:///android_asset/Body-dont-know.svg",
                        contentDescription = null,
                        modifier = Modifier.size(200.dp)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = if (uiState.searchQuery.isNotEmpty() || uiState.selectedTypes.isNotEmpty()) 
                            stringResource(R.string.no_matches) else stringResource(R.string.no_entries_yet),
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(horizontal = 32.dp)
                    )
                }
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
                        is TimelineItem.WeeklyInsights -> {
                            item(key = "weekly_insights") {
                                WeeklyInsightsCard(item.stats)
                            }
                        }
                        is TimelineItem.YearHeader -> {
                            item(key = "year_${item.year}_$index") {
                                YearHeader(item.year)
                            }
                        }
                        is TimelineItem.MonthHeader -> {
                            item(key = "month_${item.month}_$index") {
                                MonthHeader(
                                    month = item.month,
                                    yearMonth = item.yearMonth,
                                    onMonthClick = {
                                        // For "Day view for the month", we navigate to Calendar
                                        navController.navigate(Screen.Calendar.route)
                                    }
                                )
                            }
                        }
                        is TimelineItem.DayHeader -> {
                            stickyHeader(key = "day_${item.day}_$index") {
                                DayHeader(
                                    day = item.day,
                                    date = item.date,
                                    isToday = item.isToday,
                                    onDayClick = {
                                        navController.navigate(Screen.DayView.createRoute(item.date.toString()))
                                    }
                                )
                            }
                        }
                        is TimelineItem.Entry -> {
                            item(key = "entry_${item.entry.id}") {
                                EntryItem(
                                    entry = item.entry,
                                    modifier = Modifier.clickable { onEntryClick(item.entry) }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun QuickAddBar(
    favorites: List<EntryType>,
    onEntryTypeClick: (EntryType) -> Unit
) {
    Surface(
        tonalElevation = 8.dp,
        shadowElevation = 8.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .navigationBarsPadding()
                .padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Vertical "Quick Add" Label
            Box(
                modifier = Modifier
                    .padding(end = 8.dp)
                    .layout { measurable, constraints ->
                        val placeable = measurable.measure(constraints)
                        layout(placeable.height, placeable.width) {
                            placeable.placeRelative(
                                x = -(placeable.width / 2 - placeable.height / 2),
                                y = -(placeable.height / 2 - placeable.width / 2)
                            )
                        }
                    }
                    .rotate(-90f)
            ) {
                Text(
                    text = stringResource(R.string.quick_add),
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Black,
                    color = MaterialTheme.colorScheme.primary,
                    maxLines = 1
                )
            }

            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(end = 8.dp),
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically
            ) {
                items(favorites) { type ->
                    QuickAddChip(
                        type = type,
                        onClick = { onEntryTypeClick(type) }
                    )
                }
            }
        }
    }
}

@Composable
fun WeeklyInsightsCard(stats: WeeklyStats) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
        ),
        shape = RoundedCornerShape(24.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = stringResource(R.string.weekly_insights_title),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                InsightItem(
                    label = stringResource(R.string.avg_pain),
                    value = stats.avgPain?.let { String.format(Locale.getDefault(), "%.1f", it) } ?: "-",
                    trend = stats.painTrend,
                    isPositiveBetter = false,
                    modifier = Modifier.weight(1f)
                )
                InsightItem(
                    label = stringResource(R.string.medications),
                    value = stats.drugCount.toString(),
                    trend = stats.drugTrend,
                    isPositiveBetter = null, // Neutral
                    modifier = Modifier.weight(1f)
                )
                InsightItem(
                    label = stringResource(R.string.avg_sleep),
                    value = stats.avgSleepMinutes?.let { "${it / 60}h" } ?: "-",
                    trend = stats.sleepTrend,
                    isPositiveBetter = true,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
fun InsightItem(
    label: String,
    value: String,
    trend: Int?,
    isPositiveBetter: Boolean?,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(text = label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(text = value, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Black)
        
        if (trend != null && trend != 0) {
            val color = when {
                isPositiveBetter == null -> MaterialTheme.colorScheme.onSurfaceVariant
                isPositiveBetter && trend > 0 -> Color(0xFF388E3C) // Green
                !isPositiveBetter && trend < 0 -> Color(0xFF388E3C) // Green
                else -> Color(0xFFD32F2F) // Red
            }
            
            Row(verticalAlignment = Alignment.CenterVertically) {
                val icon = when {
                    trend > 0 -> Icons.AutoMirrored.Filled.TrendingUp
                    trend < 0 -> Icons.AutoMirrored.Filled.TrendingDown
                    else -> Icons.AutoMirrored.Filled.TrendingFlat
                }
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = color,
                    modifier = Modifier.size(12.dp)
                )
                Spacer(modifier = Modifier.width(2.dp))
                val isAvg = label.contains("Avg") || label.contains("moy")
                Text(
                    text = "${if (trend > 0) "+" else ""}$trend${if (isAvg) "%" else ""}",
                    style = MaterialTheme.typography.labelSmall,
                    color = color,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
fun QuickAddChip(
    type: EntryType,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .clickable { onClick() }
            .padding(4.dp)
    ) {
        @Suppress("DEPRECATION")
        Surface(
            shape = CircleShape,
            color = MaterialTheme.colorScheme.primaryContainer,
            modifier = Modifier.size(40.dp) // Reduced from 48.dp
        ) {
            Box(contentAlignment = Alignment.Center) {
                Text(text = type.emoji, fontSize = 20.sp) // Reduced from 24.sp
            }
        }
        Spacer(modifier = Modifier.height(2.dp)) // Reduced from 4.dp
        Text(
            text = stringResource(type.displayRes),
            style = MaterialTheme.typography.labelSmall,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            fontSize = 10.sp // Reduced from default
        )
    }
}

sealed class TimelineItem {
    data class WeeklyInsights(val stats: WeeklyStats) : TimelineItem()
    data class YearHeader(val year: Int) : TimelineItem()
    data class MonthHeader(val month: String, val yearMonth: YearMonth) : TimelineItem()
    data class DayHeader(val day: String, val date: LocalDate, val isToday: Boolean = false) : TimelineItem()
    data class Entry(val entry: HealthEntry) : TimelineItem()
}

fun buildTimelineItems(entries: List<HealthEntry>, stats: WeeklyStats?): List<TimelineItem> {
    val items = mutableListOf<TimelineItem>()
    val zoneId = ZoneId.systemDefault()
    val today = LocalDate.now()

    if (stats != null) {
        items.add(TimelineItem.WeeklyInsights(stats))
    }

    // Group entries by date
    val entriesByDate = entries.groupBy {
        it.timestamp.atZone(zoneId).toLocalDate()
    }

    // Include today in the set of dates to display, even if it has no entries
    val allDates = (entriesByDate.keys + today).sortedDescending()

    var lastYear = -1
    var lastMonth = -1

    val dayGroupFormatter = DateTimeFormatter.ofLocalizedDate(FormatStyle.FULL)

    allDates.forEach { date ->
        if (date.year != lastYear) {
            items.add(TimelineItem.YearHeader(date.year))
            lastYear = date.year
            lastMonth = -1
        }
        if (date.monthValue != lastMonth) {
            items.add(TimelineItem.MonthHeader(
                month = date.month.getDisplayName(TextStyle.FULL, Locale.getDefault()),
                yearMonth = YearMonth.from(date)
            ))
            lastMonth = date.monthValue
        }
        
        val isToday = date == today
        // Use localized date format for the header
        val fullDate = date.format(dayGroupFormatter)
        items.add(TimelineItem.DayHeader(fullDate, date, isToday))
        
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
fun MonthHeader(
    month: String, 
    yearMonth: YearMonth,
    onMonthClick: () -> Unit
) {
    Surface(
        color = MaterialTheme.colorScheme.secondaryContainer,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = month,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.secondary
            )
            IconButton(
                onClick = onMonthClick,
                modifier = Modifier.size(24.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Visibility,
                    contentDescription = "View Month",
                    tint = MaterialTheme.colorScheme.secondary,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}

@Composable
fun DayHeader(
    day: String, 
    date: LocalDate,
    isToday: Boolean,
    onDayClick: () -> Unit
) {
    Surface(
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 4.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
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
                            text = stringResource(R.string.today).uppercase(),
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }
            }
            IconButton(
                onClick = onDayClick,
                modifier = Modifier.size(24.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Visibility,
                    contentDescription = "View Day Details",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}

@Composable
fun EntryItem(
    entry: HealthEntry,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
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

    // Localize pain location if applicable
    val displayedLocation = remember(entry.location) {
        if (entry.type == EntryType.PAIN && entry.location != null) {
            formatId(context, entry.location)
        } else {
            entry.location
        }
    }

    val mainParameter = entry.name ?: entry.origin ?: displayedLocation ?: ""

    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 1.dp),
        shape = RectangleShape,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface,
            contentColor = MaterialTheme.colorScheme.onSurface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
    ) {
        Row(modifier = Modifier.height(IntrinsicSize.Min).defaultMinSize(minHeight = 80.dp)) {
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
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(
                                    text = "${entry.type.emoji} ${stringResource(entry.type.displayRes)}",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                
                                if (entry.hasReminder) {
                                    Icon(
                                        imageVector = Icons.Default.NotificationsActive,
                                        contentDescription = "Reminder set",
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                            
                            if (mainParameter.isNotEmpty()) {
                                Text(
                                    text = mainParameter,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Medium,
                                    color = MaterialTheme.colorScheme.primary,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    textAlign = TextAlign.End,
                                    modifier = Modifier.weight(1f, fill = false).padding(start = 8.dp)
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
                
                // If both name/origin and location exist, we already showed one in the title. 
                // Show the other one here if it wasn't the one highlighted.
                if ((entry.name != null || entry.origin != null) && !displayedLocation.isNullOrBlank() && displayedLocation != mainParameter) {
                    Text(
                        text = stringResource(R.string.location_label_format, displayedLocation),
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }

                // Value and Unit / Dosage / Quantity
                // For Drugs and Beverages, the quantity/dosage is stored in 'unit' and 'value' is often null.
                val displayValue = if (entry.value != null) {
                    stringResource(R.string.value_label, entry.value.toString(), entry.unit ?: "")
                } else if (!entry.unit.isNullOrBlank()) {
                    // For Drugs/Beverages, unit field contains the whole "500mg" or "250ml"
                    if (entry.type == EntryType.DRUG) {
                        stringResource(R.string.dosage_label_format, entry.unit)
                    } else if (entry.type == EntryType.BEVERAGE) {
                        stringResource(R.string.dosage_label_format, entry.unit).replace("Dosage", stringResource(R.string.quantity))
                    } else {
                        entry.unit
                    }
                } else {
                    null
                }

                displayValue?.let {
                    Text(
                        text = it,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }

                // Alcohol/Caffeine (for Beverages)
                if (entry.type == EntryType.BEVERAGE && (entry.isAlcoholic == true || entry.isCaffeinated == true)) {
                    val traits = mutableListOf<String>()
                    if (entry.isAlcoholic == true) traits.add(stringResource(R.string.is_alcoholic))
                    if (entry.isCaffeinated == true) traits.add(stringResource(R.string.is_caffeinated))
                    Text(
                        text = traits.joinToString(", "),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }

                // Duration
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
                            text = stringResource(R.string.duration_label) + ": $durationText", 
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }
                }

                // Allergens (for Meals)
                if (!entry.allergens.isNullOrEmpty()) {
                    Text(
                        text = stringResource(R.string.allergens) + ": " + entry.allergens.joinToString(", "),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }

                // Ingredients (for Meals)
                if (!entry.ingredients.isNullOrEmpty()) {
                    val ingredientsText = entry.ingredients.joinToString(", ") { ingredient ->
                        val qtyPart = ingredient.quantity?.let { 
                            val formattedQty = if (it == it.toLong().toDouble()) it.toLong().toString() else it.toString()
                            " ($formattedQty${ingredient.unit ?: ""})"
                        } ?: ""
                        "${ingredient.name}$qtyPart"
                    }
                    Text(
                        text = stringResource(R.string.ingredients_label) + ": $ingredientsText",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }

                if (entry.note.isNotEmpty()) {
                    Text(
                        text = entry.note,
                        modifier = Modifier.padding(top = 8.dp),
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }

            // Vertical intensity gauge on the right
            entry.intensity?.let { intensity ->
                val maxVal = 10
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
