package org.chronicheal.app.presentation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
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
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddCompleteEntryScreen(
    dateString: String? = null,
    onBackClick: () -> Unit,
    onSaveSuccess: () -> Unit,
    viewModel: TimelineViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    var logDate by rememberSaveable { mutableStateOf(if (dateString != null) LocalDate.parse(dateString) else LocalDate.now()) }
    var startTime by rememberSaveable { mutableStateOf(LocalTime.now()) }

    // Mood State
    var moodIntensity by rememberSaveable { mutableFloatStateOf(5f) }
    var moodNote by rememberSaveable { mutableStateOf("") }

    // Pain State
    var painIntensity by rememberSaveable { mutableFloatStateOf(0f) }
    var painLocation by rememberSaveable { mutableStateOf("") }
    var painNote by rememberSaveable { mutableStateOf("") }

    // Sleep State
    var sleepDurationHours by rememberSaveable { mutableStateOf("") }
    var sleepQuality by rememberSaveable { mutableFloatStateOf(5f) }

    // Medication State
    var medicationName by rememberSaveable { mutableStateOf("") }
    var medicationDosage by rememberSaveable { mutableStateOf("") }

    // General Note
    var generalNote by rememberSaveable { mutableStateOf("") }

    val handleSave = {
        val timestamp = logDate.atTime(startTime).atZone(ZoneId.systemDefault()).toInstant()
        val entries = mutableListOf<HealthEntry>()

        // Add Mood
        entries.add(
            HealthEntry(
                timestamp = timestamp,
                type = EntryType.MOOD,
                intensity = moodIntensity.roundToInt(),
                note = moodNote
            )
        )

        // Add Pain if recorded
        if (painIntensity > 0) {
            entries.add(
                HealthEntry(
                    timestamp = timestamp,
                    type = EntryType.PAIN,
                    intensity = painIntensity.roundToInt(),
                    location = painLocation.ifBlank { null },
                    note = painNote
                )
            )
        }

        // Add Sleep if recorded
        val sleepMins = sleepDurationHours.toIntOrNull()?.let { it * 60 }
        if (sleepMins != null || sleepQuality != 5f) {
            entries.add(
                HealthEntry(
                    timestamp = timestamp,
                    type = EntryType.SLEEP,
                    durationMinutes = sleepMins,
                    intensity = sleepQuality.roundToInt()
                )
            )
        }

        // Add Medication if recorded
        if (medicationName.isNotBlank()) {
            entries.add(
                HealthEntry(
                    timestamp = timestamp,
                    type = EntryType.DRUG,
                    name = medicationName,
                    unit = medicationDosage,
                    note = ""
                )
            )
        }

        // Add General Journal if recorded
        if (generalNote.isNotBlank()) {
            entries.add(
                HealthEntry(
                    timestamp = timestamp,
                    type = EntryType.JOURNAL,
                    note = generalNote
                )
            )
        }

        // Save all
        entries.forEach { viewModel.addEntry(it) }
        viewModel.showMessage(context.getString(R.string.entry_saved))
        onSaveSuccess()
    }

    AddEntryScaffold(
        title = stringResource(R.string.complete_checkin),
        existingEntry = null,
        currentEntry = { HealthEntry(type = EntryType.JOURNAL) }, // Dummy
        onBackClick = onBackClick,
        onSaveSuccess = onSaveSuccess,
        onDeleteClick = {},
        viewModel = viewModel,
        onSave = handleSave
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            EntryDateTimePicker(
                date = logDate,
                onDateChange = { logDate = it },
                startTime = startTime,
                onStartTimeChange = { startTime = it }
            )

            SectionHeader(type = EntryType.MOOD, title = stringResource(R.string.section_mood))
            MoodSection(
                intensity = moodIntensity,
                onIntensityChange = { moodIntensity = it },
                note = moodNote,
                onNoteChange = { moodNote = it }
            )

            SectionHeader(type = EntryType.PAIN, title = stringResource(R.string.section_pain))
            PainSection(
                intensity = painIntensity,
                onIntensityChange = { painIntensity = it },
                location = painLocation,
                onLocationChange = { painLocation = it },
                note = painNote,
                onNoteChange = { painNote = it },
                viewModel = viewModel
            )

            SectionHeader(type = EntryType.SLEEP, title = stringResource(R.string.section_sleep))
            SleepSection(
                durationHours = sleepDurationHours,
                onDurationChange = { sleepDurationHours = it },
                quality = sleepQuality,
                onQualityChange = { sleepQuality = it }
            )

            SectionHeader(type = EntryType.DRUG, title = stringResource(R.string.section_medication))
            MedicationSection(
                name = medicationName,
                onNameChange = { medicationName = it },
                dosage = medicationDosage,
                onDosageChange = { medicationDosage = it },
                viewModel = viewModel
            )

            SectionHeader(type = EntryType.JOURNAL, title = stringResource(R.string.section_anything_else))
            VoiceEnabledTextField(
                value = generalNote,
                onValueChange = { generalNote = it },
                label = stringResource(R.string.notes_label),
                minLines = 3
            )
            
            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@Composable
fun PainSection(
    intensity: Float,
    onIntensityChange: (Float) -> Unit,
    location: String,
    onLocationChange: (String) -> Unit,
    note: String,
    onNoteChange: (String) -> Unit,
    viewModel: TimelineViewModel
) {
    ElevatedCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(stringResource(R.string.intensity_label, intensity.roundToInt()))
            Slider(value = intensity, onValueChange = onIntensityChange, valueRange = 0f..10f, steps = 9)
            if (intensity > 0) {
                val suggestions by viewModel.painLocationSuggestions.collectAsState()
                AutoCompleteTextField(value = location, onValueChange = onLocationChange, suggestions = suggestions, label = stringResource(R.string.location_label))
                Spacer(Modifier.height(8.dp))
                VoiceEnabledTextField(value = note, onValueChange = onNoteChange, label = stringResource(R.string.section_pain))
            }
        }
    }
}

@Composable
fun SleepSection(
    durationHours: String,
    onDurationChange: (String) -> Unit,
    quality: Float,
    onQualityChange: (Float) -> Unit
) {
    ElevatedCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            VoiceEnabledTextField(value = durationHours, onValueChange = onDurationChange, label = stringResource(R.string.duration_hours_label))
            Spacer(Modifier.height(8.dp))
            Text(stringResource(R.string.quality_label, quality.roundToInt()))
            Slider(value = quality, onValueChange = onQualityChange, valueRange = 1f..10f, steps = 9)
        }
    }
}

@Composable
fun MedicationSection(
    name: String,
    onNameChange: (String) -> Unit,
    dosage: String,
    onDosageChange: (String) -> Unit,
    viewModel: TimelineViewModel
) {
    ElevatedCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            val suggestions by viewModel.drugSuggestions.collectAsState()
            AutoCompleteTextField(value = name, onValueChange = onNameChange, suggestions = suggestions, label = stringResource(R.string.name_label))
            Spacer(Modifier.height(8.dp))
            VoiceEnabledTextField(value = dosage, onValueChange = onDosageChange, label = stringResource(R.string.dosage_label))
        }
    }
}
