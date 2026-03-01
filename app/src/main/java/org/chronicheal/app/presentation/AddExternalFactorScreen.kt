package org.chronicheal.app.presentation

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
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
fun AddExternalFactorScreen(
    dateString: String? = null,
    id: Long? = null,
    onBackClick: () -> Unit,
    onSaveSuccess: () -> Unit,
    viewModel: TimelineViewModel = hiltViewModel()
) {
    var factorName by remember { mutableStateOf("") }
    var intensity by remember { mutableFloatStateOf(5f) }
    var note by remember { mutableStateOf("") }
    var existingEntry by remember { mutableStateOf<HealthEntry?>(null) }

    LaunchedEffect(id) {
        if (id != null) {
            val entry = viewModel.getEntryById(id)
            if (entry != null) {
                existingEntry = entry
                factorName = entry.name ?: ""
                intensity = entry.intensity?.toFloat() ?: 5f
                note = entry.note
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (id == null) "Log External Factor" else "Edit External Factor") },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
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
            OutlinedTextField(
                value = factorName,
                onValueChange = { factorName = it },
                label = { Text("Factor Name (e.g. Weather, Stress, Noise)") },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Impact/Intensity: ${intensity.roundToInt()}/10",
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
                value = note,
                onValueChange = { note = it },
                label = { Text("Notes/Impact") },
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
                        type = EntryType.EXTERNAL_FACTOR,
                        name = factorName,
                        intensity = intensity.roundToInt(),
                        note = note
                    )

                    if (id == null) {
                        viewModel.addEntry(entry)
                    } else {
                        viewModel.updateEntry(entry)
                    }
                    onSaveSuccess()
                },
                enabled = factorName.isNotBlank(),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(if (id == null) "Save" else "Update")
            }
        }
    }
}
