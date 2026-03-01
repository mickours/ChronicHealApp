package org.chronicheal.app.presentation

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
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
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddActivityScreen(
    dateString: String? = null,
    id: Long? = null,
    onBackClick: () -> Unit,
    onSaveSuccess: () -> Unit,
    viewModel: TimelineViewModel = hiltViewModel()
) {
    var name by remember { mutableStateOf("") }
    var startTime by remember { mutableStateOf(LocalTime.now()) }
    var durationMinutes by remember { mutableIntStateOf(EntryType.ACTIVITY.defaultDurationMinutes) }
    var intensity by remember { mutableFloatStateOf(3f) }
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
                name = entry.name ?: ""
                startTime = entry.timestamp.atZone(ZoneId.systemDefault()).toLocalTime()
                durationMinutes = entry.durationMinutes ?: EntryType.ACTIVITY.defaultDurationMinutes
                intensity = entry.intensity?.toFloat() ?: 3f
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

    AddEntryScaffold(
        title = if (id == null) "Log Activity" else "Edit Activity",
        id = id,
        onBackClick = onBackClick,
        onDeleteClick = {
            existingEntry?.let { viewModel.deleteEntry(it) }
            onSaveSuccess()
        },
        onSaveClick = {
            val date = if (dateString != null) LocalDate.parse(dateString) else LocalDate.now()
            val timestamp = date.atTime(startTime).atZone(ZoneId.systemDefault()).toInstant()

            val entry = HealthEntry(
                id = id ?: 0,
                timestamp = timestamp,
                type = EntryType.ACTIVITY,
                name = name,
                durationMinutes = durationMinutes,
                intensity = intensity.roundToInt(),
                note = note,
                hasReminder = setReminder,
                reminderId = existingEntry?.reminderId,
                isFinished = existingEntry?.isFinished ?: false
            )

            if (setReminder) {
                val reminder = Reminder(
                    id = existingEntry?.reminderId ?: 0,
                    title = "Activity: $name",
                    time = reminderTime,
                    daysOfWeek = (1..7).toSet(),
                    entryType = EntryType.ACTIVITY
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
        saveButtonEnabled = name.isNotBlank(),
        viewModel = viewModel
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp)
        ) {
            EntryTimeAndDurationPicker(
                startTime = startTime,
                onStartTimeChange = { startTime = it },
                durationMinutes = durationMinutes,
                onDurationChange = { durationMinutes = it }
            )

            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Activity Name (e.g. Walking, Yoga)") },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Intensity: ${intensity.roundToInt()}/10",
                style = MaterialTheme.typography.titleMedium
            )
            Slider(
                value = intensity,
                onValueChange = { intensity = it },
                valueRange = 1f..10f,
                steps = 8
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
                verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Checkbox(
                    checked = setReminder,
                    onCheckedChange = { setReminder = it }
                )
                Text(
                    text = if (existingEntry?.hasReminder == true) "Update daily reminder" else "Set daily reminder for this activity",
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
