package org.chronicheal.app.presentation

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import kotlinx.coroutines.launch
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
fun AddDrugScreen(
    dateString: String? = null,
    id: Long? = null,
    onBackClick: () -> Unit,
    onSaveSuccess: () -> Unit,
    viewModel: TimelineViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    
    var name by rememberSaveable { mutableStateOf("") }
    var dosage by rememberSaveable { mutableStateOf("") }
    var note by rememberSaveable { mutableStateOf("") }
    var logDate by rememberSaveable { mutableStateOf(if (dateString != null) LocalDate.parse(dateString) else LocalDate.now()) }
    var startTime by rememberSaveable { mutableStateOf(LocalTime.now()) }
    
    var setReminder by rememberSaveable { mutableStateOf(false) }
    var reminderTime by rememberSaveable { mutableStateOf(LocalTime.now()) }
    var showTimePicker by rememberSaveable { mutableStateOf(false) }
    var existingEntry by remember { mutableStateOf<HealthEntry?>(null) }
    var isNewFromTemplate by remember { mutableStateOf(false) }

    val nameSuggestions by viewModel.drugSuggestions.collectAsState()

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            setReminder = true
        } else {
            scope.launch {
                snackbarHostState.showSnackbar(context.getString(R.string.notification_permission_required))
            }
        }
    }

    LaunchedEffect(id) {
        if (id != null && existingEntry == null) {
            var entry = viewModel.getEntryById(id)
            if (entry == null) {
                entry = viewModel.getEntryByReminderId(id)
                if (entry != null) {
                    isNewFromTemplate = true
                }
            }
            
            if (entry != null) {
                existingEntry = entry
                name = entry.name ?: ""
                dosage = entry.unit ?: ""
                note = entry.note
                if (!isNewFromTemplate) {
                    logDate = entry.timestamp.atZone(ZoneId.systemDefault()).toLocalDate()
                    startTime = entry.timestamp.atZone(ZoneId.systemDefault()).toLocalTime()
                }
                setReminder = entry.hasReminder
                
                if (entry.hasReminder && entry.reminderId != null) {
                    viewModel.getReminderById(entry.reminderId)?.let { reminder ->
                        reminderTime = reminder.time
                    }
                }
            }
        }
    }

    val createEntry = {
        HealthEntry(
            id = if (isNewFromTemplate) 0 else (id ?: 0),
            timestamp = logDate.atTime(startTime).atZone(ZoneId.systemDefault()).toInstant(),
            type = EntryType.DRUG,
            name = name.trim(),
            unit = dosage.trim(),
            note = note,
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
                title = context.getString(R.string.type_drug) + ": ${entry.name}",
                time = reminderTime,
                daysOfWeek = setOf(1, 2, 3, 4, 5, 6, 7),
                isEnabled = true,
                entryType = EntryType.DRUG
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
        title = if (id == null || isNewFromTemplate) stringResource(R.string.log_drug) else stringResource(R.string.edit_drug),
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
                label = stringResource(R.string.name_label)
            )

            Spacer(modifier = Modifier.height(16.dp))

            VoiceEnabledTextField(
                value = dosage,
                onValueChange = { dosage = it },
                label = stringResource(R.string.dosage_label)
            )

            Spacer(modifier = Modifier.height(16.dp))

            VoiceEnabledTextField(
                value = note,
                onValueChange = { note = it },
                label = stringResource(R.string.notes_label),
                minLines = 3
            )

            Spacer(modifier = Modifier.height(24.dp))

            Row(
                verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Checkbox(
                    checked = setReminder,
                    onCheckedChange = { checked ->
                        if (checked) {
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                when (ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS)) {
                                    PackageManager.PERMISSION_GRANTED -> setReminder = true
                                    else -> permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                                }
                            } else {
                                setReminder = true
                            }
                        } else {
                            setReminder = false
                        }
                    }
                )
                Text(
                    text = if (existingEntry?.hasReminder == true) stringResource(R.string.update_daily_reminder) else stringResource(R.string.set_daily_reminder),
                    style = MaterialTheme.typography.bodyLarge
                )
            }

            if (setReminder) {
                val timeFormatter = remember { DateTimeFormatter.ofPattern("HH:mm") } // Can be localized
                OutlinedButton(
                    onClick = { showTimePicker = true },
                    modifier = Modifier.padding(start = 32.dp)
                ) {
                    Text(stringResource(R.string.time_label) + ": ${reminderTime.format(timeFormatter)}")
                }
            }
        }
        
        SnackbarHost(hostState = snackbarHostState, modifier = Modifier.padding(innerPadding))

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
