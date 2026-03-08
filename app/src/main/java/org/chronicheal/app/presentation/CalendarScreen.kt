package org.chronicheal.app.presentation

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SearchOff
import androidx.compose.material.icons.filled.Today
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import org.chronicheal.app.domain.model.EntryType
import org.chronicheal.app.domain.model.HealthEntry
import org.chronicheal.app.ui.theme.HeaderBlue
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale
import kotlin.math.min

data class AggregatedEntry(
    val type: EntryType,
    val name: String?,
    val count: Int,
    val totalDurationMinutes: Int?,
    val averageIntensity: Float?,
    val latestTimestamp: java.time.Instant
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CalendarScreen(
    onBackClick: () -> Unit,
    onDateClick: (LocalDate) -> Unit,
    onManageRemindersClick: () -> Unit,
    viewModel: TimelineViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val currentMonth = remember { mutableStateOf(YearMonth.now()) }
    var startDate by rememberSaveable { mutableStateOf<LocalDate?>(LocalDate.now()) }
    var endDate by rememberSaveable { mutableStateOf<LocalDate?>(null) }
    var isExpanded by rememberSaveable { mutableStateOf(true) }
    var isSearchVisible by rememberSaveable { mutableStateOf(false) }
    var showAggregated by rememberSaveable { mutableStateOf(false) }
    
    val selectedRangeEntries = remember(startDate, endDate, uiState.entries) {
        if (startDate == null) emptyList()
        else {
            val start = startDate!!
            val end = endDate ?: start
            val actualStart = if (start.isBefore(end)) start else end
            val actualEnd = if (start.isBefore(end)) end else start
            
            uiState.entries.filter {
                val entryDate = it.timestamp.atZone(ZoneId.systemDefault()).toLocalDate()
                (entryDate == actualStart || entryDate == actualEnd || (entryDate.isAfter(actualStart) && entryDate.isBefore(actualEnd)))
            }.sortedByDescending { it.timestamp }
        }
    }

    val aggregatedEntries = remember(selectedRangeEntries) {
        selectedRangeEntries.groupBy { "${it.type.name}_${it.name ?: ""}" }
            .map { (_, group) ->
                val first = group.first()
                AggregatedEntry(
                    type = first.type,
                    name = first.name,
                    count = group.size,
                    totalDurationMinutes = group.sumOf { it.durationMinutes ?: 0 }.takeIf { it > 0 },
                    averageIntensity = group.mapNotNull { it.intensity }.takeIf { it.isNotEmpty() }?.average()?.toFloat(),
                    latestTimestamp = group.maxOf { it.timestamp }
                )
            }.sortedByDescending { it.latestTimestamp }
    }

    Scaffold(
        modifier = Modifier.systemBarsPadding(),
        topBar = {
            Column {
                TopAppBar(
                    title = { Text("Calendar") },
                    navigationIcon = {
                        IconButton(onClick = onBackClick) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                        }
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
                        TextButton(
                            onClick = { 
                                currentMonth.value = YearMonth.now()
                                startDate = LocalDate.now()
                                endDate = null
                            },
                            colors = ButtonDefaults.textButtonColors(contentColor = Color.Black)
                        ) {
                            Icon(Icons.Default.Today, contentDescription = null)
                            Spacer(Modifier.width(4.dp))
                            Text("Today")
                        }
                        IconButton(onClick = onManageRemindersClick) {
                            Icon(Icons.Default.Notifications, contentDescription = "Manage Reminders")
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = HeaderBlue,
                        titleContentColor = Color.Black,
                        navigationIconContentColor = Color.Black,
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
                                    label = { Text("${type.emoji} ${type.name.lowercase().replaceFirstChar { it.uppercase() }}") },
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
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            item {
                CalendarHeader(
                    currentMonth = currentMonth.value,
                    onMonthChange = { currentMonth.value = it }
                )
            }
            
            item {
                CalendarGrid(
                    currentMonth = currentMonth.value,
                    entries = uiState.entries,
                    startDate = startDate,
                    endDate = endDate,
                    onDateClick = { date ->
                        if (startDate == null || (startDate != null && endDate != null)) {
                            startDate = date
                            endDate = null
                        } else if (date.isBefore(startDate)) {
                            endDate = startDate
                            startDate = date
                        } else if (date.isAfter(startDate)) {
                            endDate = date
                        } else {
                            // Clicking the same date as startDate when endDate is null
                            startDate = null
                        }
                        isExpanded = true
                    }
                )
            }

            item {
                Spacer(Modifier.height(16.dp))
            }

            item {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                    shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
                ) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { isExpanded = !isExpanded }
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            val rangeText = remember(startDate, endDate) {
                                when {
                                    startDate != null && endDate != null -> {
                                        "${startDate!!.format(DateTimeFormatter.ofPattern("MMM dd"))} - ${endDate!!.format(DateTimeFormatter.ofPattern("MMM dd"))}"
                                    }
                                    startDate != null -> {
                                        startDate!!.format(DateTimeFormatter.ofPattern("EEEE, MMM dd"))
                                    }
                                    else -> "Select a date"
                                }
                            }
                            Column {
                                Text(
                                    text = rangeText,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                if (startDate != null && endDate != null) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Checkbox(
                                            checked = showAggregated,
                                            onCheckedChange = { checked -> showAggregated = checked },
                                            modifier = Modifier.size(24.dp)
                                        )
                                        Spacer(Modifier.width(8.dp))
                                        Text("Show Aggregated View", style = MaterialTheme.typography.labelMedium)
                                    }
                                }
                            }
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "${selectedRangeEntries.size} entries",
                                    style = MaterialTheme.typography.bodySmall,
                                    modifier = Modifier.padding(end = 8.dp)
                                )
                                Icon(
                                    if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                    contentDescription = if (isExpanded) "Collapse" else "Expand"
                                )
                            }
                        }
                    }
                }
            }

            if (isExpanded) {
                if (selectedRangeEntries.isEmpty()) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
                                .padding(32.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                "No entries for this period",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                } else if (showAggregated && startDate != null && endDate != null) {
                    items(aggregatedEntries) { entry ->
                        Box(modifier = Modifier.background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))) {
                            AggregatedEntryItem(entry = entry)
                        }
                    }
                } else {
                    items(selectedRangeEntries) { entry ->
                        Box(modifier = Modifier.background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))) {
                            DayEntryItem(
                                entry = entry,
                                onClick = { onDateClick(entry.timestamp.atZone(ZoneId.systemDefault()).toLocalDate()) }
                            )
                        }
                    }
                    item {
                        Spacer(modifier = Modifier.height(16.dp).background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)))
                    }
                }
            }
        }
    }
}

