package org.chronicheal.app.presentation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import org.chronicheal.app.R
import org.chronicheal.app.domain.model.EntryType
import org.chronicheal.app.domain.model.HealthEntry
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddBeverageScreen(
    dateString: String? = null,
    id: Long? = null,
    reminderId: Long? = null,
    templateId: Long? = null,
    onBackClick: () -> Unit,
    onSaveSuccess: () -> Unit,
    viewModel: TimelineViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    var name by rememberSaveable { mutableStateOf("") }
    var quantity by rememberSaveable { mutableStateOf("") }
    var selectedUnit by rememberSaveable { mutableStateOf(context.getString(R.string.unit_cup)) }
    var note by rememberSaveable { mutableStateOf("") }
    var logDate by rememberSaveable { mutableStateOf(if (dateString != null) LocalDate.parse(dateString) else LocalDate.now()) }
    var startTime by rememberSaveable { mutableStateOf(LocalTime.now()) }
    
    var setReminder by rememberSaveable { mutableStateOf(false) }
    var reminderTime by rememberSaveable { mutableStateOf(LocalTime.now()) }
    var isAlcoholic by rememberSaveable { mutableStateOf(false) }
    var isCaffeinated by rememberSaveable { mutableStateOf(false) }
    
    var existingEntry by remember { mutableStateOf<HealthEntry?>(null) }
    var isNewFromTemplate by remember { mutableStateOf(false) }

    val nameSuggestions by viewModel.beverageSuggestions.collectAsState()

    val unitOptions = listOf(stringResource(R.string.unit_cup), stringResource(R.string.unit_ml))
    var unitMenuExpanded by remember { mutableStateOf(false) }

    LogNowEffect(
        id = id, 
        reminderId = reminderId,
        viewModel = viewModel,
        onEntryFound = { entry, fromTemplate ->
            existingEntry = entry
            isNewFromTemplate = fromTemplate
            name = entry.name ?: ""
            if (entry.value != null) {
                quantity = if (entry.value == entry.value.toLong().toDouble()) entry.value.toLong().toString() else entry.value.toString()
                selectedUnit = entry.unit ?: context.getString(R.string.unit_cup)
            } else {
                quantity = entry.unit ?: ""
            }
            note = entry.note
            if (!isNewFromTemplate) {
                logDate = entry.timestamp.atZone(ZoneId.systemDefault()).toLocalDate()
                startTime = entry.timestamp.atZone(ZoneId.systemDefault()).toLocalTime()
            }
            setReminder = entry.hasReminder
            isAlcoholic = entry.isAlcoholic ?: false
            isCaffeinated = entry.isCaffeinated ?: false
        },
        onReminderTimeFound = { reminderTime = it }
    )

    val createEntry = {
        HealthEntry(
            id = if (isNewFromTemplate) 0 else (id ?: 0),
            timestamp = logDate.atTime(startTime).atZone(ZoneId.systemDefault()).toInstant(),
            type = EntryType.BEVERAGE,
            name = name.trim(),
            value = quantity.replace(",", ".").toDoubleOrNull(),
            unit = selectedUnit,
            note = note,
            hasReminder = setReminder,
            reminderId = existingEntry?.reminderId,
            durationMinutes = existingEntry?.durationMinutes,
            isAlcoholic = isAlcoholic,
            isCaffeinated = isCaffeinated
        )
    }

    val handleSave = {
        handleEntrySave(
            viewModel = viewModel,
            existingEntry = existingEntry,
            isNewFromTemplate = isNewFromTemplate,
            currentEntry = createEntry(),
            setReminder = setReminder,
            reminderTime = reminderTime,
            reminderTitle = context.getString(R.string.type_beverage) + ": $name",
            onSaveSuccess = onSaveSuccess
        )
    }

    AddEntryScaffold(
        title = if (id == null || isNewFromTemplate) stringResource(R.string.log_beverage) else stringResource(R.string.edit_beverage),
        existingEntry = if (isNewFromTemplate) null else existingEntry,
        currentEntry = createEntry,
        onBackClick = onBackClick,
        onSaveSuccess = onSaveSuccess,
        onDeleteClick = {
            existingEntry?.let { viewModel.deleteEntry(it) }
            onBackClick()
        },
        viewModel = viewModel,
        onSave = handleSave
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp)
        ) {
            EntryDateTimePicker(
                date = logDate,
                onDateChange = { logDate = it },
                startTime = startTime,
                onStartTimeChange = { startTime = it }
            )

            Spacer(modifier = Modifier.height(16.dp))

            AutoCompleteTextField(
                value = name,
                onValueChange = { name = it },
                suggestions = nameSuggestions,
                label = stringResource(R.string.beverage_name_label)
            )

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = quantity,
                    onValueChange = { quantity = it },
                    label = { Text(stringResource(R.string.quantity)) },
                    modifier = Modifier.weight(1f),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true
                )

                ExposedDropdownMenuBox(
                    expanded = unitMenuExpanded,
                    onExpandedChange = { unitMenuExpanded = !unitMenuExpanded },
                    modifier = Modifier.weight(1f)
                ) {
                    OutlinedTextField(
                        value = selectedUnit,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text(stringResource(R.string.unit)) },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = unitMenuExpanded) },
                        modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable, true)
                    )
                    ExposedDropdownMenu(
                        expanded = unitMenuExpanded,
                        onDismissRequest = { unitMenuExpanded = false }
                    ) {
                        unitOptions.forEach { option ->
                            DropdownMenuItem(
                                text = { Text(option) },
                                onClick = {
                                    selectedUnit = option
                                    unitMenuExpanded = false
                                }
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Checkbox(checked = isAlcoholic, onCheckedChange = { isAlcoholic = it })
                Text(text = stringResource(R.string.is_alcoholic), style = MaterialTheme.typography.bodyLarge)
                Spacer(modifier = Modifier.padding(8.dp))
                Checkbox(checked = isCaffeinated, onCheckedChange = { isCaffeinated = it })
                Text(text = stringResource(R.string.is_caffeinated), style = MaterialTheme.typography.bodyLarge)
            }

            Spacer(modifier = Modifier.height(16.dp))

            VoiceEnabledTextField(
                value = note,
                onValueChange = { note = it },
                label = stringResource(R.string.notes_label),
                minLines = 3
            )

            Spacer(modifier = Modifier.height(24.dp))

            ReminderSection(
                setReminder = setReminder,
                onSetReminderChange = { setReminder = it },
                reminderTime = reminderTime,
                onReminderTimeChange = { reminderTime = it },
                isUpdate = existingEntry?.hasReminder == true
            )
        }
    }
}
