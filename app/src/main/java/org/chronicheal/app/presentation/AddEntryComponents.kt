package org.chronicheal.app.presentation

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.chronicheal.app.R
import org.chronicheal.app.domain.model.EntryType
import org.chronicheal.app.domain.model.HealthEntry
import org.chronicheal.app.ui.theme.HeaderBlue
import org.chronicheal.app.ui.theme.PrimaryContainerLight
import org.chronicheal.app.ui.theme.OnPrimaryContainerLight
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEntryScaffold(
    title: String,
    existingEntry: HealthEntry?,
    currentEntry: () -> HealthEntry,
    onBackClick: () -> Unit,
    onSaveSuccess: () -> Unit,
    onDeleteClick: () -> Unit,
    viewModel: TimelineViewModel,
    onSave: (() -> Unit)? = null,
    content: @Composable (PaddingValues) -> Unit
) {
    var showDeleteConfirmation by rememberSaveable { mutableStateOf(false) }

    val handleSave = {
        if (onSave != null) {
            onSave()
        } else {
            viewModel.saveEntryAndNotify(existingEntry, currentEntry())
            onSaveSuccess()
        }
    }

    BackHandler(onBack = onBackClick)

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text(title) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back))
                    }
                },
                actions = {
                    if (existingEntry != null) {
                        IconButton(onClick = { showDeleteConfirmation = true }) {
                            Icon(Icons.Default.Delete, contentDescription = stringResource(R.string.delete))
                        }
                    }
                    Button(
                        onClick = handleSave,
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
        ) {
            Box(modifier = Modifier.weight(1f)) {
                content(PaddingValues(0.dp))
            }
        }

        if (showDeleteConfirmation) {
            AlertDialog(
                onDismissRequest = { showDeleteConfirmation = false },
                title = { Text(stringResource(R.string.delete_entry_title)) },
                text = { Text(stringResource(R.string.delete_entry_msg)) },
                confirmButton = {
                    TextButton(
                        onClick = {
                            onDeleteClick()
                            showDeleteConfirmation = false
                        }
                    ) {
                        Text(stringResource(R.string.delete), color = MaterialTheme.colorScheme.error)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showDeleteConfirmation = false }) {
                        Text(stringResource(R.string.cancel))
                    }
                }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EntryDateTimePicker(
    date: LocalDate,
    onDateChange: (LocalDate) -> Unit,
    startTime: LocalTime,
    onStartTimeChange: (LocalTime) -> Unit
) {
    var showStartTimePicker by rememberSaveable { mutableStateOf(false) }
    var showDatePicker by rememberSaveable { mutableStateOf(false) }
    
    val startTimeState = rememberTimePickerState(
        initialHour = startTime.hour,
        initialMinute = startTime.minute
    )

    val datePickerState = rememberDatePickerState(
        initialSelectedDateMillis = date.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
    )

    // Locale-aware formatters
    val dateFormatter = remember { DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM) }
    val timeFormatter = remember { DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT) }

    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Date
            OutlinedCard(
                onClick = { showDatePicker = true },
                modifier = Modifier.weight(1f)
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.CalendarToday, contentDescription = null, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(8.dp))
                    Column {
                        Text(stringResource(R.string.date_label), style = MaterialTheme.typography.labelSmall)
                        Text(
                            text = date.format(dateFormatter),
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }

            // Start Time
            OutlinedCard(
                onClick = { showStartTimePicker = true },
                modifier = Modifier.weight(1f)
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.AccessTime, contentDescription = null, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(8.dp))
                    Column {
                        Text(stringResource(R.string.start_time_label), style = MaterialTheme.typography.labelSmall)
                        Text(
                            text = startTime.format(timeFormatter),
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }
        }

        if (showStartTimePicker) {
            TimePickerDialog(
                onDismissRequest = { showStartTimePicker = false },
                confirmButton = {
                    TextButton(onClick = {
                        onStartTimeChange(LocalTime.of(startTimeState.hour, startTimeState.minute))
                        showStartTimePicker = false
                    }) {
                        Text(stringResource(R.string.ok))
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showStartTimePicker = false }) {
                        Text(stringResource(R.string.cancel))
                    }
                }
            ) {
                TimePicker(state = startTimeState)
            }
        }

        if (showDatePicker) {
            DatePickerDialog(
                onDismissRequest = { showDatePicker = false },
                confirmButton = {
                    TextButton(onClick = {
                        datePickerState.selectedDateMillis?.let {
                            onDateChange(Instant.ofEpochMilli(it).atZone(ZoneId.systemDefault()).toLocalDate())
                        }
                        showDatePicker = false
                    }) {
                        Text(stringResource(R.string.ok))
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showDatePicker = false }) {
                        Text(stringResource(R.string.cancel))
                    }
                }
            ) {
                DatePicker(state = datePickerState)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TimePickerDialog(
    onDismissRequest: () -> Unit,
    confirmButton: @Composable () -> Unit,
    dismissButton: @Composable () -> Unit,
    content: @Composable () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismissRequest,
        confirmButton = confirmButton,
        dismissButton = dismissButton,
        text = { content() }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AutoCompleteTextField(
    value: String,
    onValueChange: (String) -> Unit,
    suggestions: List<String>,
    label: String,
    modifier: Modifier = Modifier,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    keyboardActions: KeyboardActions = KeyboardActions.Default
) {
    val filteredSuggestions = remember(value, suggestions) {
        if (value.isBlank()) {
            suggestions.take(5)
        } else {
            suggestions
                .filter { it.contains(value, ignoreCase = true) && it != value }
                .take(5)
        }
    }

    Column(modifier = modifier.fillMaxWidth()) {
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            label = { Text(label) },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            keyboardOptions = keyboardOptions,
            keyboardActions = keyboardActions
        )

        if (filteredSuggestions.isNotEmpty()) {
            LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(horizontal = 4.dp)
            ) {
                items(filteredSuggestions) { suggestion ->
                    SuggestionChip(
                        onClick = { onValueChange(suggestion) },
                        label = {
                            Text(
                                text = suggestion,
                                style = MaterialTheme.typography.labelSmall
                            )
                        },
                        colors = SuggestionChipDefaults.suggestionChipColors(
                            containerColor = PrimaryContainerLight,
                            labelColor = OnPrimaryContainerLight
                        )
                    )
                }
            }
        }
    }
}

@Composable
fun TextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    minLines: Int = 1,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    keyboardActions: KeyboardActions = KeyboardActions.Default
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        modifier = modifier.fillMaxWidth(),
        minLines = minLines,
        keyboardOptions = keyboardOptions,
        keyboardActions = keyboardActions
    )
}

