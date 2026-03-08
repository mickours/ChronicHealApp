package org.chronicheal.app.presentation

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import kotlinx.coroutines.launch
import org.chronicheal.app.R
import org.chronicheal.app.domain.model.EntryType
import org.chronicheal.app.domain.model.Reminder
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun AddReminderScreen(
    id: Long? = null,
    initialType: EntryType? = null,
    onBackClick: () -> Unit,
    onSaveSuccess: () -> Unit,
    viewModel: RemindersViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    
    val bodyScanTitle = stringResource(R.string.body_scan_title)
    var title by remember { mutableStateOf(if (initialType == EntryType.PAIN) bodyScanTitle else "") }
    var selectedTime by remember { mutableStateOf(LocalTime.now()) }
    var selectedDays by remember { mutableStateOf(setOf(1, 2, 3, 4, 5, 6, 7)) }
    var selectedType by remember { mutableStateOf<EntryType?>(initialType) }
    var showTimePicker by remember { mutableStateOf(false) }
    var existingReminder by remember { mutableStateOf<Reminder?>(null) }

    LaunchedEffect(id) {
        if (id != null) {
            viewModel.getReminderById(id)?.let { reminder ->
                existingReminder = reminder
                title = reminder.title
                selectedTime = reminder.time
                selectedDays = reminder.daysOfWeek
                selectedType = reminder.entryType
            }
        }
    }

    val timeState = rememberTimePickerState(
        initialHour = selectedTime.hour,
        initialMinute = selectedTime.minute
    )

    val defaultReminderTitle = stringResource(R.string.app_name) // Fallback
    val saveReminderAction = {
        val reminder = Reminder(
            id = id ?: 0,
            title = title.ifBlank { defaultReminderTitle },
            time = selectedTime,
            daysOfWeek = selectedDays,
            entryType = selectedType,
            isEnabled = existingReminder?.isEnabled ?: true
        )
        if (id == null) {
            viewModel.addReminder(reminder)
        } else {
            viewModel.updateReminder(reminder)
        }
        onSaveSuccess()
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            saveReminderAction()
        } else {
            scope.launch {
                snackbarHostState.showSnackbar(context.getString(R.string.notification_permission_required))
            }
        }
    }

    Scaffold(
        modifier = Modifier.systemBarsPadding(),
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text(if (id == null) stringResource(R.string.add_reminder) else stringResource(R.string.edit_appointment)) }, // Use generic edit?
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back))
                    }
                },
                actions = {
                    Button(
                        onClick = {
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                when (ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS)) {
                                    PackageManager.PERMISSION_GRANTED -> saveReminderAction()
                                    else -> permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                                }
                            } else {
                                saveReminderAction()
                            }
                        },
                        modifier = Modifier.padding(end = 8.dp)
                    ) {
                        Text(stringResource(R.string.save))
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
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                label = { Text(stringResource(R.string.name_label)) },
                modifier = Modifier.fillMaxWidth()
            )

            Text(stringResource(R.string.time_label), style = MaterialTheme.typography.titleMedium)
            
            val timeFormatter = remember { DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT) }
            Button(
                onClick = { showTimePicker = true },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.secondaryContainer,
                    contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                )
            ) {
                Text(
                    text = selectedTime.format(timeFormatter),
                    style = MaterialTheme.typography.headlineMedium
                )
            }

            Text(stringResource(R.string.reminders_title), style = MaterialTheme.typography.titleMedium)
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                val dayLabels = listOf(
                    stringResource(R.string.day_mon).take(1),
                    stringResource(R.string.day_tue).take(1),
                    stringResource(R.string.day_wed).take(1),
                    stringResource(R.string.day_thu).take(1),
                    stringResource(R.string.day_fri).take(1),
                    stringResource(R.string.day_sat).take(1),
                    stringResource(R.string.day_sun).take(1)
                )
                for (i in 1..7) {
                    val isSelected = selectedDays.contains(i)
                    FilterChip(
                        selected = isSelected,
                        onClick = {
                            selectedDays = if (isSelected) {
                                selectedDays - i
                            } else {
                                selectedDays + i
                            }
                        },
                        label = { Text(dayLabels[i - 1]) }
                    )
                }
            }

            Text(stringResource(R.string.track_selection_title), style = MaterialTheme.typography.titleMedium)
            
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                EntryType.entries.forEach { type ->
                    FilterChip(
                        selected = selectedType == type,
                        onClick = {
                            selectedType = if (selectedType == type) null else type
                        },
                        label = { Text(stringResource(type.displayRes)) }
                    )
                }
            }
        }

        if (showTimePicker) {
            TimePickerDialog(
                onDismissRequest = { showTimePicker = false },
                confirmButton = {
                    TextButton(onClick = {
                        selectedTime = LocalTime.of(timeState.hour, timeState.minute)
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
