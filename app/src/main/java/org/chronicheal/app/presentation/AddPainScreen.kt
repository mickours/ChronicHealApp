package org.chronicheal.app.presentation

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
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
    var existingEntry by remember { mutableStateOf<HealthEntry?>(null) }
    var showDeleteConfirmation by remember { mutableStateOf(false) }

    LaunchedEffect(id) {
        if (id != null) {
            val entry = viewModel.getEntryById(id)
            if (entry != null) {
                existingEntry = entry
                intensity = entry.intensity?.toFloat() ?: 5f
                location = entry.location ?: ""
                note = entry.note
            }
        }
    }

    val handleBack = {
        if (id != null) {
            viewModel.showMessage("Edition canceled")
        }
        onBackClick()
    }

    BackHandler(onBack = handleBack)

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (id == null) "Log Pain" else "Edit Pain") },
                navigationIcon = {
                    IconButton(onClick = handleBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    if (id != null) {
                        IconButton(onClick = { showDeleteConfirmation = true }) {
                            Icon(Icons.Default.Delete, contentDescription = "Delete")
                        }
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp)
        ) {
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

            Spacer(modifier = Modifier.weight(1f))

            Button(
                onClick = {
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
                        type = EntryType.PAIN,
                        intensity = intensity.roundToInt(),
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
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(if (id == null) "Save" else "Update")
            }
        }

        if (showDeleteConfirmation) {
            AlertDialog(
                onDismissRequest = { showDeleteConfirmation = false },
                title = { Text("Delete Entry") },
                text = { Text("Are you sure you want to delete this pain log?") },
                confirmButton = {
                    TextButton(
                        onClick = {
                            existingEntry?.let { viewModel.deleteEntry(it) }
                            showDeleteConfirmation = false
                            onSaveSuccess()
                        }
                    ) {
                        Text("Delete", color = MaterialTheme.colorScheme.error)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showDeleteConfirmation = false }) {
                        Text("Cancel")
                    }
                }
            )
        }
    }
}
