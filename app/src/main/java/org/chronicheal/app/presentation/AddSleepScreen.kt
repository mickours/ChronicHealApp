package org.chronicheal.app.presentation

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
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
import java.time.Duration
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
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
    val now = remember { LocalTime.now() }
    val today = remember { LocalDate.now() }

    // Logic for default values based on time of day
    val defaultStartDateTime = remember(now, today) {
        when {
            now.hour in 5..11 -> today.minusDays(1).atTime(now.minusHours(8)) // Morning: 8h before
            now.hour in 12..17 -> today.atTime(now) // Afternoon: Now
            else -> today.atTime(now) // Evening: Now
        }
    }

    val defaultEndDateTime = remember(now, today) {
        when {
            now.hour in 5..11 -> today.atTime(now) // Morning: Now
            now.hour in 12..17 -> today.atTime(now.plusHours(1)) // Afternoon: In 1 hour
            else -> today.plusDays(1).atTime(now.plusHours(8)) // Evening: 8h after
        }
    }

    var logDate by rememberSaveable { mutableStateOf(if (dateString != null) LocalDate.parse(dateString) else defaultStartDateTime.toLocalDate()) }
    var startTime by rememberSaveable { mutableStateOf(defaultStartDateTime.toLocalTime()) }
    
    var endDate by rememberSaveable { mutableStateOf(if (dateString != null) logDate.plusDays(1) else defaultEndDateTime.toLocalDate()) }
    var endTime by rememberSaveable { mutableStateOf(defaultEndDateTime.toLocalTime()) }

    var quality by rememberSaveable { mutableFloatStateOf(5f) }
    var note by rememberSaveable { mutableStateOf("") }
    var existingEntry by remember { mutableStateOf<HealthEntry?>(null) }

    var setReminder by rememberSaveable { mutableStateOf(false) }
    var reminderTime by rememberSaveable { mutableStateOf(LocalTime.of(22, 0)) }
    var showTimePicker by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(id) {
        if (id != null && existingEntry == null) {
            val entry = viewModel.getEntryById(id)
            if (entry != null) {
                existingEntry = entry
                quality = entry.intensity?.toFloat() ?: 5f
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
        title = if (id == null) stringResource(R.string.log_sleep) else stringResource(R.string.edit_sleep),
        existingEntry = existingEntry,
        currentEntry = createEntry,
        onBackClick = onBackClick,
        onSaveSuccess = onSaveSuccess,
        onDeleteClick = {
            existingEntry?.let { viewModel.deleteEntry(it) }
            onBackClick()
        },
        viewModel = viewModel
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp)
        ) {
            Text(stringResource(R.string.bedtime_label), style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(bottom = 8.dp))
            EntryDateTimePicker(
                date = logDate,
                onDateChange = { logDate = it },
                startTime = startTime,
                onStartTimeChange = { startTime = it }
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(stringResource(R.string.wakeup_time_label), style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(bottom = 8.dp))
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
                label = { Text(stringResource(R.string.computed_duration_label)) },
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
                text = stringResource(R.string.quality_label, quality.roundToInt()),
                style = MaterialTheme.typography.titleMedium
            )
            Slider(
                value = quality,
                onValueChange = { quality = it },
                valueRange = 1f..10f,
                steps = 8
            )

            Spacer(modifier = Modifier.height(16.dp))

            TextField(
                value = note,
                onValueChange = { note = it },
                label = stringResource(R.string.notes_label),
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
                    text = if (existingEntry?.hasReminder == true) stringResource(R.string.update_daily_reminder) else stringResource(R.string.set_daily_reminder),
                    style = MaterialTheme.typography.bodyLarge
                )
            }

            if (setReminder) {
                val timeFormatter = remember { DateTimeFormatter.ofPattern("HH:mm") }
                OutlinedButton(
                    onClick = { showTimePicker = true },
                    modifier = Modifier.padding(start = 32.dp)
                ) {
                    Text(stringResource(R.string.time_label) + ": ${reminderTime.format(timeFormatter)}")
                }
            }
        }

        if (showTimePicker) {
            val timeState = rememberTimePickerState(
                initialHour = reminderTime.hour,
                initialMinute = reminderTime.minute
            )
            TimePickerDialog(
                onDismissRequest = { showTimePicker = false },
                confirmButton = {
                    TextButton(onClick = {
                        reminderTime = LocalTime.of(timeState.hour, timeState.minute)
                        showTimePicker = false
                    }) {
                        Text(stringResource(R.string.ok))
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showTimePicker = false }) {
                        Text(stringResource(R.string.cancel))
                    }
                }
            ) {
                TimePicker(state = timeState)
            }
        }
    }
}
