package org.chronicheal.app.presentation

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
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
fun AddSleepScreen(
    dateString: String? = null,
    onBackClick: () -> Unit,
    onSaveSuccess: () -> Unit,
    viewModel: TimelineViewModel = hiltViewModel()
) {
    var durationHours by remember { mutableStateOf("") }
    var quality by remember { mutableFloatStateOf(3f) }
    var note by remember { mutableStateOf("") }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Log Sleep") },
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
                value = durationHours,
                onValueChange = { durationHours = it },
                label = { Text("Duration (Hours)") },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Quality: ${quality.roundToInt()}/5",
                style = MaterialTheme.typography.titleMedium
            )
            Slider(
                value = quality,
                onValueChange = { quality = it },
                valueRange = 1f..5f,
                steps = 3
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
                    val timestamp = if (dateString != null) {
                        LocalDate.parse(dateString).atTime(LocalTime.now()).atZone(ZoneId.systemDefault()).toInstant()
                    } else {
                        java.time.Instant.now()
                    }
                    viewModel.addEntry(
                        HealthEntry(
                            timestamp = timestamp,
                            type = EntryType.SLEEP,
                            value = durationHours.toDoubleOrNull(),
                            intensity = quality.roundToInt(),
                            note = note
                        )
                    )
                    onSaveSuccess()
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Save")
            }
        }
    }
}
