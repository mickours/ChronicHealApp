package org.chronicheal.app.presentation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import org.chronicheal.app.R
import org.chronicheal.app.domain.model.EntryType
import org.chronicheal.app.domain.model.HealthEntry
import org.chronicheal.app.domain.model.Reminder
import java.time.Duration
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
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

    // Reminder State
    var isReminderEnabled by rememberSaveable { mutableStateOf(false) }
    var reminderTime by rememberSaveable { mutableStateOf(LocalTime.of(20, 0)) }
    
    val checkupReminders by viewModel.checkupReminders.collectAsState()
    
    LaunchedEffect(checkupReminders) {
        val existingReminder = checkupReminders.firstOrNull()
        if (existingReminder != null) {
            isReminderEnabled = existingReminder.isEnabled
            reminderTime = existingReminder.time
        }
    }

    // Mood State
    var moodIntensity by rememberSaveable { mutableFloatStateOf(5f) }
    var moodNote by rememberSaveable { mutableStateOf("") }

    // Pain State
    val painEntries = remember { mutableStateListOf<HealthEntry>() }
    var painNote by rememberSaveable { mutableStateOf("") }

    // Sleep State
    val now = remember { LocalTime.now() }
    val today = remember { LocalDate.now() }
    val defaultStartDateTime = remember(now, today) {
        if (now.hour in 5..11) today.minusDays(1).atTime(22, 0) else today.atTime(22, 0)
    }
    val defaultEndDateTime = remember(now, today) {
        if (now.hour in 5..11) today.atTime(7, 0) else today.plusDays(1).atTime(7, 0)
    }

    var sleepLogDate by rememberSaveable { mutableStateOf(defaultStartDateTime.toLocalDate()) }
    var sleepStartTime by rememberSaveable { mutableStateOf(defaultStartDateTime.toLocalTime()) }
    var sleepEndDate by rememberSaveable { mutableStateOf(defaultEndDateTime.toLocalDate()) }
    var sleepEndTime by rememberSaveable { mutableStateOf(defaultEndDateTime.toLocalTime()) }
    var sleepQuality by rememberSaveable { mutableFloatStateOf(5f) }

    val sleepDurationMinutes by remember(sleepLogDate, sleepStartTime, sleepEndDate, sleepEndTime) {
        derivedStateOf {
            val start = sleepLogDate.atTime(sleepStartTime).atZone(ZoneId.systemDefault())
            val end = sleepEndDate.atTime(sleepEndTime).atZone(ZoneId.systemDefault())
            Duration.between(start, end).toMinutes().toInt().coerceAtLeast(0)
        }
    }

    // Medication State
    val drugReminders by viewModel.drugReminders.collectAsState()
    val medicationTaken = remember { mutableStateListOf<Boolean>() }
    val medicationTimes = remember { mutableStateListOf<LocalTime>() }
    
    LaunchedEffect(drugReminders) {
        if (medicationTaken.size != drugReminders.size) {
            medicationTaken.clear()
            medicationTimes.clear()
            drugReminders.forEach { reminder ->
                medicationTaken.add(false)
                medicationTimes.add(reminder.time)
            }
        }
    }

    // Symptoms State
    val symptomEntries = remember { mutableStateListOf<SymptomEntryState>() }
    LaunchedEffect(Unit) {
        if (symptomEntries.isEmpty()) {
            symptomEntries.add(SymptomEntryState())
        }
    }

    // General Note
    var generalNote by rememberSaveable { mutableStateOf("") }

    val handleSave = {
        val timestamp = logDate.atTime(startTime).atZone(ZoneId.systemDefault()).toInstant()
        val entries = mutableListOf<HealthEntry>()

        // Update/Create Checkup Reminder
        val currentReminder = checkupReminders.firstOrNull()
        if (isReminderEnabled) {
            val reminder = (currentReminder ?: Reminder(
                title = "Checkup",
                time = reminderTime,
                daysOfWeek = (1..7).toSet(),
                isEnabled = true
            )).copy(time = reminderTime, isEnabled = true)
            viewModel.saveReminder(reminder)
        } else if (currentReminder != null) {
            viewModel.saveReminder(currentReminder.copy(isEnabled = false))
        }

        // Add Mood
        entries.add(
            HealthEntry(
                timestamp = timestamp,
                type = EntryType.MOOD,
                intensity = moodIntensity.roundToInt(),
                note = moodNote
            )
        )

        // Add Pains
        painEntries.forEach { pain ->
            entries.add(
                pain.copy(
                    timestamp = timestamp,
                    note = painNote
                )
            )
        }

        // Add Sleep if recorded
        if (sleepDurationMinutes > 0) {
            entries.add(
                HealthEntry(
                    timestamp = sleepLogDate.atTime(sleepStartTime).atZone(ZoneId.systemDefault()).toInstant(),
                    type = EntryType.SLEEP,
                    durationMinutes = sleepDurationMinutes,
                    intensity = sleepQuality.roundToInt()
                )
            )
        }

        // Add Medication from reminders
        drugReminders.forEachIndexed { index, reminder ->
            if (index < medicationTaken.size && medicationTaken[index]) {
                val medTime = if (index < medicationTimes.size) medicationTimes[index] else startTime
                entries.add(
                    HealthEntry(
                        timestamp = logDate.atTime(medTime).atZone(ZoneId.systemDefault()).toInstant(),
                        type = EntryType.DRUG,
                        name = reminder.title,
                        note = ""
                    )
                )
            }
        }

        // Add Symptoms
        symptomEntries.forEach { symptom ->
            if (symptom.name.isNotBlank()) {
                entries.add(
                    HealthEntry(
                        timestamp = timestamp,
                        type = EntryType.SYMPTOM,
                        name = symptom.name,
                        intensity = symptom.intensity.roundToInt(),
                        note = symptom.note
                    )
                )
            }
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
        currentEntry = { HealthEntry(type = EntryType.JOURNAL, timestamp = logDate.atTime(startTime).atZone(ZoneId.systemDefault()).toInstant()) },
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

            // Reminder Settings
            ElevatedCard(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.NotificationsActive, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            Spacer(Modifier.width(8.dp))
                            Text(text = stringResource(R.string.set_daily_reminder), style = MaterialTheme.typography.titleMedium)
                        }
                        Switch(checked = isReminderEnabled, onCheckedChange = { isReminderEnabled = it })
                    }
                    if (isReminderEnabled) {
                        Spacer(Modifier.height(8.dp))
                        HorizontalDivider()
                        Spacer(Modifier.height(8.dp))
                        var showTimePicker by remember { mutableStateOf(false) }
                        val timeFormatter = remember { DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT) }
                        
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(text = stringResource(R.string.reminder_time_label), style = MaterialTheme.typography.bodyMedium)
                            TextButton(onClick = { showTimePicker = true }) {
                                Text(text = reminderTime.format(timeFormatter))
                            }
                        }

                        if (showTimePicker) {
                            val timeState = rememberTimePickerState(initialHour = reminderTime.hour, initialMinute = reminderTime.minute)
                            TimePickerDialog(
                                onDismissRequest = { showTimePicker = false },
                                confirmButton = {
                                    TextButton(onClick = {
                                        reminderTime = LocalTime.of(timeState.hour, timeState.minute)
                                        showTimePicker = false
                                    }) { Text(stringResource(R.string.ok)) }
                                },
                                dismissButton = {
                                    TextButton(onClick = { showTimePicker = false }) {
                                        Text(stringResource(R.string.cancel))
                                    }
                                }
                            ) {
                                TimePicker(state = timeState)
                            }
                        }
                    }
                }
            }

            SectionHeader(type = EntryType.MOOD, title = stringResource(R.string.section_mood))
            MoodSection(
                intensity = moodIntensity,
                onIntensityChange = { moodIntensity = it },
                note = moodNote,
                onNoteChange = { moodNote = it }
            )

            SectionHeader(type = EntryType.PAIN, title = stringResource(R.string.section_pain))
            PainSection(
                pains = painEntries,
                note = painNote,
                onNoteChange = { painNote = it },
                logDate = logDate,
                startTime = startTime,
                viewModel = viewModel
            )

            SectionHeader(type = EntryType.SYMPTOM, title = stringResource(R.string.type_symptom))
            SymptomsSection(
                symptoms = symptomEntries,
                onAddSymptom = { symptomEntries.add(SymptomEntryState()) },
                onRemoveSymptom = { if (symptomEntries.size > 1) symptomEntries.removeAt(it) },
                onSymptomChange = { index, newState -> symptomEntries[index] = newState },
                viewModel = viewModel
            )

            SectionHeader(type = EntryType.SLEEP, title = stringResource(R.string.question_sleep_well))
            SleepSection(
                logDate = sleepLogDate,
                onLogDateChange = { sleepLogDate = it },
                startTime = sleepStartTime,
                onStartTimeChange = { sleepStartTime = it },
                endDate = sleepEndDate,
                onEndDateChange = { sleepEndDate = it },
                endTime = sleepEndTime,
                onEndTimeChange = { sleepEndTime = it },
                durationMinutes = sleepDurationMinutes,
                quality = sleepQuality,
                onQualityChange = { sleepQuality = it }
            )

            SectionHeader(type = EntryType.DRUG, title = stringResource(R.string.section_medication))
            MedicationCheckSection(
                reminders = drugReminders,
                taken = medicationTaken,
                onTakenChange = { index, isTaken -> if (index < medicationTaken.size) medicationTaken[index] = isTaken },
                times = medicationTimes,
                onTimeChange = { index, newTime -> if (index < medicationTimes.size) medicationTimes[index] = newTime }
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
    pains: MutableList<HealthEntry>,
    note: String,
    onNoteChange: (String) -> Unit,
    logDate: LocalDate,
    startTime: LocalTime,
    viewModel: TimelineViewModel
) {
    val context = LocalContext.current
    var currentHoldRegionId by remember { mutableStateOf<String?>(null) }
    var currentHoldIntensity by remember { mutableFloatStateOf(1f) }

    ElevatedCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Box(modifier = Modifier.fillMaxWidth().height(300.dp)) {
                BodySilhouette(
                    modifier = Modifier.fillMaxSize(),
                    onRegionHold = { regionId: String, intensity: Float ->
                        currentHoldRegionId = regionId
                        currentHoldIntensity = intensity
                    },
                    onRelease = {
                        val regionId = currentHoldRegionId
                        if (regionId != null) {
                            val existingIndex = pains.indexOfFirst { it.location == regionId }
                            if (existingIndex >= 0) {
                                pains[existingIndex] = pains[existingIndex].copy(intensity = currentHoldIntensity.toInt())
                            } else {
                                pains.add(HealthEntry(
                                    type = EntryType.PAIN,
                                    location = regionId,
                                    intensity = currentHoldIntensity.toInt(),
                                    timestamp = logDate.atTime(startTime).atZone(ZoneId.systemDefault()).toInstant()
                                ))
                            }
                        }
                        currentHoldRegionId = null
                    },
                    painEntries = pains
                )

                if (currentHoldRegionId != null) {
                    Box(modifier = Modifier.align(Alignment.TopEnd)) {
                        VerticalIntensityGauge(
                            intensity = currentHoldIntensity.toInt(),
                            maxVal = 10,
                            color = Color.Red,
                            label = stringResource(R.string.intensity_short_label)
                        )
                    }
                }
            }

            if (pains.isNotEmpty()) {
                Spacer(Modifier.height(8.dp))
                pains.forEachIndexed { index, pain ->
                    Column(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            val locationName = formatId(context, pain.location ?: "")
                            val intensityValue = (pain.intensity ?: 0).toLong()
                            Text(
                                text = stringResource(R.string.pain_item_format, locationName, intensityValue),
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold
                            )
                            IconButton(onClick = { pains.removeAt(index) }, modifier = Modifier.size(24.dp)) {
                                Icon(Icons.Default.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                            }
                        }
                        
                        val originSuggestions by remember(pain.location) { 
                            viewModel.getPainOriginSuggestions(pain.location ?: "") 
                        }.collectAsState(initial = emptyList())
                        
                        AutoCompleteTextField(
                            value = pain.origin ?: "",
                            onValueChange = { newOrigin ->
                                pains[index] = pain.copy(origin = newOrigin)
                            },
                            suggestions = originSuggestions,
                            label = stringResource(R.string.pain_origin_label)
                        )
                    }
                }
                Spacer(Modifier.height(8.dp))
                VoiceEnabledTextField(value = note, onValueChange = onNoteChange, label = stringResource(R.string.notes_label))
            } else {
                Text(
                    text = stringResource(R.string.select_body_part),
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.align(Alignment.CenterHorizontally).padding(top = 8.dp)
                )
            }
        }
    }
}

