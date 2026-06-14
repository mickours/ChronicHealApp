package org.chronicheal.app.presentation

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import org.chronicheal.app.R
import org.chronicheal.app.domain.model.EntryType
import org.chronicheal.app.domain.model.HealthEntry
import org.chronicheal.app.domain.model.Reminder
import org.chronicheal.app.presentation.components.AutoCompleteTextField
import org.chronicheal.app.presentation.components.IntensityField
import org.chronicheal.app.presentation.components.TimePickerDialog
import org.chronicheal.app.ui.theme.HeaderBlue
import java.time.LocalTime
import java.time.format.TextStyle
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddReminderScreen(
    id: Long? = null,
    onBackClick: () -> Unit,
    onSaveSuccess: () -> Unit,
    viewModel: RemindersViewModel = hiltViewModel()
) {
    var title by rememberSaveable { mutableStateOf("") }
    var time by rememberSaveable { mutableStateOf(LocalTime.of(8, 0)) }
    var selectedDays by rememberSaveable { mutableStateOf(setOf(1, 2, 3, 4, 5, 6, 7)) }
    var type: EntryType by rememberSaveable { mutableStateOf(EntryType.DRUG) }
    var showTimePicker by rememberSaveable { mutableStateOf(false) }

    // Advanced Details State
    var showAdvancedDetails by rememberSaveable { mutableStateOf(false) }
    var detailName by rememberSaveable { mutableStateOf("") }
    var detailLocation by rememberSaveable { mutableStateOf("") }
    var detailIntensity by rememberSaveable { mutableStateOf(5) }
    var detailUnit by rememberSaveable { mutableStateOf("") }
    var detailValue by rememberSaveable { mutableStateOf("") }
    var detailNote by rememberSaveable { mutableStateOf("") }

    var existingReminder by remember { mutableStateOf<Reminder?>(null) }
    var templateEntry by remember { mutableStateOf<HealthEntry?>(null) }

    val nameSuggestions by viewModel.getNameSuggestions(type).collectAsState()
    val locationSuggestions by viewModel.getLocationSuggestions(type).collectAsState()
    val unitSuggestions by viewModel.getUnitSuggestions(type).collectAsState()

    LaunchedEffect(id) {
        if (id != null) {
            viewModel.getReminderById(id)?.let { reminder ->
                existingReminder = reminder
                title = reminder.title
                time = reminder.time
                selectedDays = reminder.daysOfWeek
                type = reminder.entryType

                reminder.templateEntryId?.let { entryId ->
                    viewModel.getEntryById(entryId)?.let { entry ->
                        templateEntry = entry
                        detailName = entry.name ?: ""
                        detailLocation = entry.location ?: ""
                        detailIntensity = entry.intensity ?: 5
                        detailUnit = entry.unit ?: ""
                        detailValue = entry.value?.toString() ?: ""
                        detailNote = entry.note
                        if (detailName.isNotBlank() || detailLocation.isNotBlank() || detailNote.isNotBlank()) {
                            showAdvancedDetails = true
                        }
                    }
                }
            }
        }
    }

    val createTemplateEntry = {
        HealthEntry(
            id = templateEntry?.id ?: 0,
            type = type,
            name = detailName.trim().takeIf { it.isNotBlank() },
            location = detailLocation.trim().takeIf { it.isNotBlank() },
            intensity = detailIntensity.takeIf { type?.hasIntensity()!! },
            unit = detailUnit.trim().takeIf { it.isNotBlank() },
            value = detailValue.replace(",", ".").toDoubleOrNull(),
            note = detailNote.trim()
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        if (id == null) stringResource(R.string.add_reminder) else stringResource(
                            R.string.edit_reminder
                        )
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back))
                    }
                },
                actions = {
                    Button(
                        onClick = {
                            val reminder = Reminder(
                                id = existingReminder?.id ?: 0,
                                title = title.trim().ifEmpty { "Reminder" },
                                time = time,
                                daysOfWeek = selectedDays,
                                isEnabled = true,
                                entryType = type,
                                templateEntryId = existingReminder?.templateEntryId
                            )

                            val entryToSave =
                                if (showAdvancedDetails && (detailName.isNotBlank() || detailLocation.isNotBlank() || detailValue.isNotBlank() || detailNote.isNotBlank() || type.hasIntensity())) {
                                    createTemplateEntry()
                                } else null

                            if (existingReminder != null) {
                                viewModel.updateReminder(reminder, entryToSave)
                            } else {
                                viewModel.addReminder(reminder, entryToSave)
                            }
                            onSaveSuccess()
                        },
                        modifier = Modifier.padding(end = 8.dp)
                    ) {
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
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState())
        ) {
            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                label = { Text(stringResource(R.string.reminder_title)) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(16.dp))

            var typeMenuExpanded by remember { mutableStateOf(false) }
            ExposedDropdownMenuBox(
                expanded = typeMenuExpanded,
                onExpandedChange = { typeMenuExpanded = !typeMenuExpanded }
            ) {
                OutlinedTextField(
                    value = type.name.lowercase().replaceFirstChar { it.uppercase() },
                    onValueChange = {},
                    readOnly = true,
                    label = { Text(stringResource(R.string.entry_type_label)) },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = typeMenuExpanded) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .menuAnchor(MenuAnchorType.PrimaryNotEditable, true)
                )
                ExposedDropdownMenu(
                    expanded = typeMenuExpanded,
                    onDismissRequest = { typeMenuExpanded = false }
                ) {
                    EntryType.entries.forEach { option ->
                        DropdownMenuItem(
                            text = {
                                Text(
                                    "${option.emoji} ${
                                        option.name.lowercase().replaceFirstChar { it.uppercase() }
                                    }"
                                )
                            },
                            onClick = {
                                type = option
                                typeMenuExpanded = false
                            }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    stringResource(R.string.time_label),
                    style = MaterialTheme.typography.titleMedium
                )
                Spacer(modifier = Modifier.width(16.dp))
                OutlinedButton(onClick = { showTimePicker = true }) {
                    val timeFormatter = java.time.format.DateTimeFormatter.ofPattern("HH:mm")
                    Text(time.format(timeFormatter))
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(stringResource(R.string.repeat_days), style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(8.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                (1..7).forEach { dayOfWeek ->
                    val dayName = java.time.DayOfWeek.of(dayOfWeek)
                        .getDisplayName(TextStyle.SHORT, Locale.getDefault())
                    val isSelected = selectedDays.contains(dayOfWeek)
                    
                    FilterChip(
                        selected = isSelected,
                        onClick = {
                            selectedDays = if (isSelected) {
                                if (selectedDays.size > 1) selectedDays - dayOfWeek else selectedDays
                            } else {
                                selectedDays + dayOfWeek
                            }
                        },
                        label = { Text(dayName) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                            selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Advanced Details Section
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { showAdvancedDetails = !showAdvancedDetails }
                    .padding(vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Advanced Options (Prefill details)",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Icon(
                    imageVector = if (showAdvancedDetails) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                    contentDescription = null
                )
            }

            if (showAdvancedDetails) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Text(
                            text = "These details will be prefilled when you log an entry from this reminder.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        // Name
                        if (type == EntryType.DRUG || type == EntryType.ACTIVITY || type == EntryType.MEAL || type == EntryType.SYMPTOM || type == EntryType.DISEASE || type == EntryType.EXTERNAL_FACTOR || type == EntryType.MEDICAL_APPOINTMENT) {
                            AutoCompleteTextField(
                                value = detailName,
                                onValueChange = { detailName = it },
                                suggestions = nameSuggestions,
                                label = "Name"
                            )
                        }

                        // Location
                        if (type == EntryType.PAIN || type == EntryType.SYMPTOM) {
                            AutoCompleteTextField(
                                value = detailLocation,
                                onValueChange = { detailLocation = it },
                                suggestions = locationSuggestions,
                                label = "Location"
                            )
                        }

                        // Value and Unit
                        if (type == EntryType.DRUG || type == EntryType.BEVERAGE) {
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                OutlinedTextField(
                                    value = detailValue,
                                    onValueChange = { detailValue = it },
                                    label = { Text("Amount/Dosage") },
                                    modifier = Modifier.weight(1f),
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                    singleLine = true
                                )
                                AutoCompleteTextField(
                                    value = detailUnit,
                                    onValueChange = { detailUnit = it },
                                    suggestions = unitSuggestions,
                                    label = "Unit",
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }

                        // Intensity
                        if (type.hasIntensity()) {
                            Box(
                                modifier = Modifier.fillMaxWidth(),
                                contentAlignment = Alignment.Center
                            ) {
                                IntensityField(
                                    intensity = detailIntensity,
                                    onIntensityChange = { detailIntensity = it ?: 5 }
                                )
                            }
                        }

                        // Note
                        OutlinedTextField(
                            value = detailNote,
                            onValueChange = { detailNote = it },
                            label = { Text("Note") },
                            modifier = Modifier.fillMaxWidth(),
                            minLines = 2
                        )
                    }
                }
            }

            if (showTimePicker) {
                val timeState =
                    rememberTimePickerState(initialHour = time.hour, initialMinute = time.minute)
                TimePickerDialog(
                    onDismissRequest = { showTimePicker = false },
                    confirmButton = {
                        TextButton(onClick = {
                            time = LocalTime.of(timeState.hour, timeState.minute)
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

fun EntryType.hasIntensity(): Boolean {
    return this == EntryType.PAIN || this == EntryType.SYMPTOM || this == EntryType.MOOD ||
            this == EntryType.SLEEP || this == EntryType.ACTIVITY || this == EntryType.STOOL ||
            this == EntryType.PERIOD || this == EntryType.DISEASE || this == EntryType.EXTERNAL_FACTOR
}
