package org.chronicheal.app.presentation

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import org.chronicheal.app.domain.model.EntryType
import org.chronicheal.app.domain.model.HealthEntry
import org.chronicheal.app.domain.model.Reminder
import java.time.*
import java.time.format.DateTimeFormatter
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddSleepScreen(
    dateString: String? = null,
    id: Long? = null,
    onBackClick: () -> Unit,
    onSaveSuccess: () -> Unit,
    viewModel: TimelineViewModel = hiltViewModel()
) {
    var logDate by remember { mutableStateOf(if (dateString != null) LocalDate.parse(dateString) else LocalDate.now()) }
    var startTime by remember { mutableStateOf(LocalTime.of(22, 0)) }
    
    var endDate by remember { mutableStateOf(logDate.plusDays(1)) }
    var endTime by remember { mutableStateOf(LocalTime.of(6, 0)) }

    var quality by remember { mutableFloatStateOf(3f) }
    var note by remember { mutableStateOf("") }
    var existingEntry by remember { mutableStateOf<HealthEntry?>(null) }

    var setReminder by remember { mutableStateOf(false) }
    var reminderTime by remember { mutableStateOf(LocalTime.of(22, 0)) }
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
                quality = entry.intensity?.toFloat() ?: 3f
                note = entry.note
                logDate = entry.timestamp.atZone(ZoneId.systemDefault()).toLocalDate()
                startTime = entry.timestamp.atZone(ZoneId.systemDefault()).toLocalTime()
                
                val durationMins = entry.durationMinutes?.toLong() ?: 480L
                val end = entry.timestamp.plus(Duration.ofMinutes(durationMins)).atZone(ZoneId.systemDefault())
                endDate = end.toLocalDate()
                endTime = end.toLocalTime()
                
                setReminder = entry.hasReminder
                
                if (entry.hasReminder && entry.reminderId != null) {
                    viewModel.getReminderById(entry.reminderId)?.let { reminder ->
                        reminderTime = reminder.time
                    }
                }
            }
        }
    }

    val durationMinutes by remember(logDate, startTime, endDate, endTime) {
        derivedStateOf {
            val start = logDate.atTime(startTime).atZone(ZoneId.systemDefault())
            val end = endDate.atTime(endTime).atZone(ZoneId.systemDefault())
            Duration.between(start, end).toMinutes().toInt().coerceAtLeast(0)
        }
    }

    val durationText = remember(durationMinutes) {
        val h = durationMinutes / 60
        val m = durationMinutes % 60
        if (h > 0) "${h}h ${m}m" else "${m}m"
    }

    val createEntry = {
        HealthEntry(
            id = id ?: 0,
            timestamp = logDate.atTime(startTime).atZone(ZoneId.systemDefault()).toInstant(),
            type = EntryType.SLEEP,
            intensity = quality.roundToInt(),
            note = note,
            hasReminder = setReminder,
            reminderId = existingEntry?.reminderId,
            durationMinutes = durationMinutes
        )
    }

    AddEntryScaffold(
        title = if (id == null) "Log Sleep" else "Edit Sleep",
        existingEntry = existingEntry,
        currentEntry = createEntry,
        onBackClick = onBackClick,
        onDeleteClick = {
            existingEntry?.let { viewModel.deleteEntry(it) }
            onSaveSuccess()
        },
        onSaveClick = {
            val entry = createEntry()
            if (setReminder) {
                val reminder = Reminder(
                    id = existingEntry?.reminderId ?: 0,
                    title = "Sleep Reminder",
                    time = reminderTime,
                    daysOfWeek = (1..7).toSet(),
                    entryType = EntryType.SLEEP
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
        saveButtonEnabled = true,
        viewModel = viewModel
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp)
        ) {
            Text("Bedtime:", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(bottom = 8.dp))
            EntryDateTimePicker(
                date = logDate,
                onDateChange = { logDate = it },
                startTime = startTime,
                onStartTimeChange = { startTime = it }
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text("Wake up time:", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(bottom = 8.dp))
            EntryDateTimePicker(
                date = endDate,
                onDateChange = { endDate = it },
                startTime = endTime,
                onStartTimeChange = { endTime = it }
            )

            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = durationText,
                onValueChange = { },
                label = { Text("Computed Duration") },
                modifier = Modifier.fillMaxWidth(),
                readOnly = true,
                leadingIcon = { Icon(Icons.Default.Timer, contentDescription = null) },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outline
                )
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Quality: ${quality.roundToInt()}/5",
                style = MaterialTheme.typography.titleMedium
            )
            Slider(
                value = quality,
                onValueChange = { quality = it },
                valueRange = 1f..5f,
                steps = 3
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
                    text = if (existingEntry?.hasReminder == true) "Update daily reminder" else "Set daily sleep reminder",
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
