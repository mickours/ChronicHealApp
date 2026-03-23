package org.chronicheal.app.presentation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import org.chronicheal.app.R
import org.chronicheal.app.domain.model.EntryType
import org.chronicheal.app.domain.model.HealthEntry
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddActivityScreen(
    dateString: String? = null,
    id: Long? = null,
    reminderId: Long? = null,
    templateId: Long? = null,
    onBackClick: () -> Unit,
    onSaveSuccess: () -> Unit,
    viewModel: TimelineViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    var name by rememberSaveable { mutableStateOf("") }
    var logDate by rememberSaveable { mutableStateOf(if (dateString != null) LocalDate.parse(dateString) else LocalDate.now()) }
    var startTime by rememberSaveable { mutableStateOf(LocalTime.now()) }
    var durationHours by rememberSaveable { mutableIntStateOf(0) }
    var durationMinutes by rememberSaveable { mutableIntStateOf(30) }
    var intensity by rememberSaveable { mutableFloatStateOf(3f) }
    var note by rememberSaveable { mutableStateOf("") }
    var existingEntry by remember { mutableStateOf<HealthEntry?>(null) }
    var isNewFromTemplate by remember { mutableStateOf(false) }

    var setReminder by rememberSaveable { mutableStateOf(false) }
    var reminderTime by rememberSaveable { mutableStateOf(LocalTime.now()) }

    val nameSuggestions by viewModel.activitySuggestions.collectAsState()

    LogNowEffect(
        id = id, 
        reminderId = reminderId,
        viewModel = viewModel,
        onEntryFound = { entry, fromTemplate ->
            existingEntry = entry
            isNewFromTemplate = fromTemplate
            name = entry.name ?: ""
            if (!isNewFromTemplate) {
                logDate = entry.timestamp.atZone(ZoneId.systemDefault()).toLocalDate()
                startTime = entry.timestamp.atZone(ZoneId.systemDefault()).toLocalTime()
            }
            val totalMinutes = entry.durationMinutes ?: 30
            durationHours = totalMinutes / 60
            durationMinutes = totalMinutes % 60
            intensity = entry.intensity?.toFloat() ?: 3f
            note = entry.note
            setReminder = entry.hasReminder
        },
        onReminderTimeFound = { reminderTime = it }
    )

    val createEntry = {
        HealthEntry(
            id = if (isNewFromTemplate) 0 else (existingEntry?.id ?: 0),
            timestamp = logDate.atTime(startTime).atZone(ZoneId.systemDefault()).toInstant(),
            type = EntryType.ACTIVITY,
            name = name.trim(),
            durationMinutes = (durationHours * 60) + durationMinutes,
            intensity = intensity.roundToInt(),
            note = note,
            hasReminder = setReminder,
            reminderId = existingEntry?.reminderId
        )
    }

    val handleSave = {
        handleEntrySave(
            viewModel = viewModel,
            existingEntry = existingEntry,
            isNewFromTemplate = isNewFromTemplate,
            currentEntry = createEntry(),
            setReminder = setReminder,
            reminderTime = reminderTime,
            reminderTitle = context.getString(R.string.type_activity) + ": $name",
            onSaveSuccess = onSaveSuccess
        )
    }

    AddEntryScaffold(
        title = if (id == null || isNewFromTemplate) stringResource(R.string.log_activity) else stringResource(R.string.edit_activity),
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

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(Icons.Default.Timer, contentDescription = null)
                Text(stringResource(R.string.duration_label) + ":", style = MaterialTheme.typography.titleMedium)
                
                OutlinedTextField(
                    value = durationHours.toString(),
                    onValueChange = { durationHours = it.toIntOrNull() ?: 0 },
                    label = { Text("H") },
                    modifier = Modifier.weight(1f),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )
                
                OutlinedTextField(
                    value = durationMinutes.toString(),
                    onValueChange = { durationMinutes = it.toIntOrNull() ?: 0 },
                    label = { Text("M") },
                    modifier = Modifier.weight(1f),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            AutoCompleteTextField(
                value = name,
                onValueChange = { name = it },
                suggestions = nameSuggestions,
                label = stringResource(R.string.name_label)
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = stringResource(R.string.intensity_label, intensity.roundToInt()),
                style = MaterialTheme.typography.titleMedium
            )
            Slider(
                value = intensity,
                onValueChange = { intensity = it },
                valueRange = 1f..10f,
                steps = 8
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
    }
}
