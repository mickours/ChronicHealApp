package org.chronicheal.app.presentation

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import org.chronicheal.app.domain.model.EntryType
import org.chronicheal.app.domain.model.HealthEntry
import org.chronicheal.app.domain.model.Reminder
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddMealScreen(
    dateString: String? = null,
    id: Long? = null,
    onBackClick: () -> Unit,
    onSaveSuccess: () -> Unit,
    viewModel: TimelineViewModel = hiltViewModel()
) {
    var description by remember { mutableStateOf("") }
    var triggers by remember { mutableStateOf("") }
    var note by remember { mutableStateOf("") }
    var existingEntry by remember { mutableStateOf<HealthEntry?>(null) }

    var setReminder by remember { mutableStateOf(false) }
    var reminderTime by remember { mutableStateOf(LocalTime.now()) }
    var showTimePicker by remember { mutableStateOf(false) }

    val timeState = rememberTimePickerState(
        initialHour = reminderTime.hour,
        initialMinute = reminderTime.minute
    )

    LaunchedEffect(id) {
        if (id != null) {
            val entry = viewModel.getEntryById(id)
            if (entry != null) {
                existingEntry = entry
                description = entry.name ?: ""
                triggers = entry.location ?: ""
                note = entry.note
                setReminder = entry.hasReminder
                
                if (entry.hasReminder && entry.reminderId != null) {
                    viewModel.getReminderById(entry.reminderId)?.let { reminder ->
                        reminderTime = reminder.time
                    }
                }
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (id == null) "Log Meal" else "Edit Meal") },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp)
        ) {
            OutlinedTextField(
                value = description,
                onValueChange = { description = it },
                label = { Text("Meal Description (e.g. Breakfast, Pasta)") },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = triggers,
                onValueChange = { triggers = it },
                label = { Text("Potential Triggers (e.g. Gluten, Dairy)") },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = note,
                onValueChange = { note = it },
                label = { Text("Notes") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 3
            )

            Spacer(modifier = Modifier.height(24.dp))

            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Checkbox(
                    checked = setReminder,
                    onCheckedChange = { setReminder = it }
                )
                Text(
                    text = if (existingEntry?.hasReminder == true) "Update daily reminder" else "Set daily reminder for this meal",
                    style = MaterialTheme.typography.bodyLarge
                )
            }

            if (setReminder) {
                OutlinedButton(
                    onClick = { showTimePicker = true },
                    modifier = Modifier.padding(start = 32.dp)
                ) {
                    Text("Time: ${reminderTime.format(DateTimeFormatter.ofPattern("HH:mm"))}")
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            Button(
                onClick = {
                    val timestamp = if (id == null) {
                        if (dateString != null) {
                            LocalDate.parse(dateString).atTime(LocalTime.now()).atZone(ZoneId.systemDefault()).toInstant()
                        } else {
                            java.time.Instant.now()
                        }
                    } else {
                        existingEntry?.timestamp ?: java.time.Instant.now()
                    }

                    val entry = HealthEntry(
                        id = id ?: 0,
                        timestamp = timestamp,
                        type = EntryType.MEAL,
                        name = description,
                        location = triggers,
                        note = note,
                        hasReminder = setReminder,
                        reminderId = existingEntry?.reminderId
                    )

                    if (setReminder) {
                        val reminder = Reminder(
                            id = existingEntry?.reminderId ?: 0,
                            title = "Meal: $description",
                            time = reminderTime,
                            daysOfWeek = (1..7).toSet(),
                            entryType = EntryType.MEAL
                        )
                        if (id == null) {
                            viewModel.addEntryWithReminder(entry, reminder)
                        } else {
                            viewModel.updateEntryWithReminder(entry, reminder)
                        }
                    } else {
                        if (id == null) {
                            viewModel.addEntry(entry)
                        } else {
                            viewModel.updateEntry(entry)
                        }
                    }
                    onSaveSuccess()
                },
                enabled = description.isNotBlank(),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(if (id == null) "Save" else "Update")
            }
        }

        if (showTimePicker) {
            TimePickerDialog(
                onDismissRequest = { showTimePicker = false },
                confirmButton = {
                    TextButton(onClick = {
                        reminderTime = LocalTime.of(timeState.hour, timeState.minute)
                        showTimePicker = false
                    }) {
                        Text("OK")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showTimePicker = false }) {
                        Text("Cancel")
                    }
                }
            ) {
                TimePicker(state = timeState)
            }
        }
    }
}
