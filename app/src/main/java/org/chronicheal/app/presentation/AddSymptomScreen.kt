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
fun AddSymptomScreen(
    dateString: String? = null,
    locationString: String? = null,
    id: Long? = null,
    onBackClick: () -> Unit,
    onSaveSuccess: () -> Unit,
    viewModel: TimelineViewModel = hiltViewModel()
) {
    var name by remember { mutableStateOf("") }
    var severity by remember { mutableFloatStateOf(3f) }
    var location by remember { mutableStateOf(locationString ?: "") }
    var note by remember { mutableStateOf("") }
    var existingEntry by remember { mutableStateOf<HealthEntry?>(null) }

    LaunchedEffect(id) {
        if (id != null) {
            val entry = viewModel.getEntryById(id)
            if (entry != null) {
                existingEntry = entry
                name = entry.name ?: ""
                severity = entry.intensity?.toFloat() ?: 3f
                location = entry.location ?: ""
                note = entry.note
            }
        }
    }

    AddEntryScaffold(
        title = if (id == null) "Log Symptom" else "Edit Symptom",
        id = id,
        onBackClick = onBackClick,
        onDeleteClick = {
            existingEntry?.let { viewModel.deleteEntry(it) }
            onSaveSuccess()
        },
        onSaveClick = {
            val timestamp = if (id == null) {
                if (dateString != null) {
                    LocalDate.parse(dateString).atTime(LocalTime.now()).atZone(ZoneId.systemDefault()).toInstant()
                } else {
                    java.time.Instant.now()
                }
            } else {
                existingEntry?.timestamp ?: java.time.Instant.now()
            }

            val entry = HealthEntry(
                id = id ?: 0,
                timestamp = timestamp,
                type = EntryType.SYMPTOM,
                name = name,
                intensity = severity.roundToInt(),
                location = location,
                note = note,
                isFinished = existingEntry?.isFinished ?: false,
                durationMinutes = existingEntry?.durationMinutes
            )

            if (id == null) {
                viewModel.addEntry(entry)
            } else {
                viewModel.updateEntry(entry)
            }
            onSaveSuccess()
        },
        saveButtonEnabled = name.isNotBlank(),
        viewModel = viewModel
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp)
        ) {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Symptom Name (e.g. Fatigue, Nausea)") },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Severity: ${severity.roundToInt()}/10",
                style = MaterialTheme.typography.titleMedium
            )
            Slider(
                value = severity,
                onValueChange = { severity = it },
                valueRange = 1f..10f,
                steps = 8
            )

            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = location,
                onValueChange = { location = it },
                label = { Text("Location (Optional)") },
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
