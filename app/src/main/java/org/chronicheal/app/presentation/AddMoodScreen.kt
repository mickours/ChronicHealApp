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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
fun AddMoodScreen(
    dateString: String? = null,
    id: Long? = null,
    reminderId: Long? = null,
    templateId: Long? = null,
    onBackClick: () -> Unit,
    onSaveSuccess: () -> Unit,
    viewModel: TimelineViewModel = hiltViewModel()
) {
    var moodLevel by rememberSaveable { mutableFloatStateOf(5f) }
    var note by rememberSaveable { mutableStateOf("") }
    var logDate by rememberSaveable { mutableStateOf(if (dateString != null) LocalDate.parse(dateString) else LocalDate.now()) }
    var startTime by rememberSaveable { mutableStateOf(LocalTime.now()) }
    var existingEntry by remember { mutableStateOf<HealthEntry?>(null) }
    var isNewFromTemplate by remember { mutableStateOf(false) }

    LogNowEffect(
        id = id, 
        reminderId = reminderId,
        viewModel = viewModel,
        onEntryFound = { entry, fromTemplate ->
            existingEntry = entry
            isNewFromTemplate = fromTemplate
            moodLevel = entry.intensity?.toFloat() ?: 5f
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
            type = EntryType.MOOD,
            intensity = moodLevel.roundToInt(),
            note = note,
            durationMinutes = existingEntry?.durationMinutes
        )
    }

    val moodSmiley = when (moodLevel.roundToInt()) {
        1 -> "😫"
        2 -> "☹️"
        3 -> "🙁"
        4 -> "😐"
        5 -> "🙂"
        6 -> "😊"
        7 -> "😁"
        8 -> "😄"
        9 -> "🥳"
        10 -> "🤩"
        else -> "😐"
    }

    val moodLabel = when (moodLevel.roundToInt()) {
        in 1..2 -> stringResource(R.string.mood_very_bad)
        in 3..4 -> stringResource(R.string.mood_bad)
        in 5..6 -> stringResource(R.string.mood_neutral)
        in 7..8 -> stringResource(R.string.mood_good)
        else -> stringResource(R.string.mood_amazing)
    }

    AddEntryScaffold(
        title = if (id == null || isNewFromTemplate) stringResource(R.string.log_mood) else stringResource(R.string.edit_mood),
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
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            EntryDateTimePicker(
                date = logDate,
                onDateChange = { logDate = it },
                startTime = startTime,
                onStartTimeChange = { startTime = it }
            )

            Spacer(modifier = Modifier.height(32.dp))

            Text(
                text = moodSmiley,
                fontSize = 80.sp
            )
            
            Text(
                text = moodLabel,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = stringResource(R.string.intensity_label, moodLevel.roundToInt()),
                style = MaterialTheme.typography.titleMedium
            )
            Slider(
                value = moodLevel,
                onValueChange = { moodLevel = it },
                valueRange = 1f..10f,
                steps = 8,
                modifier = Modifier.padding(horizontal = 16.dp)
            )

            Spacer(modifier = Modifier.height(32.dp))

            VoiceEnabledTextField(
                value = note,
                onValueChange = { note = it },
                label = stringResource(R.string.section_mood),
                minLines = 3
            )
        }
    }
}
