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
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Today
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
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
    var selectedDate by rememberSaveable { mutableStateOf(LocalDate.now()) }
    var isExpanded by rememberSaveable { mutableStateOf(true) }
    
    val entriesByDate = remember(uiState.entries) {
        uiState.entries.groupBy {
            it.timestamp.atZone(ZoneId.systemDefault()).toLocalDate()
        }
    }

    val selectedDayEntries = remember(selectedDate, entriesByDate) {
        entriesByDate[selectedDate]?.sortedByDescending { it.intensity ?: 0 } ?: emptyList()
    }

    Scaffold(
        modifier = Modifier.systemBarsPadding(),
        topBar = {
            TopAppBar(
                title = { Text("Calendar") },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    TextButton(
                        onClick = { 
                            currentMonth.value = YearMonth.now()
                            selectedDate = LocalDate.now()
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
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            CalendarHeader(
                currentMonth = currentMonth.value,
                onMonthChange = { currentMonth.value = it }
            )
            
            CalendarGrid(
                currentMonth = currentMonth.value,
                entries = uiState.entries,
                selectedDate = selectedDate,
                onDateClick = { 
                    selectedDate = it
                    isExpanded = true
                }
            )

            Spacer(Modifier.height(16.dp))

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
                        Text(
                            text = selectedDate.format(DateTimeFormatter.ofPattern("EEEE, MMM dd")),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "${selectedDayEntries.size} entries",
                                style = MaterialTheme.typography.bodySmall,
                                modifier = Modifier.padding(end = 8.dp)
                            )
                            Icon(
                                if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                contentDescription = if (isExpanded) "Collapse" else "Expand"
                            )
                        }
                    }

                    AnimatedVisibility(
                        visible = isExpanded,
                        enter = fadeIn() + expandVertically(),
                        exit = fadeOut() + shrinkVertically()
                    ) {
                        if (selectedDayEntries.isEmpty()) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(32.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    "No entries for this day",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        } else {
                            LazyColumn(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(max = 400.dp),
                                contentPadding = PaddingValues(bottom = 16.dp)
                            ) {
                                items(selectedDayEntries) { entry ->
                                    DayEntryItem(
                                        entry = entry,
                                        onClick = { onDateClick(selectedDate) }
                                    )
                                }
                            }
                        }
                    }
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
    val time = entry.timestamp.atZone(ZoneId.systemDefault()).toLocalTime()

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
                    text = entry.name ?: entry.type.name.lowercase().capitalize(Locale.getDefault()),
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = time.format(timeFormatter),
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
    selectedDate: LocalDate,
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
                                
                                DayCell(
                                    day = day, 
                                    occurrenceIntensity = occurrenceIntensitySum,
                                    hasManagement = hasManagement,
                                    isToday = date == today,
                                    isSelected = date == selectedDate,
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
    onClick: () -> Unit
) {
    val isDark = isSystemInDarkTheme()
    val occColor = if (isDark) Color(0xFFFF5722) else Color(0xFFBF360C) 
    val mangColor = if (isDark) Color(0xFF00BCD4) else Color(0xFF006064) 

    Box(
        modifier = Modifier
            .aspectRatio(1f)
            .padding(2.dp)
            .clip(CircleShape)
            .background(
                when {
                    isSelected -> MaterialTheme.colorScheme.primaryContainer
                    isToday -> MaterialTheme.colorScheme.surfaceVariant
                    else -> Color.Transparent
                }
            )
            .border(
                width = if (isSelected) 2.dp else if (isToday) 1.dp else 0.dp,
                color = if (isSelected) MaterialTheme.colorScheme.primary else if (isToday) MaterialTheme.colorScheme.outline else Color.Transparent,
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
                color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
            )
            Row(
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                if (occurrenceIntensity > 0) {
                    val dotSize = min(20f, 10f + (occurrenceIntensity / 2f)).dp
                    CategoryDot(color = occColor, size = dotSize)
                }
                if (hasManagement) {
                    CategoryDot(color = mangColor, size = 10.dp)
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
