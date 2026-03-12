package org.chronicheal.app.presentation

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import org.chronicheal.app.R
import org.chronicheal.app.domain.model.EntryType
import org.chronicheal.app.domain.model.HealthEntry
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddDrugScreen(
    dateString: String? = null,
    id: Long? = null,
    reminderId: Long? = null,
    onBackClick: () -> Unit,
    onSaveSuccess: () -> Unit,
    viewModel: TimelineViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }
    
    var name by rememberSaveable { mutableStateOf("") }
    var dosage by rememberSaveable { mutableStateOf("") }
    var note by rememberSaveable { mutableStateOf("") }
    var logDate by rememberSaveable { mutableStateOf(if (dateString != null) LocalDate.parse(dateString) else LocalDate.now()) }
    var startTime by rememberSaveable { mutableStateOf(LocalTime.now()) }
    
    var setReminder by rememberSaveable { mutableStateOf(false) }
    var reminderTime by rememberSaveable { mutableStateOf(LocalTime.now()) }
    var existingEntry by remember { mutableStateOf<HealthEntry?>(null) }
    var isNewFromTemplate by remember { mutableStateOf(false) }

    val nameSuggestions by viewModel.drugSuggestions.collectAsState()

    LogNowEffect(
        id = id,
        reminderId = reminderId,
        viewModel = viewModel,
        onEntryFound = { entry, fromTemplate ->
            existingEntry = entry
            isNewFromTemplate = fromTemplate
            name = entry.name ?: ""
            dosage = entry.unit ?: ""
            note = entry.note
            if (!isNewFromTemplate) {
                logDate = entry.timestamp.atZone(ZoneId.systemDefault()).toLocalDate()
                startTime = entry.timestamp.atZone(ZoneId.systemDefault()).toLocalTime()
            }
            setReminder = entry.hasReminder
        },
        onReminderTimeFound = { reminderTime = it }
    )

    val createEntry = {
        HealthEntry(
            id = if (isNewFromTemplate) 0 else (existingEntry?.id ?: 0),
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
        handleEntrySave(
            viewModel = viewModel,
            existingEntry = existingEntry,
            isNewFromTemplate = isNewFromTemplate,
            currentEntry = createEntry(),
            setReminder = setReminder,
            reminderTime = if (setReminder) reminderTime else null,
            reminderTitle = context.getString(R.string.type_drug) + ": $name",
            onSaveSuccess = onSaveSuccess
        )
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

            ReminderSection(
                setReminder = setReminder,
                onSetReminderChange = { setReminder = it },
                reminderTime = reminderTime,
                onReminderTimeChange = { reminderTime = it },
                isUpdate = existingEntry?.hasReminder == true
            )
        }
        
        SnackbarHost(hostState = snackbarHostState, modifier = Modifier.padding(innerPadding))
    }
}
