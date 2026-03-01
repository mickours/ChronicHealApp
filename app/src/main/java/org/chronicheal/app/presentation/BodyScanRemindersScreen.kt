package org.chronicheal.app.presentation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import org.chronicheal.app.domain.model.EntryType

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BodyScanRemindersScreen(
    onBackClick: () -> Unit,
    onAddReminderClick: () -> Unit,
    viewModel: RemindersViewModel = hiltViewModel()
) {
    val reminders by viewModel.reminders.collectAsState()
    val bodyScanReminders = reminders.filter { it.entryType == EntryType.PAIN }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Body Scan Reminders") },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = onAddReminderClick) {
                Icon(Icons.Default.Add, contentDescription = "Add Body Scan Reminder")
            }
        }
    ) { innerPadding ->
        if (bodyScanReminders.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentAlignment = Alignment.Center
            ) {
                Text("No reminders set for Body Scan")
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(bodyScanReminders) { reminder ->
                    ReminderItem(
                        reminder = reminder,
                        onToggle = { viewModel.toggleReminder(reminder) },
                        onDelete = { viewModel.deleteReminder(reminder) }
                    )
                }
            }
        }
    }
}
