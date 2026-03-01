package org.chronicheal.app.presentation

import androidx.compose.foundation.layout.*
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
fun AddMedicalAppointmentScreen(
    dateString: String? = null,
    id: Long? = null,
    onBackClick: () -> Unit,
    onSaveSuccess: () -> Unit,
    viewModel: TimelineViewModel = hiltViewModel()
) {
    var doctorName by remember { mutableStateOf("") }
    var purpose by remember { mutableStateOf("") }
    var outcome by remember { mutableStateOf("") }
    var note by remember { mutableStateOf("") }
    var startTime by remember { mutableStateOf(LocalTime.now()) }
    var durationMinutes by remember { mutableIntStateOf(EntryType.MEDICAL_APPOINTMENT.defaultDurationMinutes) }
    
    var setReminder by remember { mutableStateOf(false) }
    var reminderTime by remember { mutableStateOf(LocalTime.now()) }
    var showTimePicker by remember { mutableStateOf(false) }
    var existingEntry by remember { mutableStateOf<HealthEntry?>(null) }

    val timeState = rememberTimePickerState(
        initialHour = reminderTime.hour,
        initialMinute = reminderTime.minute
    )

    LaunchedEffect(id) {
        if (id != null) {
            val entry = viewModel.getEntryById(id)
            if (entry != null) {
                existingEntry = entry
                doctorName = entry.name ?: ""
                purpose = entry.location ?: ""
                note = entry.note
                startTime = entry.timestamp.atZone(ZoneId.systemDefault()).toLocalTime()
                durationMinutes = entry.durationMinutes ?: EntryType.MEDICAL_APPOINTMENT.defaultDurationMinutes
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
        title = if (id == null) "Medical Appointment" else "Edit Appointment",
        id = id,
        onBackClick = onBackClick,
        onDeleteClick = {
            existingEntry?.let { viewModel.deleteEntry(it) }
            onSaveSuccess()
        },
        onSaveClick = {
            val date = if (dateString != null) LocalDate.parse(dateString) else LocalDate.now()
            val timestamp = date.atTime(startTime).atZone(ZoneId.systemDefault()).toInstant()

            val finalNote = buildString {
                append(note)
                if (outcome.isNotBlank() && !note.contains("Outcome: $outcome")) {
                    if (isNotEmpty()) append("\n\n")
                    append("Outcome: $outcome")
                }
            }
            val entry = HealthEntry(
                id = id ?: 0,
                timestamp = timestamp,
                type = EntryType.MEDICAL_APPOINTMENT,
                name = doctorName,
                location = purpose,
                note = finalNote,
                hasReminder = setReminder,
                reminderId = existingEntry?.reminderId,
                isFinished = existingEntry?.isFinished ?: false,
                durationMinutes = durationMinutes
            )

            if (setReminder) {
                val reminder = Reminder(
                    id = existingEntry?.reminderId ?: 0,
                    title = "Appointment with $doctorName",
                    time = reminderTime,
                    daysOfWeek = (1..7).toSet(),
                    entryType = EntryType.MEDICAL_APPOINTMENT
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
        saveButtonEnabled = doctorName.isNotBlank(),
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
                value = doctorName,
                onValueChange = { doctorName = it },
                label = { Text("Doctor/Specialist Name") },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = purpose,
                onValueChange = { purpose = it },
                label = { Text("Purpose (e.g. Follow-up, Scan)") },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = outcome,
                onValueChange = { outcome = it },
                label = { Text("Outcome/Diagnosis") },
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
                    text = if (existingEntry?.hasReminder == true) "Update daily reminder" else "Set daily reminder for this appointment",
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
