package org.chronicheal.app.presentation

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
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
fun AddSymptomScreen(
    dateString: String? = null,
    locationString: String? = null,
    id: Long? = null,
    reminderId: Long? = null,
    onBackClick: () -> Unit,
    onSaveSuccess: () -> Unit,
    viewModel: TimelineViewModel = hiltViewModel()
) {
    var name by rememberSaveable { mutableStateOf("") }
    var severity by rememberSaveable { mutableFloatStateOf(3f) }
    var location by rememberSaveable { mutableStateOf(locationString ?: "") }
    var note by rememberSaveable { mutableStateOf("") }
    var logDate by rememberSaveable { mutableStateOf(if (dateString != null) LocalDate.parse(dateString) else LocalDate.now()) }
    var startTime by rememberSaveable { mutableStateOf(LocalTime.now()) }
    var existingEntry by remember { mutableStateOf<HealthEntry?>(null) }
    var isNewFromTemplate by remember { mutableStateOf(false) }

    val nameSuggestions by viewModel.symptomSuggestions.collectAsState()
    val locationSuggestions by viewModel.painLocationSuggestions.collectAsState()

    LogNowEffect(
        id = id, 
        reminderId = reminderId,
        viewModel = viewModel,
        onEntryFound = { entry, fromTemplate ->
            existingEntry = entry
            isNewFromTemplate = fromTemplate
            name = entry.name ?: ""
            severity = entry.intensity?.toFloat() ?: 3f
            location = entry.location ?: ""
            note = entry.note
            if (!isNewFromTemplate) {
                logDate = entry.timestamp.atZone(ZoneId.systemDefault()).toLocalDate()
                startTime = entry.timestamp.atZone(ZoneId.systemDefault()).toLocalTime()
            }
        }
    )

    val createEntry = {
        HealthEntry(
            id = if (isNewFromTemplate) 0 else (existingEntry?.id ?: 0),
            timestamp = logDate.atTime(startTime).atZone(ZoneId.systemDefault()).toInstant(),
            type = EntryType.SYMPTOM,
            name = name.trim(),
            intensity = severity.roundToInt(),
            location = location.trim(),
            note = note,
            durationMinutes = existingEntry?.durationMinutes
        )
    }

    AddEntryScaffold(
        title = if (id == null || isNewFromTemplate) stringResource(R.string.log_symptom) else stringResource(R.string.edit_symptom),
        existingEntry = if (isNewFromTemplate) null else existingEntry,
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
                label = stringResource(R.string.symptom_name_label)
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = stringResource(R.string.intensity_label, severity.roundToInt()),
                style = MaterialTheme.typography.titleMedium
            )
            Slider(
                value = severity,
                onValueChange = { severity = it },
                valueRange = 1f..10f,
                steps = 8
            )

            Spacer(modifier = Modifier.height(16.dp))

            AutoCompleteTextField(
                value = location,
                onValueChange = { location = it },
                suggestions = locationSuggestions,
                label = stringResource(R.string.location_label)
            )

            Spacer(modifier = Modifier.height(16.dp))

            VoiceEnabledTextField(
                value = note,
                onValueChange = { note = it },
                label = stringResource(R.string.notes_label),
                minLines = 3
            )
        }
    }
}
