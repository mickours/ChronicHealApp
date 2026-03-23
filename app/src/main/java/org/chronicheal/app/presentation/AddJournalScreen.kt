package org.chronicheal.app.presentation

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import org.chronicheal.app.R
import org.chronicheal.app.domain.model.EntryType
import org.chronicheal.app.domain.model.HealthEntry
import org.chronicheal.app.domain.model.Reminder
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddJournalScreen(
    dateString: String? = null,
    id: Long? = null,
    reminderId: Long? = null,
    templateId: Long? = null,
    onBackClick: () -> Unit,
    onSaveSuccess: () -> Unit,
    viewModel: TimelineViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    var content by rememberSaveable { mutableStateOf("") }
    var logDate by rememberSaveable { mutableStateOf(if (dateString != null) LocalDate.parse(dateString) else LocalDate.now()) }
    var startTime by rememberSaveable { mutableStateOf(LocalTime.now()) }
    var existingEntry by remember { mutableStateOf<HealthEntry?>(null) }
    var isNewFromTemplate by remember { mutableStateOf(false) }

    var setReminder by rememberSaveable { mutableStateOf(false) }
    var reminderTime by rememberSaveable { mutableStateOf(LocalTime.now()) }
    var showTimePicker by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(id, reminderId) {
        if (id != null) {
            val entry = viewModel.getEntryById(id)
            if (entry != null) {
                existingEntry = entry
                isNewFromTemplate = false
                content = entry.note
                logDate = entry.timestamp.atZone(ZoneId.systemDefault()).toLocalDate()
                startTime = entry.timestamp.atZone(ZoneId.systemDefault()).toLocalTime()
                setReminder = entry.hasReminder
                
                if (entry.hasReminder && entry.reminderId != null) {
                    viewModel.getReminderById(entry.reminderId)?.let { reminder ->
                        reminderTime = reminder.time
                    }
                }
            }
        } else if (reminderId != null) {
            val entry = viewModel.getEntryByReminderId(reminderId)
            if (entry != null) {
                existingEntry = entry
                isNewFromTemplate = true
                content = entry.note
                setReminder = entry.hasReminder
                
                viewModel.getReminderById(reminderId)?.let { reminder ->
                    reminderTime = reminder.time
                }
            }
        }
    }

    val createEntry = {
        HealthEntry(
            id = if (isNewFromTemplate) 0 else (id ?: 0),
            timestamp = logDate.atTime(startTime).atZone(ZoneId.systemDefault()).toInstant(),
            type = EntryType.JOURNAL,
            note = content,
            hasReminder = setReminder,
            reminderId = existingEntry?.reminderId,
            durationMinutes = existingEntry?.durationMinutes
        )
    }

    val handleSave = {
        val entry = createEntry()
        if (setReminder) {
            val reminder = Reminder(
                id = existingEntry?.reminderId ?: 0,
                title = context.getString(R.string.type_journal),
                time = reminderTime,
                daysOfWeek = setOf(1, 2, 3, 4, 5, 6, 7),
                isEnabled = true,
                entryType = EntryType.JOURNAL
            )
            if (entry.id == 0L) {
                viewModel.addEntryWithReminder(entry, reminder)
            } else {
                viewModel.updateEntryWithReminder(entry, reminder)
            }
        } else {
            viewModel.saveEntryAndNotify(if (isNewFromTemplate) null else existingEntry, entry)
        }
        onSaveSuccess()
    }

    AddEntryScaffold(
        title = if (id == null || isNewFromTemplate) stringResource(R.string.log_journal) else stringResource(R.string.edit_journal),
        existingEntry = if (isNewFromTemplate) null else existingEntry,
        currentEntry = createEntry,
        onBackClick = onBackClick,
        onSaveSuccess = onSaveSuccess,
        onDeleteClick = {
            existingEntry?.let { viewModel.deleteEntry(it) }
            onBackClick()
        },
        viewModel = viewModel,
        onSave = handleSave
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp)
        ) {
            EntryDateTimePicker(
                date = logDate,
                onDateChange = { logDate = it },
                startTime = startTime,
                onStartTimeChange = { startTime = it }
            )

            Spacer(modifier = Modifier.height(16.dp))

            VoiceEnabledTextField(
                value = content,
                onValueChange = { content = it },
                label = stringResource(R.string.section_anything_else),
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                minLines = 10
            )

            Spacer(modifier = Modifier.height(16.dp))

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
                OutlinedButton(
                    onClick = { showTimePicker = true },
                    modifier = Modifier.padding(start = 32.dp)
                ) {
                    Text(stringResource(R.string.time_label) + ": ${reminderTime.format(DateTimeFormatter.ofPattern("HH:mm"))}")
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
