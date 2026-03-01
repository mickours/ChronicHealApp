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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddPainScreen(
    dateString: String? = null,
    locationString: String? = null,
    id: Long? = null,
    onBackClick: () -> Unit,
    onSaveSuccess: () -> Unit,
    viewModel: TimelineViewModel = hiltViewModel()
) {
    var intensity by remember { mutableFloatStateOf(5f) }
    var location by remember { mutableStateOf(locationString ?: "") }
    var note by remember { mutableStateOf("") }
    var startTime by remember { mutableStateOf(LocalTime.now()) }
    var durationMinutes by remember { mutableIntStateOf(EntryType.PAIN.defaultDurationMinutes) }
    var existingEntry by remember { mutableStateOf<HealthEntry?>(null) }

    LaunchedEffect(id) {
        if (id != null) {
            val entry = viewModel.getEntryById(id)
            if (entry != null) {
                existingEntry = entry
                intensity = entry.intensity?.toFloat() ?: 5f
                location = entry.location ?: ""
                note = entry.note
                startTime = entry.timestamp.atZone(ZoneId.systemDefault()).toLocalTime()
                durationMinutes = entry.durationMinutes ?: EntryType.PAIN.defaultDurationMinutes
            }
        }
    }

    AddEntryScaffold(
        title = if (id == null) "Log Pain" else "Edit Pain",
        id = id,
        onBackClick = onBackClick,
        onDeleteClick = {
            existingEntry?.let { viewModel.deleteEntry(it) }
            onSaveSuccess()
        },
        onSaveClick = {
            val date = if (dateString != null) LocalDate.parse(dateString) else LocalDate.now()
            val timestamp = date.atTime(startTime).atZone(ZoneId.systemDefault()).toInstant()

            val entry = HealthEntry(
                id = id ?: 0,
                timestamp = timestamp,
                type = EntryType.PAIN,
                intensity = intensity.roundToInt(),
                location = location,
                note = note,
                isFinished = existingEntry?.isFinished ?: false,
                durationMinutes = durationMinutes
            )

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
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp)
        ) {
            EntryTimeAndDurationPicker(
                startTime = startTime,
                onStartTimeChange = { startTime = it },
                durationMinutes = durationMinutes,
                onDurationChange = { durationMinutes = it }
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Intensity: ${intensity.roundToInt()}/10",
                style = MaterialTheme.typography.titleMedium
            )
            Slider(
                value = intensity,
                onValueChange = { intensity = it },
                valueRange = 1f..10f,
                steps = 8
            )

            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = location,
                onValueChange = { location = it },
                label = { Text("Location (e.g. Back, Knee)") },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = note,
                onValueChange = { note = it },
                label = { Text("Notes") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 3
            )
        }
    }
}