@Composable
fun AggregatedEntryItem(entry: AggregatedEntry) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .padding(12.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = entry.type.emoji,
                style = MaterialTheme.typography.headlineSmall,
                modifier = Modifier.padding(end = 12.dp)
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = entry.name ?: entry.type.name.lowercase().replaceFirstChar { it.uppercase() },
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Occurred ${entry.count} times",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                entry.totalDurationMinutes?.let { duration ->
                    val hours = duration / 60
                    val mins = duration % 60
                    val durationText = when {
                        hours > 0 && mins > 0 -> "${hours}h ${mins}min"
                        hours > 0 -> "${hours}h"
                        else -> "${mins}min"
                    }
                    Text(
                        text = "Total duration: $durationText",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            if (entry.averageIntensity != null) {
                Surface(
                    color = getIntensityColor(entry.averageIntensity.toInt()).copy(alpha = 0.1f),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        text = "Avg: ${String.format("%.1f", entry.averageIntensity)}/10",
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = getIntensityColor(entry.averageIntensity.toInt())
                    )
                }
            }
        }
    }
}

@Composable
fun DayEntryItem(
    entry: HealthEntry,
    onClick: () -> Unit
) {
    val timeFormatter = remember { DateTimeFormatter.ofPattern("HH:mm") }
    val dateFormatter = remember { DateTimeFormatter.ofPattern("MMM dd") }
    val dateTime = entry.timestamp.atZone(ZoneId.systemDefault())

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp)
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .padding(12.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = entry.type.emoji,
                style = MaterialTheme.typography.headlineSmall,
                modifier = Modifier.padding(end = 12.dp)
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = entry.name ?: entry.type.name.lowercase().replaceFirstChar { it.uppercase() },
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "${dateTime.toLocalDate().format(dateFormatter)} at ${dateTime.toLocalTime().format(timeFormatter)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            if (entry.intensity != null) {
                Surface(
                    color = getIntensityColor(entry.intensity).copy(alpha = 0.1f),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        text = "${entry.intensity}/10",
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = getIntensityColor(entry.intensity)
                    )
                }
            }
        }
    }
}

@Composable
fun getIntensityColor(intensity: Int): Color {
    return when {
        intensity >= 8 -> Color(0xFFD32F2F)
        intensity >= 5 -> Color(0xFFF57C00)
        else -> Color(0xFF388E3C)
    }
}

@Composable
fun CalendarHeader(
    currentMonth: YearMonth,
    onMonthChange: (YearMonth) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        IconButton(onClick = { onMonthChange(currentMonth.minusMonths(1)) }) {
            Icon(Icons.Default.ChevronLeft, contentDescription = "Previous Month")
        }
        Text(
            text = "${currentMonth.month.getDisplayName(TextStyle.FULL, Locale.getDefault())} ${currentMonth.year}",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
        )
        IconButton(onClick = { onMonthChange(currentMonth.plusMonths(1)) }) {
            Icon(Icons.Default.ChevronRight, contentDescription = "Next Month")
        }
    }
}

