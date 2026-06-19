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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import org.chronicheal.app.R
import org.chronicheal.app.domain.model.EntryType
import org.chronicheal.app.domain.model.HealthEntry
import org.chronicheal.app.domain.usecase.GetSuggestionsUseCase
import org.chronicheal.app.presentation.components.AutoCompleteTextField
import org.chronicheal.app.presentation.components.EntryDateTimePicker
import org.chronicheal.app.presentation.components.IntensityField
import org.chronicheal.app.presentation.components.MoodSection
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
) {
    var logDate by rememberSaveable { mutableStateOf(if (dateString != null) LocalDate.parse(dateString) else LocalDate.now()) }
    var startTime by rememberSaveable { mutableStateOf(LocalTime.now()) }
    var isSaving by remember { mutableStateOf(value = false) }

    // State for sections
    var moodIntensity by rememberSaveable { mutableFloatStateOf(5f) }
    var moodNote by rememberSaveable { mutableStateOf("") }

    var showPain by rememberSaveable { mutableStateOf(value = false) }
    val painEntries = remember { mutableStateListOf<HealthEntry>() }

    var showSymptoms by rememberSaveable { mutableStateOf(value = false) }
    val symptomEntries = remember { mutableStateListOf<HealthEntry>() }

    // Pre-fill active drugs as checkboxes
    val allReminders by remindersViewModel.reminders.collectAsState()
    val drugReminders = remember(allReminders) {
        allReminders.filter { (it.entryType == EntryType.DRUG) && it.isEnabled }
    }

    val checkedDrugs = remember { mutableStateMapOf<Long, Boolean>() }
    val drugEntries = remember { mutableStateMapOf<Long, HealthEntry?>() }

    LaunchedEffect(drugReminders) {
        drugReminders.forEach { reminder ->
            if (!checkedDrugs.containsKey(reminder.id)) {
                checkedDrugs[reminder.id] = false
                if (reminder.templateEntryId != null) {
                    val entry = remindersViewModel.getEntryById(reminder.templateEntryId)
                    drugEntries[reminder.id] = entry
                }
            }
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
                            viewModel.saveEntry(moodEntry, null)

                            if (showPain) {
                                painEntries.forEach {
                                    viewModel.saveEntry(
                                        it.copy(timestamp = timestamp),
                                        null
                                    )
                                }
                            }

                            if (showSymptoms) {
                                symptomEntries.forEach {
                                    viewModel.saveEntry(
                                        it.copy(timestamp = timestamp),
                                        null
                                    )
                                }
                            }

                            drugReminders.forEach { reminder ->
                                if (checkedDrugs[reminder.id] == true) {
                                    val templateEntry = drugEntries[reminder.id]
                                    var baseName =
                                        reminder.title.removePrefix("Medication: ").trim()
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
                                            type = EntryType.DRUG,
                                            name = baseName,
                                            value = value,
                                            unit = unit,
                                            reminderId = reminder.id
                                        ), null
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
            item {
                SectionHeader(EntryType.MOOD, stringResource(R.string.how_are_you_feeling))
                MoodSection(
                    intensity = moodIntensity,
                    onIntensityChange = { moodIntensity = it },
                    note = moodNote
                ) { moodNote = it }
                Spacer(modifier = Modifier.height(24.dp))
            }

            // 2. Drugs
            if (drugReminders.isNotEmpty()) {
                item {
                    SectionHeader(EntryType.DRUG, stringResource(R.string.medications_taken))
                    ElevatedCard(modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            drugReminders.forEach { reminder ->
                                val templateEntry = drugEntries[reminder.id]
                                val drugName = templateEntry?.name
                                    ?: reminder.title.removePrefix("Medication: ").trim()
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
                                            checkedDrugs[reminder.id] =
                                                !(checkedDrugs[reminder.id] ?: false)
                                        }
                                        .padding(vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Checkbox(
                                        checked = checkedDrugs[reminder.id] ?: false,
                                        onCheckedChange = { checkedDrugs[reminder.id] = it }
                                    )
                                    Column {
                                        Text(drugName, style = MaterialTheme.typography.bodyLarge)
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

            // 3. Optional Pain
            item {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(
                        checked = showPain,
                        onCheckedChange = {
                            showPain = it
                            if (it && painEntries.isEmpty()) {
                                painEntries.add(HealthEntry(type = EntryType.PAIN, intensity = 5))
                            }
                        }
                    )
                    Text(
                        stringResource(R.string.log_pain_question),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            if (showPain) {
                items(painEntries) { pain ->
                    val index = painEntries.indexOf(pain)
                    ElevatedCard(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            val originSuggestions by viewModel.getSuggestions(
                                setOf(EntryType.PAIN),
                                GetSuggestionsUseCase.SuggestionField.ORIGIN,
                                parentLocation = pain.location
                            ).collectAsState(initial = emptyList())

                            val locationSuggestions by viewModel.getSuggestions(
                                setOf(EntryType.PAIN, EntryType.SYMPTOM),
                                GetSuggestionsUseCase.SuggestionField.LOCATION
                            ).collectAsState(initial = emptyList())

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

            // 4. Optional Symptoms
            item {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = showSymptoms, onCheckedChange = {
                        showSymptoms = it
                        if (it && symptomEntries.isEmpty()) {
                            symptomEntries.add(HealthEntry(type = EntryType.SYMPTOM, intensity = 5))
                        }
                    })
                    Text(
                        stringResource(R.string.log_symptoms_question),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            if (showSymptoms) {
                items(symptomEntries) { symptom ->
                    val index = symptomEntries.indexOf(symptom)
                    ElevatedCard(modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp)) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            val suggestions by viewModel.getSuggestions(
                                setOf(EntryType.SYMPTOM),
                                GetSuggestionsUseCase.SuggestionField.NAME
                            ).collectAsState(initial = emptyList())

                            AutoCompleteTextField(
                                value = symptom.name ?: "",
                                onValueChange = { symptomEntries[index] = symptom.copy(name = it) },
                                suggestions = suggestions,
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
        }
    }
}
