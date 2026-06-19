package org.chronicheal.app.presentation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import org.chronicheal.app.R
import org.chronicheal.app.domain.model.EntryType
import org.chronicheal.app.domain.model.HealthEntry
import org.chronicheal.app.presentation.components.AddEntryScaffold
import org.chronicheal.app.presentation.components.EntryDateTimePicker
import org.chronicheal.app.presentation.components.IntensityField
import org.chronicheal.app.presentation.components.LogNowEffect
import org.chronicheal.app.presentation.components.TimePickerDialog
import org.chronicheal.app.presentation.components.VoiceEnabledTextField
import java.time.Duration
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddSleepScreen(
    dateString: String? = null,
    id: Long? = null,
    reminderId: Long? = null,
    templateId: Long? = null,
    onBackClick: () -> Unit,
    onSaveSuccess: () -> Unit,
    viewModel: AddEntryViewModel = hiltViewModel(),
) {
    var sleepTime by rememberSaveable { mutableStateOf(LocalTime.of(23, 0)) }
    var wakeTime by rememberSaveable { mutableStateOf(LocalTime.of(7, 0)) }
    var intensity by rememberSaveable { mutableIntStateOf(5) }
    var note by rememberSaveable { mutableStateOf("") }
    var logDate by rememberSaveable {
        mutableStateOf(
            if (dateString != null) LocalDate.parse(
                dateString
            ) else LocalDate.now()
        )
    }
    var startTime by rememberSaveable { mutableStateOf(LocalTime.now()) }

    val uiState by viewModel.uiState.collectAsState()
    val existingEntry = uiState.entry
    val isNewFromTemplate = uiState.isNewFromTemplate

    var showSleepTimePicker by rememberSaveable { mutableStateOf(value = false) }
    var showWakeTimePicker by rememberSaveable { mutableStateOf(value = false) }

    LogNowEffect(
        id = id,
        reminderId = reminderId,
        templateId = templateId,
        viewModel = viewModel,
        onEntryFound = { entry, fromTemplate ->
            intensity = entry.intensity ?: 5
            note = entry.note
            if (entry.durationMinutes != null) {
                // If duration is present, we calculate fake sleep/wake times to match duration
                // This is a simplification as the DB model doesn't store both sleep and wake time.
                val duration = Duration.ofMinutes(entry.durationMinutes.toLong())
                sleepTime = LocalTime.of(23, 0)
                wakeTime = sleepTime + duration
            }
            if (!fromTemplate) {
                logDate = entry.timestamp.atZone(ZoneId.systemDefault()).toLocalDate()
                startTime = entry.timestamp.atZone(ZoneId.systemDefault()).toLocalTime()
            }
        }
    )

    val createEntry = {
        var duration = Duration.between(sleepTime, wakeTime)
        if (duration.isNegative) {
            duration = duration.plusDays(1)
        }
        
        HealthEntry(
            id = if (isNewFromTemplate) 0 else (existingEntry?.id ?: 0),
            timestamp = logDate.atTime(startTime).atZone(ZoneId.systemDefault()).toInstant(),
            type = EntryType.SLEEP,
            intensity = intensity,
            note = note,
            durationMinutes = duration.toMinutes().toInt()
        )
    }

    AddEntryScaffold(
        title = if ((id == null) || isNewFromTemplate) stringResource(R.string.log_sleep) else stringResource(
            R.string.edit_sleep
        ),
        hasExistingEntry = (!isNewFromTemplate) && (existingEntry != null),
        onBackClick = onBackClick,
        onSaveClick = {
            viewModel.saveEntry(createEntry(), if (isNewFromTemplate) null else existingEntry)
            onSaveSuccess()
        },
        onDeleteClick = {
            existingEntry?.let { viewModel.deleteEntry(it) }
            onBackClick()
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            EntryDateTimePicker(
                date = logDate,
                onDateChange = { logDate = it },
                startTime = startTime
            ) { startTime = it }

            Spacer(modifier = Modifier.height(16.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(stringResource(R.string.sleep_time))
                Spacer(modifier = Modifier.width(8.dp))
                OutlinedButton(onClick = { showSleepTimePicker = true }) {
                    Text(sleepTime.toString())
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(stringResource(R.string.wake_time))
                Spacer(modifier = Modifier.width(8.dp))
                OutlinedButton(onClick = { showWakeTimePicker = true }) {
                    Text(wakeTime.toString())
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            var duration = Duration.between(sleepTime, wakeTime)
            if (duration.isNegative) {
                duration = duration.plusDays(1)
            }
            Text("Duration: ${duration.toHours()}h ${duration.toMinutes() % 60}m")

            Spacer(modifier = Modifier.height(16.dp))

            VoiceEnabledTextField(
                value = note,
                onValueChange = { note = it },
                label = stringResource(R.string.notes_label),
                minLines = 3
            )

            Spacer(modifier = Modifier.height(16.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 16.dp),
                contentAlignment = Alignment.Center
            ) {
                IntensityField(
                    intensity = intensity,
                    onIntensityChange = { intensity = it ?: 5 },
                    label = stringResource(R.string.sleep_quality_label)
                )
            }
        }
    }

    if (showSleepTimePicker) {
        val timeState =
            rememberTimePickerState(initialHour = sleepTime.hour, initialMinute = sleepTime.minute)
        TimePickerDialog(
            onDismissRequest = {
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        sleepTime = LocalTime.of(timeState.hour, timeState.minute)
                    }
                ) {
                    Text(stringResource(R.string.ok))
                }
            },
            dismissButton = {
                TextButton(onClick = { }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        ) {
            TimePicker(state = timeState)
        }
    }

    if (showWakeTimePicker) {
        val timeState =
            rememberTimePickerState(initialHour = wakeTime.hour, initialMinute = wakeTime.minute)
        TimePickerDialog(
            onDismissRequest = {
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        wakeTime = LocalTime.of(timeState.hour, timeState.minute)
                        showWakeTimePicker = false
                    }
                ) {
                    Text(stringResource(R.string.ok))
                }
            },
            dismissButton = {
                TextButton(onClick = { showWakeTimePicker = false }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        ) {
            TimePicker(state = timeState)
        }
    }
}