@Composable
fun CalendarGrid(
    currentMonth: YearMonth,
    entries: List<HealthEntry>,
    startDate: LocalDate?,
    endDate: LocalDate?,
    onDateClick: (LocalDate) -> Unit
) {
    val daysInMonth = currentMonth.lengthOfMonth()
    val firstDayOfMonth = currentMonth.atDay(1).dayOfWeek.value % 7 
    val days = (1..daysInMonth).toList()
    val emptyDaysBefore = (0 until firstDayOfMonth).toList()
    val today = LocalDate.now()

    val entriesByDate = remember(entries) {
        entries.groupBy {
            it.timestamp.atZone(ZoneId.systemDefault()).toLocalDate()
        }
    }

    Column(modifier = Modifier.padding(8.dp)) {
        Row(modifier = Modifier.fillMaxWidth()) {
            val dayNames = listOf("Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat")
            dayNames.forEach { day ->
                Text(
                    text = day,
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        val rows = (days.size + emptyDaysBefore.size + 6) / 7
        for (row in 0 until rows) {
            Row(modifier = Modifier.fillMaxWidth()) {
                for (col in 0 until 7) {
                    val index = row * 7 + col
                    Box(modifier = Modifier.weight(1f)) {
                        if (index < emptyDaysBefore.size) {
                            Box(modifier = Modifier.aspectRatio(1f))
                        } else {
                            val dayIndex = index - emptyDaysBefore.size
                            if (dayIndex < days.size) {
                                val day = days[dayIndex]
                                val date = currentMonth.atDay(day)
                                val dayEntries = entriesByDate[date] ?: emptyList()
                                
                                val occurrenceIntensitySum = dayEntries
                                    .filter { it.type.category == EntryType.Category.OCCURRENCE }
                                    .sumOf { it.intensity ?: 0 }
                                
                                val hasManagement = dayEntries.any { it.type.category == EntryType.Category.MANAGEMENT }
                                
                                val isSelected = when {
                                    startDate != null && endDate != null -> {
                                        date == startDate || date == endDate || (date.isAfter(startDate) && date.isBefore(endDate))
                                    }
                                    startDate != null -> date == startDate
                                    else -> false
                                }

                                DayCell(
                                    day = day, 
                                    occurrenceIntensity = occurrenceIntensitySum,
                                    hasManagement = hasManagement,
                                    isToday = date == today,
                                    isSelected = isSelected,
                                    isStart = date == startDate,
                                    isEnd = date == endDate,
                                    onClick = { onDateClick(date) }
                                )
                            } else {
                                Box(modifier = Modifier.aspectRatio(1f))
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun DayCell(
    day: Int, 
    occurrenceIntensity: Int,
    hasManagement: Boolean,
    isToday: Boolean,
    isSelected: Boolean,
    isStart: Boolean,
    isEnd: Boolean,
    onClick: () -> Unit
) {
    val isDark = isSystemInDarkTheme()
    val occColor = if (isDark) Color(0xFFFF5722) else Color(0xFFBF360C) 
    val mangColor = if (isDark) Color(0xFF00BCD4) else Color(0xFF006064) 

    val backgroundColor = when {
        isStart || isEnd -> MaterialTheme.colorScheme.primary
        isSelected -> MaterialTheme.colorScheme.primaryContainer
        isToday -> MaterialTheme.colorScheme.surfaceVariant
        else -> Color.Transparent
    }

    val contentColor = when {
        isStart || isEnd -> MaterialTheme.colorScheme.onPrimary
        isSelected -> MaterialTheme.colorScheme.onPrimaryContainer
        else -> MaterialTheme.colorScheme.onSurface
    }

    Box(
        modifier = Modifier
            .aspectRatio(1f)
            .padding(2.dp)
            .clip(CircleShape)
            .background(backgroundColor)
            .border(
                width = if (isToday && !isSelected) 1.dp else 0.dp,
                color = if (isToday && !isSelected) MaterialTheme.colorScheme.outline else Color.Transparent,
                shape = CircleShape
            )
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.fillMaxSize()
        ) {
            Text(
                text = day.toString(),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = if (isSelected || isToday) FontWeight.ExtraBold else FontWeight.Bold,
                color = contentColor
            )
            Row(
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                if (occurrenceIntensity > 0) {
                    val dotSize = min(20f, 10f + (occurrenceIntensity / 2f)).dp
                    CategoryDot(color = if (isStart || isEnd) Color.White else occColor, size = dotSize)
                }
                if (hasManagement) {
                    CategoryDot(color = if (isStart || isEnd) Color.White else mangColor, size = 10.dp)
                }
            }
        }
    }
}

@Composable
fun CategoryDot(color: Color, size: Dp = 10.dp) {
    Box(
        modifier = Modifier
            .padding(horizontal = 1.dp)
            .size(size)
            .background(color, shape = CircleShape)
            .border(1.2.dp, Color.White.copy(alpha = 0.5f), CircleShape) 
    )
}
