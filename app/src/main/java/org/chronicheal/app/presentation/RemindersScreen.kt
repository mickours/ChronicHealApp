package org.chronicheal.app.presentation

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import org.chronicheal.app.domain.model.Reminder
import java.time.format.DateTimeFormatter
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RemindersScreen(
    onBackClick: () -> Unit,
    onAddReminderClick: () -> Unit,
    onReminderClick: (Long) -> Unit,
    viewModel: RemindersViewModel = hiltViewModel()
) {
    val reminders by viewModel.reminders.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Manage Reminders") },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = onAddReminderClick) {
                Icon(Icons.Default.Add, contentDescription = "Add Reminder")
            }
        }
    ) { innerPadding ->
        if (reminders.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentAlignment = Alignment.Center
            ) {
                Text("No reminders set")
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(reminders) { reminder ->
                    ReminderItem(
                        reminder = reminder,
                        onToggle = { viewModel.toggleReminder(reminder) },
                        onDelete = { viewModel.deleteReminder(reminder) },
                        onClick = { onReminderClick(reminder.id) }
                    )
                }
            }
        }
    }
}

@Composable
fun ReminderItem(
    reminder: Reminder,
    onToggle: () -> Unit,
    onDelete: () -> Unit,
    onClick: () -> Unit
) {
    val timeFormatter = DateTimeFormatter.ofPattern("HH:mm")
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(text = reminder.title, style = MaterialTheme.typography.titleMedium)
                    if (reminder.entryType != null) {
                        Spacer(Modifier.width(8.dp))
                        SuggestionChip(
                            onClick = { },
                            label = { 
                                Text(
                                    text = reminder.entryType.name.lowercase()
                                        .replaceFirstChar { it.titlecase(Locale.getDefault()) },
                                    style = MaterialTheme.typography.labelSmall
                                ) 
                            },
                            icon = {
                                Icon(
                                    Icons.Default.NotificationsActive,
                                    contentDescription = null,
                                    modifier = Modifier.size(12.dp)
                                )
                            }
                        )
                    }
                }
                Text(
                    text = reminder.time.format(timeFormatter),
                    style = MaterialTheme.typography.bodyMedium
                )
                Text(
                    text = getDaysOfWeekString(reminder.daysOfWeek),
                    style = MaterialTheme.typography.bodySmall
                )
            }
            
            Switch(
                checked = reminder.isEnabled,
                onCheckedChange = { onToggle() }
            )
            
            IconButton(onClick = onDelete) {
                Icon(Icons.Default.Delete, contentDescription = "Delete")
            }
        }
    }
}

private fun getDaysOfWeekString(days: Set<Int>): String {
    if (days.size == 7) return "Every day"
    if (days.isEmpty()) return "Never"
    
    val dayNames = listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")
    return days.sorted().joinToString(", ") { dayNames[it - 1] }
}
