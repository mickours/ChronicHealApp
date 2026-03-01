package org.chronicheal.app.presentation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import org.chronicheal.app.domain.model.EntryType
import org.chronicheal.app.domain.model.Reminder
import java.time.LocalTime
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun AddReminderScreen(
    onBackClick: () -> Unit,
    onSaveSuccess: () -> Unit,
    viewModel: RemindersViewModel = hiltViewModel()
) {
    var title by remember { mutableStateOf("") }
    var selectedTime by remember { mutableStateOf(LocalTime.now()) }
    var selectedDays by remember { mutableStateOf(setOf(1, 2, 3, 4, 5, 6, 7)) }
    var selectedType by remember { mutableStateOf<EntryType?>(null) }
    var showTimePicker by remember { mutableStateOf(false) }

    val timeState = rememberTimePickerState(
        initialHour = selectedTime.hour,
        initialMinute = selectedTime.minute
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Add Reminder") },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    Button(
                        onClick = {
                            val reminder = Reminder(
                                title = title.ifBlank { "Health Reminder" },
                                time = selectedTime,
                                daysOfWeek = selectedDays,
                                entryType = selectedType
                            )
                            viewModel.addReminder(reminder)
                            onSaveSuccess()
                        },
                        modifier = Modifier.padding(end = 8.dp)
                    ) {
                        Text("Save")
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
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                label = { Text("Title (e.g., Take Medication)") },
                modifier = Modifier.fillMaxWidth()
            )

            Text("Time", style = MaterialTheme.typography.titleMedium)
            
            Button(
                onClick = { showTimePicker = true },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.secondaryContainer,
                    contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                )
            ) {
                Text(
                    text = selectedTime.format(DateTimeFormatter.ofPattern("HH:mm")),
                    style = MaterialTheme.typography.headlineMedium
                )
            }

            Text("Days of Week", style = MaterialTheme.typography.titleMedium)
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                val dayLabels = listOf("M", "T", "W", "T", "F", "S", "S")
                for (i in 1..7) {
                    val isSelected = selectedDays.contains(i)
                    FilterChip(
                        selected = isSelected,
                        onClick = {
                            selectedDays = if (isSelected) {
                                selectedDays - i
                            } else {
                                selectedDays + i
                            }
                        },
                        label = { Text(dayLabels[i - 1]) }
                    )
                }
            }

            Text("Related Category (Optional)", style = MaterialTheme.typography.titleMedium)
            
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                EntryType.entries.forEach { type ->
                    FilterChip(
                        selected = selectedType == type,
                        onClick = {
                            selectedType = if (selectedType == type) null else type
                        },
                        label = { Text(type.name.lowercase().replaceFirstChar { it.uppercase() }.replace("_", " ")) }
                    )
                }
            }
        }

        if (showTimePicker) {
            TimePickerDialog(
                onDismissRequest = { showTimePicker = false },
                confirmButton = {
                    TextButton(onClick = {
                        selectedTime = LocalTime.of(timeState.hour, timeState.minute)
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
