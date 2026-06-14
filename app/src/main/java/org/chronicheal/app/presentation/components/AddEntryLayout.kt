package org.chronicheal.app.presentation.components

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import org.chronicheal.app.R
import org.chronicheal.app.domain.model.EntryType
import org.chronicheal.app.domain.model.HealthEntry
import org.chronicheal.app.domain.model.Reminder
import org.chronicheal.app.presentation.AddEntryViewModel
import org.chronicheal.app.ui.theme.HeaderBlue
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEntryScaffold(
    title: String,
    hasExistingEntry: Boolean,
    onBackClick: () -> Unit,
    onSaveClick: () -> Unit,
    onDeleteClick: () -> Unit,
    content: @Composable (PaddingValues) -> Unit
) {
    var showDeleteConfirmation by rememberSaveable { mutableStateOf(false) }

    BackHandler(onBack = onBackClick)

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text(title) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.back)
                        )
                    }
                },
                actions = {
                    if (hasExistingEntry) {
                        IconButton(onClick = { showDeleteConfirmation = true }) {
                            Icon(
                                Icons.Default.Delete,
                                contentDescription = stringResource(R.string.delete)
                            )
                        }
                    }
                    Button(
                        onClick = onSaveClick,
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
        Box(modifier = Modifier
            .fillMaxSize()
            .padding(innerPadding)) {
            content(PaddingValues(0.dp))
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
                        Text(
                            stringResource(R.string.delete),
                            color = MaterialTheme.colorScheme.error
                        )
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

@Composable
fun SectionHeader(type: EntryType, title: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(text = type.emoji, fontSize = 24.sp)
        Spacer(Modifier.width(8.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
        )
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
            val moodLabel = when (intensity.roundToInt()) {
                in 1..2 -> stringResource(R.string.mood_very_bad)
                in 3..4 -> stringResource(R.string.mood_bad)
                in 5..6 -> stringResource(R.string.mood_neutral)
                in 7..8 -> stringResource(R.string.mood_good)
                else -> stringResource(R.string.mood_amazing)
            }
            Text(
                stringResource(
                    R.string.mood_summary_format,
                    stringResource(R.string.type_mood),
                    moodLabel,
                    intensity.roundToInt()
                )
            )
            Slider(
                value = intensity,
                onValueChange = onIntensityChange,
                valueRange = 1f..10f,
                steps = 8
            )
            VoiceEnabledTextField(
                value = note,
                onValueChange = onNoteChange,
                label = stringResource(R.string.section_mood)
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReminderSection(
    setReminder: Boolean,
    onSetReminderChange: (Boolean) -> Unit,
    reminderTime: LocalTime,
    onReminderTimeChange: (LocalTime) -> Unit,
    isUpdate: Boolean
) {
    val context = LocalContext.current
    var showTimePicker by rememberSaveable { mutableStateOf(false) }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) onSetReminderChange(true)
    }

    Column {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            Checkbox(
                checked = setReminder,
                onCheckedChange = { checked ->
                    if (checked) {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                            when (ContextCompat.checkSelfPermission(
                                context,
                                Manifest.permission.POST_NOTIFICATIONS
                            )) {
                                PackageManager.PERMISSION_GRANTED -> onSetReminderChange(true)
                                else -> permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                            }
                        } else {
                            onSetReminderChange(true)
                        }
                    } else {
                        onSetReminderChange(false)
                    }
                }
            )
            Text(
                text = if (isUpdate) stringResource(R.string.update_daily_reminder) else stringResource(
                    R.string.set_daily_reminder
                ),
                style = MaterialTheme.typography.bodyLarge
            )
        }

        if (setReminder) {
            val timeFormatter = remember { DateTimeFormatter.ofPattern("HH:mm") }
            OutlinedButton(
                onClick = { showTimePicker = true },
                modifier = Modifier.padding(start = 32.dp)
            ) {
                Text(stringResource(R.string.time_label) + ": ${reminderTime.format(timeFormatter)}")
            }
        }
    }

    if (showTimePicker) {
        val timeState = rememberTimePickerState(
            initialHour = reminderTime.hour,
            initialMinute = reminderTime.minute
        )
        TimePickerDialog(
            onDismissRequest = { showTimePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    onReminderTimeChange(LocalTime.of(timeState.hour, timeState.minute))
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

@Composable
fun LogNowEffect(
    id: Long?,
    reminderId: Long?,
    templateId: Long? = null,
    viewModel: AddEntryViewModel,
    onEntryFound: (HealthEntry, Boolean) -> Unit,
    onReminderTimeFound: ((LocalTime) -> Unit)? = null
) {
    LaunchedEffect(id, reminderId, templateId) {
        viewModel.loadEntry(id, reminderId, templateId)
    }

    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(uiState.entry) {
        uiState.entry?.let { entry ->
            onEntryFound(entry, uiState.isNewFromTemplate)
            if (uiState.reminder != null && onReminderTimeFound != null) {
                onReminderTimeFound(uiState.reminder!!.time)
            }
        }
    }
}

fun handleEntrySave(
    viewModel: AddEntryViewModel,
    existingEntry: HealthEntry?,
    isNewFromTemplate: Boolean,
    currentEntry: HealthEntry,
    setReminder: Boolean,
    reminderTime: LocalTime?,
    reminderTitle: String,
    onSaveSuccess: () -> Unit
) {
    if (setReminder && reminderTime != null) {
        val reminder = Reminder(
            id = existingEntry?.reminderId ?: 0,
            title = reminderTitle,
            time = reminderTime,
            daysOfWeek = setOf(1, 2, 3, 4, 5, 6, 7),
            isEnabled = true,
            entryType = currentEntry.type
        )
        viewModel.saveEntryWithReminder(currentEntry, reminder)
    } else {
        viewModel.saveEntry(currentEntry, if (isNewFromTemplate) null else existingEntry)
    }
    onSaveSuccess()
}
