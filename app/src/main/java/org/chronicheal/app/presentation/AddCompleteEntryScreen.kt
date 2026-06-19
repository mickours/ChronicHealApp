package org.chronicheal.app.presentation

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import org.chronicheal.app.R
import org.chronicheal.app.domain.model.EntryType
import org.chronicheal.app.domain.model.HealthEntry
import org.chronicheal.app.domain.model.Reminder
import org.chronicheal.app.domain.usecase.GetSuggestionsUseCase
import org.chronicheal.app.presentation.components.AutoCompleteTextField
import org.chronicheal.app.presentation.components.EntryDateTimePicker
import org.chronicheal.app.presentation.components.IntensityField
import org.chronicheal.app.presentation.components.MoodSection
import org.chronicheal.app.presentation.components.ReminderSection
import org.chronicheal.app.presentation.components.SectionHeader
import org.chronicheal.app.presentation.components.VoiceEnabledTextField
import org.chronicheal.app.ui.theme.HeaderBlue
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
    remindersViewModel: RemindersViewModel = hiltViewModel(),
    viewModel: AddEntryViewModel = hiltViewModel(),
    settingsViewModel: SettingsViewModel = hiltViewModel()
) {
    val settingsUiState by settingsViewModel.uiState.collectAsState()
    val checkInSections = settingsUiState.checkInSections

    var logDate by rememberSaveable { mutableStateOf(if (dateString != null) LocalDate.parse(dateString) else LocalDate.now()) }
    var startTime by rememberSaveable { mutableStateOf(LocalTime.now()) }
    var isSaving by remember { mutableStateOf(value = false) }

    // State for sections
    var moodIntensity by rememberSaveable { mutableFloatStateOf(5f) }
    var moodNote by rememberSaveable { mutableStateOf("") }

    var sleepQuality by rememberSaveable { mutableFloatStateOf(5f) }
    var sleepNote by rememberSaveable { mutableStateOf("") }

    val painEntries = remember { mutableStateListOf<HealthEntry>() }
    val symptomEntries = remember { mutableStateListOf<HealthEntry>() }

    LaunchedEffect(checkInSections) {
        if (EntryType.PAIN in checkInSections && painEntries.isEmpty()) {
            painEntries.add(HealthEntry(type = EntryType.PAIN, intensity = 5))
        }
        if (EntryType.SYMPTOM in checkInSections && symptomEntries.isEmpty()) {
            symptomEntries.add(HealthEntry(type = EntryType.SYMPTOM, intensity = 5))
        }
    }

    // Pre-fill active drugs as checkboxes
    val allReminders by remindersViewModel.reminders.collectAsState()
    val allEntries by viewModel.entries.collectAsState()

    val locationSuggestions by remember {
        viewModel.getSuggestions(
            setOf(EntryType.PAIN, EntryType.SYMPTOM),
            GetSuggestionsUseCase.SuggestionField.LOCATION
        )
    }.collectAsState(initial = emptyList())

    val symptomNameSuggestions by remember {
        viewModel.getSuggestions(
            setOf(EntryType.SYMPTOM),
            GetSuggestionsUseCase.SuggestionField.NAME
        )
    }.collectAsState(initial = emptyList())

    val entriesOnSelectedDate = remember(allEntries, logDate) {
        allEntries.filter { entry ->
            entry.timestamp.atZone(ZoneId.systemDefault()).toLocalDate() == logDate
        }
    }

    val pendingReminders = remember(allReminders, entriesOnSelectedDate, logDate) {
        val dayOfWeek = logDate.dayOfWeek.value // 1 (Mon) to 7 (Sun)
        allReminders.filter { reminder ->
            reminder.isEnabled && reminder.title != "Checkup" &&
                    reminder.daysOfWeek.contains(dayOfWeek) &&
                    entriesOnSelectedDate.none { it.reminderId == reminder.id }
        }
    }

    val checkedReminders = remember { mutableStateMapOf<Long, Boolean>() }
    val reminderEntries = remember { mutableStateMapOf<Long, HealthEntry?>() }

    LaunchedEffect(pendingReminders) {
        pendingReminders.forEach { reminder ->
            if (!checkedReminders.containsKey(reminder.id)) {
                checkedReminders[reminder.id] = false
                if (reminder.templateEntryId != null) {
                    val entry = remindersViewModel.getEntryById(reminder.templateEntryId)
                    reminderEntries[reminder.id] = entry
                }
            }
        }
    }

    val checkupReminder = remember(allReminders) { allReminders.find { it.title == "Checkup" } }
    var isCheckupReminderEnabled by rememberSaveable { mutableStateOf(false) }
    var checkupReminderTime by rememberSaveable { mutableStateOf(LocalTime.of(20, 0)) }
    var hasInitializedReminder by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(checkupReminder) {
        if (!hasInitializedReminder && checkupReminder != null) {
            isCheckupReminderEnabled = checkupReminder.isEnabled
            checkupReminderTime = checkupReminder.time
            hasInitializedReminder = true
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.complete_check_in)) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.back),
                        )
                    }
                },
                actions = {
                    Button(
                        onClick = {
                            isSaving = true
                            val timestamp =
                                logDate.atTime(startTime).atZone(ZoneId.systemDefault()).toInstant()

                            val moodEntry = HealthEntry(
                                timestamp = timestamp,
                                type = EntryType.MOOD,
                                intensity = moodIntensity.roundToInt(),
                                note = moodNote
                            )
                            if (EntryType.MOOD in checkInSections) {
                                viewModel.saveEntry(moodEntry, null)
                            }

                            if (EntryType.SLEEP in checkInSections) {
                                viewModel.saveEntry(
                                    HealthEntry(
                                        timestamp = timestamp,
                                        type = EntryType.SLEEP,
                                        intensity = sleepQuality.roundToInt(),
                                        note = sleepNote
                                    ), null
                                )
                            }

                            if (EntryType.PAIN in checkInSections) {
                                painEntries.forEach {
                                    viewModel.saveEntry(
                                        it.copy(timestamp = timestamp),
                                        null
                                    )
                                }
                            }

                            if (EntryType.SYMPTOM in checkInSections) {
                                symptomEntries.forEach {
                                    viewModel.saveEntry(
                                        it.copy(timestamp = timestamp),
                                        null
                                    )
                                }
                            }

                            pendingReminders.forEach { reminder ->
                                if (checkedReminders[reminder.id] == true) {
                                    val templateEntry = reminderEntries[reminder.id]
                                    var baseName = reminder.title
                                    if (reminder.entryType == EntryType.DRUG) {
                                        baseName = baseName.removePrefix("Medication: ").trim()
                                    }
                                    var value: Double? = null
                                    var unit: String? = null

                                    if (templateEntry != null) {
                                        baseName = templateEntry.name ?: baseName
                                        value = templateEntry.value
                                        unit = templateEntry.unit
                                    }

                                    viewModel.saveEntry(
                                        HealthEntry(
                                            timestamp = timestamp,
                                            type = reminder.entryType,
                                            name = baseName,
                                            value = value,
                                            unit = unit,
                                            reminderId = reminder.id
                                        ), null
                                    )
                                }
                            }

                            // Update Checkup Reminder
                            checkupReminder?.let {
                                if (it.isEnabled != isCheckupReminderEnabled || it.time != checkupReminderTime) {
                                    remindersViewModel.updateReminder(
                                        it.copy(
                                            isEnabled = isCheckupReminderEnabled,
                                            time = checkupReminderTime
                                        )
                                    )
                                }
                            } ?: run {
                                if (isCheckupReminderEnabled) {
                                    remindersViewModel.addReminder(
                                        Reminder(
                                            title = "Checkup",
                                            time = checkupReminderTime,
                                            daysOfWeek = (1..7).toSet(),
                                            isEnabled = true,
                                            entryType = EntryType.DRUG
                                        )
                                    )
                                }
                            }

                            onSaveSuccess()
                        },
                        modifier = Modifier.padding(end = 8.dp),
                        enabled = !isSaving
                    ) {
                        if (isSaving) {
                            CircularProgressIndicator(
                                modifier = Modifier.padding(end = 8.dp),
                                color = MaterialTheme.colorScheme.onPrimary
                            )
                        }
                        Text(stringResource(R.string.save))
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = HeaderBlue,
                    titleContentColor = Color.Black,
                    navigationIconContentColor = Color.Black,
                    actionIconContentColor = Color.Black
                )
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp),
            contentPadding = PaddingValues(bottom = 80.dp)
        ) {
            item {
                EntryDateTimePicker(
                    date = logDate,
                    onDateChange = { logDate = it },
                    startTime = startTime
                ) { startTime = it }
                Spacer(modifier = Modifier.height(24.dp))
            }

            // 1. Mood
            if (EntryType.MOOD in checkInSections) {
                item {
                    SectionHeader(EntryType.MOOD, stringResource(R.string.section_mood))
                    MoodSection(
                        intensity = moodIntensity,
                        onIntensityChange = { moodIntensity = it },
                        note = moodNote
                    ) { moodNote = it }
                    Spacer(modifier = Modifier.height(24.dp))
                }
            }

            // 2. Sleep
            if (EntryType.SLEEP in checkInSections) {
                item {
                    SectionHeader(EntryType.SLEEP, stringResource(R.string.section_sleep))
                    Text(
                        stringResource(R.string.question_sleep_well),
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.padding(vertical = 8.dp)
                    )
                }

                item {
                    ElevatedCard(modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                stringResource(
                                    R.string.quality_label,
                                    sleepQuality.roundToInt()
                                )
                            )
                            Slider(
                                value = sleepQuality,
                                onValueChange = { sleepQuality = it },
                                valueRange = 1f..10f,
                                steps = 8
                            )
                            VoiceEnabledTextField(
                                value = sleepNote,
                                onValueChange = { sleepNote = it },
                                label = stringResource(R.string.notes_label)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(24.dp))
                }
            }

            // 3. Reminders
            if (EntryType.DRUG in checkInSections && pendingReminders.isNotEmpty()) {
                item {
                    SectionHeader(EntryType.DRUG, stringResource(R.string.reminders_title))
                    ElevatedCard(modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            pendingReminders.forEach { reminder ->
                                val templateEntry = reminderEntries[reminder.id]
                                val displayName = templateEntry?.name
                                    ?: if (reminder.entryType == EntryType.DRUG) {
                                        reminder.title.removePrefix("Medication: ").trim()
                                    } else {
                                        reminder.title
                                    }
                                val dosageText = templateEntry?.let { entry ->
                                    val value = entry.value?.let { v ->
                                        if (v == v.toLong().toDouble()) v.toLong()
                                            .toString() else v.toString()
                                    }
                                    if (value != null && (entry.unit != null)) "$value ${entry.unit}" else null
                                }

                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            checkedReminders[reminder.id] =
                                                !(checkedReminders[reminder.id] ?: false)
                                        }
                                        .padding(vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Checkbox(
                                        checked = checkedReminders[reminder.id] ?: false,
                                        onCheckedChange = { checkedReminders[reminder.id] = it }
                                    )
                                    Column {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Text(reminder.entryType.emoji)
                                            Spacer(Modifier.width(8.dp))
                                            Text(
                                                displayName,
                                                style = MaterialTheme.typography.bodyLarge
                                            )
                                        }
                                        dosageText?.let {
                                            Text(
                                                it,
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(24.dp))
                }
            }

            if (EntryType.PAIN in checkInSections) {
                item {
                    SectionHeader(EntryType.PAIN, stringResource(R.string.section_pain))
                    Text(
                        stringResource(R.string.log_pain_question),
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.padding(vertical = 8.dp)
                    )
                }

                itemsIndexed(painEntries) { index, pain ->
                    ElevatedCard(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            val originSuggestions by remember(pain.location) {
                                viewModel.getSuggestions(
                                    setOf(EntryType.PAIN),
                                    GetSuggestionsUseCase.SuggestionField.ORIGIN,
                                    parentLocation = pain.location
                                )
                            }.collectAsState(initial = emptyList())

                            AutoCompleteTextField(
                                value = pain.location ?: "",
                                onValueChange = { painEntries[index] = pain.copy(location = it) },
                                suggestions = locationSuggestions,
                                label = stringResource(R.string.location_label)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            AutoCompleteTextField(
                                value = pain.origin ?: "",
                                onValueChange = { painEntries[index] = pain.copy(origin = it) },
                                suggestions = originSuggestions,
                                label = stringResource(R.string.pain_origin_label)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Box(
                                modifier = Modifier.fillMaxWidth(),
                                contentAlignment = Alignment.Center
                            ) {
                                IntensityField(
                                    intensity = pain.intensity ?: 5,
                                    onIntensityChange = {
                                        painEntries[index] = pain.copy(intensity = it ?: 5)
                                    }
                                )
                            }
                        }
                    }
                }
                item {
                    Button(onClick = {
                        painEntries.add(
                            HealthEntry(
                                type = EntryType.PAIN,
                                intensity = 5
                            )
                        )
                    }) {
                        Text(stringResource(R.string.add_another_pain))
                    }
                    Spacer(modifier = Modifier.height(24.dp))
                }
            }

            if (EntryType.SYMPTOM in checkInSections) {
                item {
                    SectionHeader(EntryType.SYMPTOM, stringResource(R.string.section_anything_else))
                    Text(
                        stringResource(R.string.log_symptoms_question),
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.padding(vertical = 8.dp)
                    )
                }

                itemsIndexed(symptomEntries) { index, symptom ->
                    ElevatedCard(modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp)) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            AutoCompleteTextField(
                                value = symptom.name ?: "",
                                onValueChange = { symptomEntries[index] = symptom.copy(name = it) },
                                suggestions = symptomNameSuggestions,
                                label = stringResource(R.string.symptom_name_label)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            VoiceEnabledTextField(
                                value = symptom.note,
                                onValueChange = { symptomEntries[index] = symptom.copy(note = it) },
                                label = stringResource(R.string.notes_label)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Box(
                                modifier = Modifier.fillMaxWidth(),
                                contentAlignment = Alignment.Center
                            ) {
                                IntensityField(
                                    intensity = symptom.intensity ?: 5,
                                    onIntensityChange = {
                                        symptomEntries[index] = symptom.copy(intensity = it ?: 5)
                                    }
                                )
                            }
                        }
                    }
                }
                item {
                    Button(onClick = {
                        symptomEntries.add(
                            HealthEntry(
                                type = EntryType.SYMPTOM,
                                intensity = 5
                            )
                        )
                    }) {
                        Text(stringResource(R.string.add_another_symptom))
                    }
                    Spacer(modifier = Modifier.height(24.dp))
                }
            }

            // 6. Reminder Settings
            if (EntryType.DRUG in checkInSections) {
                item {
                    SectionHeader(EntryType.DRUG, stringResource(R.string.checkup_reminder_title))
                    ReminderSection(
                        setReminder = isCheckupReminderEnabled,
                        onSetReminderChange = { isCheckupReminderEnabled = it },
                        reminderTime = checkupReminderTime,
                        onReminderTimeChange = { checkupReminderTime = it },
                        isUpdate = checkupReminder != null
                    )
                }
            }
        }
    }
}
