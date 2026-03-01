package org.chronicheal.app.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Today
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import org.chronicheal.app.domain.model.HealthEntry
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import java.time.format.TextStyle
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CalendarScreen(
    onBackClick: () -> Unit,
    onDateClick: (LocalDate) -> Unit,
    onManageRemindersClick: () -> Unit,
    viewModel: TimelineViewModel = hiltViewModel(),
    remindersViewModel: RemindersViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val reminders by remindersViewModel.reminders.collectAsState()
    val currentMonth = remember { mutableStateOf(YearMonth.now()) }

    Scaffold(
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
                        onClick = { currentMonth.value = YearMonth.now() },
                        colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.primary)
                    ) {
                        Icon(Icons.Default.Today, contentDescription = null)
                        Spacer(Modifier.width(4.dp))
                        Text("Today")
                    }
                    IconButton(onClick = onManageRemindersClick) {
                        Icon(Icons.Default.Notifications, contentDescription = "Manage Reminders")
                    }
                }
            )
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
                    onDateClick = onDateClick
                )
            }
            
            if (reminders.isNotEmpty()) {
                item {
                    Text(
                        text = "Daily Reminders",
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.padding(16.dp)
                    )
                }
                
                items(reminders.filter { it.isEnabled }) { reminder ->
                    ReminderItemSmall(
                        reminder = reminder,
                        onToggle = { remindersViewModel.toggleReminder(reminder) }
                    )
                }
            }
        }
    }
}

@Composable
fun ReminderItemSmall(
    reminder: org.chronicheal.app.domain.model.Reminder,
    onToggle: () -> Unit
) {
    val timeFormatter = java.time.format.DateTimeFormatter.ofPattern("HH:mm")
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        )
    ) {
        Row(
            modifier = Modifier
                .padding(12.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(text = reminder.title, style = MaterialTheme.typography.bodyLarge)
                Text(
                    text = "${reminder.time.format(timeFormatter)} - ${formatDaysOfWeek(reminder.daysOfWeek)}",
                    style = MaterialTheme.typography.bodySmall
                )
            }
            Switch(
                checked = reminder.isEnabled,
                onCheckedChange = { onToggle() },
                modifier = Modifier.scale(0.8f)
            )
        }
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
    onDateClick: (LocalDate) -> Unit
) {
    val daysInMonth = currentMonth.lengthOfMonth()
    val firstDayOfMonth = currentMonth.atDay(1).dayOfWeek.value % 7 // 0 for Sunday, 1 for Monday...
    val days = (1..daysInMonth).toList()
    val emptyDaysBefore = (0 until firstDayOfMonth).toList()
    val today = LocalDate.now()

    val entriesByDate = entries.groupBy {
        it.timestamp.atZone(ZoneId.systemDefault()).toLocalDate()
    }

    Column(modifier = Modifier.padding(8.dp)) {
        // Day names
        Row(modifier = Modifier.fillMaxWidth()) {
            val dayNames = listOf("Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat")
            dayNames.forEach { day ->
                Text(
                    text = day,
                    modifier = Modifier.weight(1f),
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        // We use a non-scrollable grid inside the LazyColumn
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
                                val hasEntries = entriesByDate.containsKey(date)
                                val isToday = date == today
                                DayCell(
                                    day = day, 
                                    hasEntries = hasEntries,
                                    isToday = isToday,
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
    hasEntries: Boolean,
    isToday: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .aspectRatio(1f)
            .padding(4.dp)
            .clip(CircleShape)
            .background(
                if (isToday) MaterialTheme.colorScheme.primaryContainer else Color.Transparent
            )
            .then(
                if (isToday) {
                    Modifier.border(1.dp, MaterialTheme.colorScheme.primary, CircleShape)
                } else {
                    Modifier
                }
            )
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = day.toString(),
                fontWeight = if (isToday) FontWeight.Bold else FontWeight.Normal,
                color = if (isToday) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
            )
            if (hasEntries) {
                Box(
                    modifier = Modifier
                        .size(4.dp)
                        .background(
                            if (isToday) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.primary, 
                            shape = CircleShape
                        )
                )
            }
        }
    }
}

private fun formatDaysOfWeek(days: Set<Int>): String {
    if (days.size == 7) return "Every day"
    if (days.isEmpty()) return "Never"
    
    val dayNames = listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")
    return days.sorted().joinToString(", ") { dayNames[it - 1] }
}