@Composable
fun VerticalIntensityGauge(
    intensity: Int,
    maxVal: Int,
    color: Color,
    label: String,
    modifier: Modifier = Modifier
) {
    val isDark = isSystemInDarkTheme()
    
    val barBrush = remember(color) {
        Brush.verticalGradient(
            colors = listOf(
                color, // Darker (Top)
                color.copy(alpha = 0.4f) // Lighter (Bottom)
            )
        )
    }

    Column(
        modifier = modifier
            .fillMaxHeight()
            .width(40.dp) // Enlarged gauge area
            .background(color.copy(alpha = 0.1f)),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.ExtraBold,
            color = color.copy(alpha = 0.8f),
            modifier = Modifier.padding(vertical = 4.dp)
        )
        Box(
            modifier = Modifier
                .weight(1f)
                .width(16.dp) // Enlarged bar width
                .clip(RoundedCornerShape(8.dp))
                .background(if (isDark) Color.DarkGray.copy(alpha = 0.3f) else Color.LightGray.copy(alpha = 0.3f)),
            contentAlignment = Alignment.BottomCenter
        ) {
            Box(
                modifier = Modifier
                    .fillMaxHeight(fraction = intensity.coerceIn(0, maxVal).toFloat() / maxVal)
                    .fillMaxWidth()
                    .background(barBrush)
            )
        }
        Text(
            text = intensity.toString(),
            style = MaterialTheme.typography.titleMedium, // Enlarged value text
            fontWeight = FontWeight.Black,
            color = color,
            modifier = Modifier.padding(vertical = 4.dp)
        )
    }
}

@Composable
fun SectionHeader(type: EntryType, title: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(text = type.emoji, fontSize = 24.sp)
        Spacer(Modifier.width(8.dp))
        Text(text = title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun MoodSection(
    intensity: Float,
    onIntensityChange: (Float) -> Unit,
    note: String,
    onNoteChange: (String) -> Unit
) {
    ElevatedCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            val moodLabel = when(intensity.roundToInt()) {
                in 1..2 -> stringResource(R.string.mood_very_bad)
                in 3..4 -> stringResource(R.string.mood_bad)
                in 5..6 -> stringResource(R.string.mood_neutral)
                in 7..8 -> stringResource(R.string.mood_good)
                else -> stringResource(R.string.mood_amazing)
            }
            Text("${stringResource(R.string.type_mood)}: $moodLabel (${intensity.roundToInt()}/10)")
            Slider(value = intensity, onValueChange = onIntensityChange, valueRange = 1f..10f, steps = 8)
            TextField(value = note, onValueChange = onNoteChange, label = stringResource(R.string.section_mood))
        }
    }
}

@Composable
fun PainEntryForm(
    modifier: Modifier = Modifier,
    intensity: Float,
    onIntensityChange: (Float) -> Unit,
    location: String,
    onLocationChange: (String) -> Unit,
    note: String,
    onNoteChange: (String) -> Unit,
    logDate: LocalDate,
    onDateChange: (LocalDate) -> Unit,
    startTime: LocalTime,
    onStartTimeChange: (LocalTime) -> Unit,
    viewModel: TimelineViewModel
) {
    val locationSuggestions by viewModel.painLocationSuggestions.collectAsState()

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp)
    ) {
        EntryDateTimePicker(
            date = logDate,
            onDateChange = onDateChange,
            startTime = startTime,
            onStartTimeChange = { onStartTimeChange(it) }
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = stringResource(R.string.intensity_label, intensity.roundToInt()),
            style = MaterialTheme.typography.titleMedium
        )
        Slider(
            value = intensity,
            onValueChange = onIntensityChange,
            valueRange = 1f..10f,
            steps = 8
        )

        Spacer(modifier = Modifier.height(16.dp))

        AutoCompleteTextField(
            value = location,
            onValueChange = onLocationChange,
            suggestions = locationSuggestions,
            label = stringResource(R.string.location_label)
        )

        Spacer(modifier = Modifier.height(16.dp))

        TextField(
            value = note,
            onValueChange = onNoteChange,
            label = stringResource(R.string.notes_label),
            minLines = 3
        )
    }
}
