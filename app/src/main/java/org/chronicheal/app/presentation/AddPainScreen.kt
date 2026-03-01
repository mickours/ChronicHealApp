package org.chronicheal.app.presentation

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import org.chronicheal.app.domain.model.EntryType
import org.chronicheal.app.domain.model.HealthEntry
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import kotlin.math.roundToInt

@Composable
fun AddPainScreen(
    dateString: String? = null,
    locationString: String? = null,
    id: Long? = null,
    onBackClick: () -> Unit,
    onSaveSuccess: () -> Unit,
    viewModel: TimelineViewModel = hiltViewModel()
) {
    var existingEntry by remember { mutableStateOf<HealthEntry?>(null) }
    
    LaunchedEffect(id) {
        if (id != null) {
            existingEntry = viewModel.getEntryById(id)
        }
    }

    PainEntryFormWrapper(
        dateString = dateString,
        locationString = locationString,
        id = id,
        existingEntry = existingEntry,
        onBackClick = onBackClick,
        onSaveSuccess = onSaveSuccess,
        viewModel = viewModel
    )
}

@Composable
fun PainEntryFormWrapper(
    dateString: String? = null,
    locationString: String? = null,
    id: Long? = null,
    existingEntry: HealthEntry? = null,
    onBackClick: () -> Unit,
    onSaveSuccess: () -> Unit,
    viewModel: TimelineViewModel
) {
    var intensity by remember { mutableFloatStateOf(5f) }
    var location by remember { mutableStateOf(locationString ?: "") }
    var note by remember { mutableStateOf("") }
    var logDate by remember { mutableStateOf(if (dateString != null) LocalDate.parse(dateString) else LocalDate.now()) }
    var startTime by remember { mutableStateOf(LocalTime.now()) }

    LaunchedEffect(existingEntry) {
        existingEntry?.let { entry ->
            intensity = entry.intensity?.toFloat() ?: 5f
            location = entry.location ?: ""
            note = entry.note
            logDate = entry.timestamp.atZone(ZoneId.systemDefault()).toLocalDate()
            startTime = entry.timestamp.atZone(ZoneId.systemDefault()).toLocalTime()
        }
    }

    val createEntry = {
        HealthEntry(
            id = id ?: 0,
            timestamp = logDate.atTime(startTime).atZone(ZoneId.systemDefault()).toInstant(),
            type = EntryType.PAIN,
            intensity = intensity.roundToInt(),
            location = location.trim(),
            note = note,
            durationMinutes = existingEntry?.durationMinutes
        )
    }

    AddEntryScaffold(
        title = if (id == null) "Log Pain" else "Edit Pain",
        existingEntry = existingEntry,
        currentEntry = createEntry,
        onBackClick = onBackClick,
        onDeleteClick = {
            existingEntry?.let { viewModel.deleteEntry(it) }
            onSaveSuccess()
        },
        onSaveClick = {
            val entry = createEntry()
            if (id == null) {
                viewModel.addEntry(entry)
            } else {
                viewModel.updateEntry(entry)
            }
            onSaveSuccess()
        },
        saveButtonEnabled = true,
        viewModel = viewModel
    ) { innerPadding ->
        PainEntryForm(
            modifier = Modifier.padding(innerPadding),
            intensity = intensity,
            onIntensityChange = { intensity = it },
            location = location,
            onLocationChange = { location = it },
            note = note,
            onNoteChange = { note = it },
            logDate = logDate,
            onDateChange = { logDate = it },
            startTime = startTime,
            onStartTimeChange = { startTime = it },
            viewModel = viewModel
        )
    }
}

@Composable
fun PainEntryForm(
    modifier: Modifier = Modifier,
    intensity: Float,
    onIntensityChange: (Float) -> Unit,
    location: String,
    onLocationChange: (String) -> Unit,
    note: String,
    onNoteChange: (String) -> Unit,
    logDate: LocalDate,
    onDateChange: (LocalDate) -> Unit,
    startTime: LocalTime,
    onStartTimeChange: (LocalTime) -> Unit,
    viewModel: TimelineViewModel
) {
    val locationSuggestions by viewModel.painLocationSuggestions.collectAsState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        EntryDateTimePicker(
            date = logDate,
            onDateChange = onDateChange,
            startTime = startTime,
            onStartTimeChange = onStartTimeChange
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "Intensity: ${intensity.roundToInt()}/10",
            style = MaterialTheme.typography.titleMedium
        )
        Slider(
            value = intensity,
            onValueChange = onIntensityChange,
            valueRange = 1f..10f,
            steps = 8
        )

        Spacer(modifier = Modifier.height(16.dp))

        AutoCompleteTextField(
            value = location,
            onValueChange = onLocationChange,
            suggestions = locationSuggestions,
            label = "Location (e.g. Back, Knee)"
        )

        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = note,
            onValueChange = onNoteChange,
            label = { Text("Notes") },
            modifier = Modifier.fillMaxWidth(),
            minLines = 3
        )
    }
}
