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
fun AddMedicalAppointmentScreen(
    dateString: String? = null,
    onBackClick: () -> Unit,
    onSaveSuccess: () -> Unit,
    viewModel: TimelineViewModel = hiltViewModel()
) {
    var doctorName by remember { mutableStateOf("") }
    var purpose by remember { mutableStateOf("") }
    var outcome by remember { mutableStateOf("") }
    var note by remember { mutableStateOf("") }
    
    var setReminder by remember { mutableStateOf(false) }
    var reminderTime by remember { mutableStateOf(LocalTime.now()) }
    var showTimePicker by remember { mutableStateOf(false) }

    val timeState = rememberTimePickerState(
        initialHour = reminderTime.hour,
        initialMinute = reminderTime.minute
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Medical Appointment") },
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
                Text("Set daily reminder for this appointment", style = MaterialTheme.typography.bodyLarge)
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
                    val timestamp = if (dateString != null) {
                        LocalDate.parse(dateString).atTime(LocalTime.now()).atZone(ZoneId.systemDefault()).toInstant()
                    } else {
                        java.time.Instant.now()
                    }
                    val finalNote = buildString {
                        append(note)
                        if (outcome.isNotBlank()) {
                            if (isNotEmpty()) append("\n\n")
                            append("Outcome: $outcome")
                        }
                    }
                    val entry = HealthEntry(
                        timestamp = timestamp,
                        type = EntryType.MEDICAL_APPOINTMENT,
                        name = doctorName,
                        location = purpose,
                        note = finalNote,
                        hasReminder = setReminder
                    )

                    if (setReminder) {
                        val reminder = Reminder(
                            title = "Appointment with $doctorName",
                            time = reminderTime,
                            daysOfWeek = (1..7).toSet(),
                            entryType = EntryType.MEDICAL_APPOINTMENT
                        )
                        viewModel.addEntryWithReminder(entry, reminder)
                    } else {
                        viewModel.addEntry(entry)
                    }
                    onSaveSuccess()
                },
                enabled = doctorName.isNotBlank(),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Save")
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
