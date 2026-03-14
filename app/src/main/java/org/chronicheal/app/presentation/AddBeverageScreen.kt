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
import androidx.compose.runtime.collectAsState
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
fun AddBeverageScreen(
    dateString: String? = null,
    id: Long? = null,
    reminderId: Long? = null,
    onBackClick: () -> Unit,
    onSaveSuccess: () -> Unit,
    viewModel: TimelineViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    var name by rememberSaveable { mutableStateOf("") }
    var quantity by rememberSaveable { mutableStateOf("") }
    var note by rememberSaveable { mutableStateOf("") }
    var logDate by rememberSaveable { mutableStateOf(if (dateString != null) LocalDate.parse(dateString) else LocalDate.now()) }
    var startTime by rememberSaveable { mutableStateOf(LocalTime.now()) }
    
    var setReminder by rememberSaveable { mutableStateOf(false) }
    var reminderTime by rememberSaveable { mutableStateOf(LocalTime.now()) }
    var showTimePicker by rememberSaveable { mutableStateOf(false) }
    var isAlcoholic by rememberSaveable { mutableStateOf(false) }
    var isCaffeinated by rememberSaveable { mutableStateOf(false) }
    
    var existingEntry by remember { mutableStateOf<HealthEntry?>(null) }
    var isNewFromTemplate by remember { mutableStateOf(false) }

    val nameSuggestions by viewModel.beverageSuggestions.collectAsState()

    LogNowEffect(
        id = id, 
        reminderId = reminderId,
        viewModel = viewModel,
        onEntryFound = { entry, fromTemplate ->
            existingEntry = entry
            isNewFromTemplate = fromTemplate
            name = entry.name ?: ""
            quantity = entry.unit ?: ""
            note = entry.note
            if (!isNewFromTemplate) {
                logDate = entry.timestamp.atZone(ZoneId.systemDefault()).toLocalDate()
                startTime = entry.timestamp.atZone(ZoneId.systemDefault()).toLocalTime()
            }
            setReminder = entry.hasReminder
            isAlcoholic = entry.isAlcoholic ?: false
            isCaffeinated = entry.isCaffeinated ?: false
        },
        onReminderTimeFound = { reminderTime = it }
    )

    val createEntry = {
        HealthEntry(
            id = if (isNewFromTemplate) 0 else (id ?: 0),
            timestamp = logDate.atTime(startTime).atZone(ZoneId.systemDefault()).toInstant(),
            type = EntryType.BEVERAGE,
            name = name.trim(),
            unit = quantity.trim(),
            note = note,
            hasReminder = setReminder,
            reminderId = existingEntry?.reminderId,
            durationMinutes = existingEntry?.durationMinutes,
            isAlcoholic = isAlcoholic,
            isCaffeinated = isCaffeinated
        )
    }

    val handleSave = {
        val entry = createEntry()
        if (setReminder) {
            val reminder = Reminder(
                id = existingEntry?.reminderId ?: 0,
                title = context.getString(R.string.type_beverage) + ": ${entry.name}",
                time = reminderTime,
                daysOfWeek = setOf(1, 2, 3, 4, 5, 6, 7),
                isEnabled = true,
                entryType = EntryType.BEVERAGE
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
        title = if (id == null || isNewFromTemplate) stringResource(R.string.log_beverage) else stringResource(R.string.edit_beverage),
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

            AutoCompleteTextField(
                value = name,
                onValueChange = { name = it },
                suggestions = nameSuggestions,
                label = stringResource(R.string.beverage_name_label)
            )

            Spacer(modifier = Modifier.height(16.dp))

            VoiceEnabledTextField(
                value = quantity,
                onValueChange = { quantity = it },
                label = stringResource(R.string.dosage_label)
            )

            Spacer(modifier = Modifier.height(16.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Checkbox(checked = isAlcoholic, onCheckedChange = { isAlcoholic = it })
                Text(text = stringResource(R.string.is_alcoholic), style = MaterialTheme.typography.bodyLarge)
                Spacer(modifier = Modifier.padding(8.dp))
                Checkbox(checked = isCaffeinated, onCheckedChange = { isCaffeinated = it })
                Text(text = stringResource(R.string.is_caffeinated), style = MaterialTheme.typography.bodyLarge)
            }

            Spacer(modifier = Modifier.height(16.dp))

            VoiceEnabledTextField(
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