data class SymptomEntryState(
    val name: String = "",
    val intensity: Float = 5f,
    val note: String = ""
)

@Composable
fun SymptomsSection(
    symptoms: List<SymptomEntryState>,
    onAddSymptom: () -> Unit,
    onRemoveSymptom: (Int) -> Unit,
    onSymptomChange: (Int, SymptomEntryState) -> Unit,
    viewModel: TimelineViewModel
) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        symptoms.forEachIndexed { index, symptom ->
            ElevatedCard(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = stringResource(R.string.symptom_item_title, stringResource(R.string.type_symptom), index + 1),
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                        if (symptoms.size > 1) {
                            IconButton(onClick = { onRemoveSymptom(index) }) {
                                Icon(Icons.Default.Delete, contentDescription = stringResource(R.string.delete), tint = MaterialTheme.colorScheme.error)
                            }
                        }
                    }
                    
                    val suggestions by viewModel.symptomSuggestions.collectAsState()
                    AutoCompleteTextField(
                        value = symptom.name,
                        onValueChange = { onSymptomChange(index, symptom.copy(name = it)) },
                        suggestions = suggestions,
                        label = stringResource(R.string.symptom_name_label)
                    )
                    
                    Spacer(Modifier.height(16.dp))
                    Text(stringResource(R.string.intensity_label, symptom.intensity.roundToInt()))
                    Slider(
                        value = symptom.intensity,
                        onValueChange = { onSymptomChange(index, symptom.copy(intensity = it)) },
                        valueRange = 1f..10f,
                        steps = 8
                    )
                    
                    VoiceEnabledTextField(
                        value = symptom.note,
                        onValueChange = { onSymptomChange(index, symptom.copy(note = it)) },
                        label = stringResource(R.string.notes_label)
                    )
                }
            }
        }
        
        Button(
            onClick = onAddSymptom,
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(Icons.Default.Add, contentDescription = null)
            Spacer(Modifier.width(8.dp))
            Text(stringResource(R.string.add_symptom))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MedicationCheckSection(
    reminders: List<Reminder>,
    taken: List<Boolean>,
    onTakenChange: (Int, Boolean) -> Unit,
    times: List<LocalTime>,
    onTimeChange: (Int, LocalTime) -> Unit
) {
    if (reminders.isEmpty()) return

    ElevatedCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(
                text = stringResource(R.string.section_medication),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            reminders.forEachIndexed { index, reminder ->
                var showTimePicker by remember { mutableStateOf(false) }
                val currentTime = if (index < times.size) times[index] else LocalTime.now()
                val timeFormatter = remember { DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT) }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Checkbox(
                        checked = if (index < taken.size) taken[index] else false,
                        onCheckedChange = { onTakenChange(index, it) }
                    )
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = stringResource(R.string.question_drugs, reminder.title),
                            modifier = Modifier.padding(start = 8.dp)
                        )
                        IconButton(
                            onClick = { showTimePicker = true },
                            modifier = Modifier.padding(start = 4.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.AccessTime, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(4.dp))
                                Text(
                                    text = currentTime.format(timeFormatter),
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                        }
                    }
                }

                if (showTimePicker) {
                    val timeState = rememberTimePickerState(
                        initialHour = currentTime.hour,
                        initialMinute = currentTime.minute
                    )
                    TimePickerDialog(
                        onDismissRequest = { showTimePicker = false },
                        confirmButton = {
                            TextButton(onClick = {
                                onTimeChange(index, LocalTime.of(timeState.hour, timeState.minute))
                                showTimePicker = false
                            }) {
                                Text(stringResource(R.string.ok))
                            }
                        },
                        dismissButton = {
                            TextButton(onClick = { showTimePicker = false }) {
                                Text(stringResource(R.string.cancel))
                            }
                        }
                    ) {
                        TimePicker(state = timeState)
                    }
                }
            }
        }
    }
}

