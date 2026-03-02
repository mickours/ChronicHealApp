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
fun AddPeriodScreen(
    dateString: String? = null,
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

    PeriodEntryFormWrapper(
        dateString = dateString,
        id = id,
        existingEntry = existingEntry,
        onBackClick = onBackClick,
        onSaveSuccess = onSaveSuccess,
        viewModel = viewModel
    )
}

@Composable
fun PeriodEntryFormWrapper(
    dateString: String? = null,
    id: Long? = null,
    existingEntry: HealthEntry? = null,
    onBackClick: () -> Unit,
    onSaveSuccess: () -> Unit,
    viewModel: TimelineViewModel
) {
    var flowIntensity by remember { mutableFloatStateOf(3f) }
    var note by remember { mutableStateOf("") }
    var logDate by remember { mutableStateOf(if (dateString != null) LocalDate.parse(dateString) else LocalDate.now()) }
    var startTime by remember { mutableStateOf(LocalTime.now()) }

    LaunchedEffect(existingEntry) {
        existingEntry?.let { entry ->
            flowIntensity = entry.intensity?.toFloat() ?: 3f
            note = entry.note
            logDate = entry.timestamp.atZone(ZoneId.systemDefault()).toLocalDate()
            startTime = entry.timestamp.atZone(ZoneId.systemDefault()).toLocalTime()
        }
    }

    val createEntry = {
        HealthEntry(
            id = id ?: 0,
            timestamp = logDate.atTime(startTime).atZone(ZoneId.systemDefault()).toInstant(),
            type = EntryType.PERIOD,
            intensity = flowIntensity.roundToInt(),
            note = note,
            durationMinutes = existingEntry?.durationMinutes
        )
    }

    AddEntryScaffold(
        title = if (id == null) "Log Period" else "Edit Period",
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
        PeriodEntryForm(
            modifier = Modifier.padding(innerPadding),
            flowIntensity = flowIntensity,
            onFlowChange = { flowIntensity = it },
            note = note,
            onNoteChange = { note = it },
            logDate = logDate,
            onDateChange = { logDate = it },
            startTime = startTime,
            onStartTimeChange = { startTime = it }
        )
    }
}

@Composable
fun PeriodEntryForm(
    modifier: Modifier = Modifier,
    flowIntensity: Float,
    onFlowChange: (Float) -> Unit,
    note: String,
    onNoteChange: (String) -> Unit,
    logDate: LocalDate,
    onDateChange: (LocalDate) -> Unit,
    startTime: LocalTime,
    onStartTimeChange: (LocalTime) -> Unit
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
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
            text = "Flow Intensity: ${flowIntensity.roundToInt()}/5",
            style = MaterialTheme.typography.titleMedium
        )
        Slider(
            value = flowIntensity,
            onValueChange = onFlowChange,
            valueRange = 1f..5f,
            steps = 3
        )

        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = note,
            onValueChange = onNoteChange,
            label = { Text("Notes (e.g. cramps, mood)") },
            modifier = Modifier.fillMaxWidth(),
            minLines = 3
        )
    }
}
