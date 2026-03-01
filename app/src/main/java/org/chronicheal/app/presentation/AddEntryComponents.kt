package org.chronicheal.app.presentation

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import org.chronicheal.app.domain.model.HealthEntry
import org.chronicheal.app.ui.theme.HeaderBlue
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEntryScaffold(
    title: String,
    existingEntry: HealthEntry?,
    currentEntry: () -> HealthEntry,
    onBackClick: () -> Unit,
    onDeleteClick: () -> Unit,
    onSaveClick: () -> Unit,
    saveButtonEnabled: Boolean,
    saveButtonText: String = if (existingEntry == null) "Save" else "Update",
    viewModel: TimelineViewModel,
    content: @Composable (PaddingValues) -> Unit
) {
    var showDeleteConfirmation by remember { mutableStateOf(false) }

    val hasChanges by remember(existingEntry) {
        derivedStateOf {
            existingEntry?.let { it != currentEntry() } ?: false
        }
    }

    val handleBack = {
        if (existingEntry != null && hasChanges) {
            viewModel.showMessage("Edition canceled")
        }
        onBackClick()
    }

    BackHandler(onBack = handleBack)

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(title) },
                navigationIcon = {
                    IconButton(onClick = handleBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    if (existingEntry != null) {
                        IconButton(onClick = { showDeleteConfirmation = true }) {
                            Icon(Icons.Default.Delete, contentDescription = "Delete")
                        }
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
        Column(modifier = Modifier.fillMaxSize()) {
            Box(modifier = Modifier.weight(1f)) {
                content(innerPadding)
            }
            
            Button(
                onClick = onSaveClick,
                enabled = saveButtonEnabled,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Text(saveButtonText)
            }
        }

        if (showDeleteConfirmation) {
            AlertDialog(
                onDismissRequest = { showDeleteConfirmation = false },
                title = { Text("Delete Entry") },
                text = { Text("Are you sure you want to delete this log?") },
                confirmButton = {
                    TextButton(
                        onClick = {
                            onDeleteClick()
                            showDeleteConfirmation = false
                        }
                    ) {
                        Text("Delete", color = MaterialTheme.colorScheme.error)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showDeleteConfirmation = false }) {
                        Text("Cancel")
                    }
                }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EntryDateTimePicker(
    date: LocalDate,
    onDateChange: (LocalDate) -> Unit,
    startTime: LocalTime,
    onStartTimeChange: (LocalTime) -> Unit
) {
    var showStartTimePicker by remember { mutableStateOf(false) }
    var showDatePicker by remember { mutableStateOf(false) }
    
    val startTimeState = rememberTimePickerState(
        initialHour = startTime.hour,
        initialMinute = startTime.minute
    )

    val datePickerState = rememberDatePickerState(
        initialSelectedDateMillis = date.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
    )

    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Date
            OutlinedCard(
                onClick = { showDatePicker = true },
                modifier = Modifier.weight(1f)
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.CalendarToday, contentDescription = null, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(8.dp))
                    Column {
                        Text("Date", style = MaterialTheme.typography.labelSmall)
                        Text(
                            text = date.format(DateTimeFormatter.ofPattern("MMM dd, yyyy")),
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }

            // Start Time
            OutlinedCard(
                onClick = { showStartTimePicker = true },
                modifier = Modifier.weight(1f)
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.AccessTime, contentDescription = null, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(8.dp))
                    Column {
                        Text("Start Time", style = MaterialTheme.typography.labelSmall)
                        Text(
                            text = startTime.format(DateTimeFormatter.ofPattern("HH:mm")),
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }
        }

        if (showStartTimePicker) {
            TimePickerDialog(
                onDismissRequest = { showStartTimePicker = false },
                confirmButton = {
                    TextButton(onClick = {
                        onStartTimeChange(LocalTime.of(startTimeState.hour, startTimeState.minute))
                        showStartTimePicker = false
                    }) {
                        Text("OK")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showStartTimePicker = false }) {
                        Text("Cancel")
                    }
                }
            ) {
                TimePicker(state = startTimeState)
            }
        }

        if (showDatePicker) {
            DatePickerDialog(
                onDismissRequest = { showDatePicker = false },
                confirmButton = {
                    TextButton(onClick = {
                        datePickerState.selectedDateMillis?.let {
                            onDateChange(Instant.ofEpochMilli(it).atZone(ZoneId.systemDefault()).toLocalDate())
                        }
                        showDatePicker = false
                    }) {
                        Text("OK")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showDatePicker = false }) {
                        Text("Cancel")
                    }
                }
            ) {
                DatePicker(state = datePickerState)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TimePickerDialog(
    onDismissRequest: () -> Unit,
    confirmButton: @Composable () -> Unit,
    dismissButton: @Composable () -> Unit,
    content: @Composable () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismissRequest,
        confirmButton = confirmButton,
        dismissButton = dismissButton,
        text = { content() }
    )
}
