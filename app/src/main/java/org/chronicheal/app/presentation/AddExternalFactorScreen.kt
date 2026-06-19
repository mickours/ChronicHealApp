package org.chronicheal.app.presentation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
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
import org.chronicheal.app.domain.usecase.GetSuggestionsUseCase
import org.chronicheal.app.presentation.components.AddEntryScaffold
import org.chronicheal.app.presentation.components.AutoCompleteTextField
import org.chronicheal.app.presentation.components.EntryDateTimePicker
import org.chronicheal.app.presentation.components.IntensityField
import org.chronicheal.app.presentation.components.LogNowEffect
import org.chronicheal.app.presentation.components.VoiceEnabledTextField
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddExternalFactorScreen(
    dateString: String? = null,
    id: Long? = null,
    reminderId: Long? = null,
    templateId: Long? = null,
    onBackClick: () -> Unit,
    onSaveSuccess: () -> Unit,
    viewModel: AddEntryViewModel = hiltViewModel(),
) {
    var name by rememberSaveable { mutableStateOf("") }
    var intensity by rememberSaveable { mutableIntStateOf(5) }
    var note by rememberSaveable { mutableStateOf("") }
    var logDate by rememberSaveable { mutableStateOf(if (dateString != null) LocalDate.parse(dateString) else LocalDate.now()) }
    var startTime by rememberSaveable { mutableStateOf(LocalTime.now()) }

    val uiState by viewModel.uiState.collectAsState()
    val existingEntry = uiState.entry
    val isNewFromTemplate = uiState.isNewFromTemplate

    val nameSuggestions by viewModel.getSuggestions(
        setOf(EntryType.EXTERNAL_FACTOR),
        GetSuggestionsUseCase.SuggestionField.NAME,
    ).collectAsState()

    LogNowEffect(
        id = id, 
        reminderId = reminderId,
        templateId = templateId,
        viewModel = viewModel,
        onEntryFound = { entry, fromTemplate ->
            name = entry.name ?: ""
            intensity = entry.intensity ?: 5
            note = entry.note
            if (!fromTemplate) {
                logDate = entry.timestamp.atZone(ZoneId.systemDefault()).toLocalDate()
                startTime = entry.timestamp.atZone(ZoneId.systemDefault()).toLocalTime()
            }
        }
    )

    val createEntry = {
        HealthEntry(
            id = if (isNewFromTemplate) 0 else (existingEntry?.id ?: 0),
            timestamp = logDate.atTime(startTime).atZone(ZoneId.systemDefault()).toInstant(),
            type = EntryType.EXTERNAL_FACTOR,
            name = name.trim(),
            intensity = intensity,
            note = note,
            durationMinutes = existingEntry?.durationMinutes
        )
    }

    AddEntryScaffold(
        title = if ((id == null) || isNewFromTemplate) stringResource(R.string.log_external_factor) else stringResource(
            R.string.edit_external_factor
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

            AutoCompleteTextField(
                value = name,
                onValueChange = { name = it },
                suggestions = nameSuggestions,
                label = stringResource(R.string.external_factor_name_label)
            )

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
                IntensityField(intensity = intensity, onIntensityChange = { intensity = it ?: 5 })
            }
        }
    }
}