@Composable
fun SleepSection(
    logDate: LocalDate,
    onLogDateChange: (LocalDate) -> Unit,
    startTime: LocalTime,
    onStartTimeChange: (LocalTime) -> Unit,
    endDate: LocalDate,
    onEndDateChange: (LocalDate) -> Unit,
    endTime: LocalTime,
    onEndTimeChange: (LocalTime) -> Unit,
    durationMinutes: Int,
    quality: Float,
    onQualityChange: (Float) -> Unit
) {
    val context = LocalContext.current
    val durationText = remember(durationMinutes) {
        val h = durationMinutes / 60
        val m = durationMinutes % 60
        if (h > 0) context.getString(R.string.duration_h_m, h, m) else context.getString(R.string.duration_m, m)
    }

    ElevatedCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(stringResource(R.string.bedtime_label), style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(bottom = 8.dp))
            EntryDateTimePicker(
                date = logDate,
                onDateChange = onLogDateChange,
                startTime = startTime,
                onStartTimeChange = onStartTimeChange
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(stringResource(R.string.wakeup_time_label), style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(bottom = 8.dp))
            EntryDateTimePicker(
                date = endDate,
                onDateChange = onEndDateChange,
                startTime = endTime,
                onStartTimeChange = onEndTimeChange
            )

            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = durationText,
                onValueChange = { },
                label = { Text(stringResource(R.string.computed_duration_label)) },
                modifier = Modifier.fillMaxWidth(),
                readOnly = true,
                leadingIcon = { Icon(Icons.Default.Timer, contentDescription = null) },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outline
                )
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = stringResource(R.string.quality_label, quality.roundToInt()),
                style = MaterialTheme.typography.titleMedium
            )
            Slider(
                value = quality,
                onValueChange = onQualityChange,
                valueRange = 1f..10f,
                steps = 8
            )
        }
    }
}
