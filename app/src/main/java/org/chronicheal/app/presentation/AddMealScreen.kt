package org.chronicheal.app.presentation

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import kotlinx.coroutines.launch
import org.chronicheal.app.R
import org.chronicheal.app.domain.model.EntryType
import org.chronicheal.app.domain.model.HealthEntry
import org.chronicheal.app.domain.model.Ingredient
import org.chronicheal.app.domain.usecase.GetSuggestionsUseCase
import org.chronicheal.app.presentation.components.AddEntryScaffold
import org.chronicheal.app.presentation.components.AutoCompleteTextField
import org.chronicheal.app.presentation.components.EntryDateTimePicker
import org.chronicheal.app.presentation.components.LogNowEffect
import org.chronicheal.app.presentation.components.ReminderSection
import org.chronicheal.app.presentation.components.VoiceEnabledTextField
import org.chronicheal.app.presentation.components.handleEntrySave
import org.chronicheal.app.ui.theme.HeaderBlue
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun AddMealScreen(
    dateString: String? = null,
    id: Long? = null,
    reminderId: Long? = null,
    templateId: Long? = null,
    onBackClick: () -> Unit,
    onSaveSuccess: () -> Unit,
    viewModel: AddEntryViewModel = hiltViewModel(),
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var name by rememberSaveable { mutableStateOf("") }
    var note by rememberSaveable { mutableStateOf("") }
    var logDate by rememberSaveable {
        mutableStateOf(
            if (dateString != null) LocalDate.parse(
                dateString
            ) else LocalDate.now()
        )
    }
    var startTime by rememberSaveable { mutableStateOf(LocalTime.now()) }

    var setReminder by rememberSaveable { mutableStateOf(value = false) }
    var reminderTime by rememberSaveable { mutableStateOf(LocalTime.now()) }
    
    val uiState by viewModel.uiState.collectAsState()
    val existingEntry = uiState.entry
    val isNewFromTemplate = uiState.isNewFromTemplate

    val ingredients = remember { mutableStateListOf<Ingredient>() }
    var showAdvancedOptions by rememberSaveable { mutableStateOf(value = false) }
    var selectedAllergens by remember { mutableStateOf(setOf<String>()) }
    var selectedFodmaps by remember { mutableStateOf(setOf<String>()) }

    var proteins by rememberSaveable { mutableStateOf("") }
    var carbs by rememberSaveable { mutableStateOf("") }
    var lipids by rememberSaveable { mutableStateOf("") }

    val nameSuggestions by viewModel.getSuggestions(
        setOf(EntryType.MEAL),
        GetSuggestionsUseCase.SuggestionField.NAME
    ).collectAsState()

    val allergenOrder by viewModel.allergenOrder.collectAsState()
    val deactivatedAllergens by viewModel.deactivatedAllergens.collectAsState()
    val deactivatedFodmaps by viewModel.deactivatedFodmaps.collectAsState()

    val availableAllergens = allergenOrder.filter { it !in deactivatedAllergens }
    val allFodmaps = listOf(
        "Fructans" to R.string.fodmap_fructans,
        "GOS" to R.string.fodmap_gos,
        "Fructose" to R.string.fodmap_fructose,
        "Sorbitol" to R.string.fodmap_sorbitol,
        "Mannitol" to R.string.fodmap_mannitol,
        "Lactose" to R.string.fodmap_lactose
    ).filter { it.first !in deactivatedFodmaps }

    val unitOptions = listOf(
        stringResource(R.string.unit_g),
        stringResource(R.string.unit_ml),
        stringResource(R.string.unit_cup),
        stringResource(R.string.unit_spoon)
    )

    // AI Analysis State
    val isAiEnabled = uiState.isAiEnabled
    val isModelPresent = viewModel.isModelPresent()
    val isDownloadingModel by viewModel.isDownloadingModel.collectAsState()
    val modelDownloadProgress by viewModel.modelDownloadProgress.collectAsState()
    var isAnalyzing by remember { mutableStateOf(value = false) }
    var showDownloadDialog by remember { mutableStateOf(false) }

    LogNowEffect(
        id = id,
        reminderId = reminderId,
        templateId = templateId,
        viewModel = viewModel,
        onEntryFound = { entry, fromTemplate ->
            name = entry.name ?: ""
            note = entry.note
            if (!fromTemplate) {
                logDate = entry.timestamp.atZone(ZoneId.systemDefault()).toLocalDate()
                startTime = entry.timestamp.atZone(ZoneId.systemDefault()).toLocalTime()
            }
            ingredients.clear()
            entry.ingredients?.let { ingredients.addAll(it) }
            selectedAllergens = entry.allergens?.toSet() ?: emptySet()
            selectedFodmaps = entry.fodmaps?.toSet() ?: emptySet()
            proteins = entry.proteins?.let {
                if (it == it.toLong().toDouble()) it.toLong().toString() else it.toString()
            } ?: ""
            carbs = entry.carbohydrates?.let {
                if (it == it.toLong().toDouble()) it.toLong().toString() else it.toString()
            } ?: ""
            lipids = entry.lipids?.let {
                if (it == it.toLong().toDouble()) it.toLong().toString() else it.toString()
            } ?: ""
            setReminder = entry.hasReminder

            if (ingredients.isNotEmpty() || selectedAllergens.isNotEmpty() || selectedFodmaps.isNotEmpty() || proteins.isNotEmpty()) {
                showAdvancedOptions = true
            }
        }
    ) { reminderTime = it }

    val createEntry = {
        HealthEntry(
            id = if (isNewFromTemplate) 0 else (existingEntry?.id ?: 0),
            timestamp = logDate.atTime(startTime).atZone(ZoneId.systemDefault()).toInstant(),
            type = EntryType.MEAL,
            name = name.trim(),
            note = note,
            ingredients = ingredients.toList().takeIf { it.isNotEmpty() },
            allergens = selectedAllergens.toList().takeIf { it.isNotEmpty() },
            fodmaps = selectedFodmaps.toList().takeIf { it.isNotEmpty() },
            proteins = proteins.replace(",", ".").toDoubleOrNull(),
            carbohydrates = carbs.replace(",", ".").toDoubleOrNull(),
            lipids = lipids.replace(",", ".").toDoubleOrNull(),
            hasReminder = setReminder,
            reminderId = existingEntry?.reminderId,
            durationMinutes = existingEntry?.durationMinutes
        )
    }

    val analyzeMealDescription = {
        if (!isModelPresent && !isDownloadingModel) {
            showDownloadDialog = true
        } else if (isModelPresent) {
            scope.launch {
                isAnalyzing = true
                val analysis = viewModel.analyzeMeal(note)
                if (analysis != null) {
                    analysis.ingredients?.let { aiIngredients ->
                        ingredients.clear()
                        ingredients.addAll(
                            aiIngredients.map {
                                Ingredient(it.name, it.quantity, it.unit)
                            }
                        )
                    }
                    analysis.allergens?.let { aiAllergens ->
                        // Match returned allergens with available keys (case-insensitive)
                        val matched = aiAllergens.mapNotNull { aiAllergen ->
                            availableAllergens.find { it.equals(aiAllergen, ignoreCase = true) }
                        }
                        selectedAllergens = matched.toSet()
                    }
                    analysis.nutrition?.let { nutrition ->
                        nutrition.proteins?.let { proteins = it.toString() }
                        nutrition.carbohydrates?.let { carbs = it.toString() }
                        nutrition.lipids?.let { lipids = it.toString() }
                    }
                    showAdvancedOptions = true
                } else {
                    viewModel.showMessage(context.getString(R.string.ai_error_parsing))
                }
                isAnalyzing = false
            }
        }
    }

    if (showDownloadDialog) {
        AlertDialog(
            onDismissRequest = {
            },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.AutoAwesome,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(stringResource(R.string.ai_model_required))
                }
            },
            text = { 
                Column {
                    Text(stringResource(R.string.ai_download_model_desc))
                    if (viewModel.isMeteredConnection()) {
                        Spacer(Modifier.height(8.dp))
                        Text(
                            "Warning: You are on a metered connection. This download is large.",
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.downloadModel()
                    }
                ) {
                    Text(stringResource(R.string.ai_download_now_confirm))
                }
            },
            dismissButton = {
                TextButton(onClick = { }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }

    AddEntryScaffold(
        title = if ((id == null) || isNewFromTemplate) stringResource(R.string.log_meal) else stringResource(
            R.string.edit_meal
        ),
        hasExistingEntry = (!isNewFromTemplate) && (existingEntry != null),
        onBackClick = onBackClick,
        onSaveClick = {
            handleEntrySave(
                viewModel = viewModel,
                existingEntry = existingEntry,
                isNewFromTemplate = isNewFromTemplate,
                currentEntry = createEntry(),
                setReminder = setReminder,
                reminderTime = if (setReminder) reminderTime else null,
                reminderTitle = context.getString(R.string.type_meal) + ": $name",
                onSaveSuccess = onSaveSuccess
            )
        },
        onDeleteClick = {
            existingEntry?.let { viewModel.deleteEntry(it) }
            onBackClick()
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
                .verticalScroll(rememberScrollState())
        ) {
            EntryDateTimePicker(
                date = logDate,
                onDateChange = { logDate = it },
                startTime = startTime
            ) { startTime = it }

            Spacer(modifier = Modifier.height(16.dp))

            AutoCompleteTextField(
                value = name,
                onValueChange = { name = it },
                suggestions = nameSuggestions,
                label = stringResource(R.string.meal_name_label)
            )

            Spacer(modifier = Modifier.height(16.dp))

            VoiceEnabledTextField(
                value = note,
                onValueChange = { note = it },
                label = stringResource(R.string.meal_description_label),
                minLines = 3
            )

            if (isAiEnabled && note.isNotBlank()) {
                Spacer(modifier = Modifier.height(8.dp))
                if (isDownloadingModel) {
                    ElevatedCard(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    Icons.Default.Download,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary
                                )
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    if (modelDownloadProgress > 0)
                                        stringResource(
                                            R.string.ai_downloading,
                                            (modelDownloadProgress * 100).toInt()
                                        )
                                    else
                                        stringResource(R.string.ai_downloading_indeterminate),
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            if (modelDownloadProgress > 0) {
                                Spacer(Modifier.height(8.dp))
                                LinearProgressIndicator(
                                    progress = { modelDownloadProgress },
                                    modifier = Modifier.fillMaxWidth(),
                                )
                            }
                        }
                    }
                } else {
                    Button(
                        onClick = analyzeMealDescription,
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !isAnalyzing,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = HeaderBlue,
                            contentColor = Color.Black
                        )
                    ) {
                        if (isAnalyzing) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                color = Color.Black,
                                strokeWidth = 2.dp
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(stringResource(R.string.ai_analyzing))
                        } else {
                            Icon(Icons.Default.AutoAwesome, contentDescription = null)
                            Spacer(Modifier.width(8.dp))
                            Text(if (!isModelPresent) stringResource(R.string.ai_download_now) else "Analyze Meal")
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { showAdvancedOptions = !showAdvancedOptions }
                    .padding(vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = stringResource(R.string.advanced_options),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Icon(
                    imageVector = if (showAdvancedOptions) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                    contentDescription = null
                )
            }

            if (showAdvancedOptions) {
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
                        // Ingredients
                        Text(
                            stringResource(R.string.ingredients_label),
                            fontWeight = FontWeight.Bold
                        )
                        ingredients.forEachIndexed { index, ingredient ->
                            var isMenuExpanded by remember { mutableStateOf(false) }
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                OutlinedTextField(
                                    value = ingredient.name,
                                    onValueChange = {
                                        ingredients[index] = ingredient.copy(name = it)
                                    },
                                    label = { Text(stringResource(R.string.ingredient_name)) },
                                    modifier = Modifier.weight(2f),
                                    singleLine = true
                                )
                                OutlinedTextField(
                                    value = ingredient.quantity?.let {
                                        if (it == it.toLong().toDouble()) it.toLong()
                                            .toString() else it.toString()
                                    } ?: "",
                                    onValueChange = {
                                        ingredients[index] = ingredient.copy(
                                            quantity = it.replace(",", ".").toDoubleOrNull()
                                        )
                                    },
                                    label = { Text(stringResource(R.string.quantity)) },
                                    modifier = Modifier.weight(1f),
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                    singleLine = true
                                )
                                ExposedDropdownMenuBox(
                                    expanded = isMenuExpanded,
                                    onExpandedChange = { isMenuExpanded = !isMenuExpanded },
                                    modifier = Modifier.weight(1.5f)
                                ) {
                                    OutlinedTextField(
                                        value = ingredient.unit ?: "",
                                        onValueChange = {
                                            ingredients[index] = ingredient.copy(unit = it)
                                        },
                                        label = { Text(stringResource(R.string.unit)) },
                                        trailingIcon = {
                                            ExposedDropdownMenuDefaults.TrailingIcon(
                                                expanded = isMenuExpanded
                                            )
                                        },
                                        modifier = Modifier.menuAnchor(
                                            MenuAnchorType.PrimaryEditable,
                                            true
                                        ),
                                        singleLine = true
                                    )
                                    ExposedDropdownMenu(
                                        expanded = isMenuExpanded,
                                        onDismissRequest = { isMenuExpanded = false }
                                    ) {
                                        unitOptions.forEach { option ->
                                            DropdownMenuItem(
                                                text = { Text(option) },
                                                onClick = {
                                                    ingredients[index] =
                                                        ingredient.copy(unit = option)
                                                    isMenuExpanded = false
                                                }
                                            )
                                        }
                                    }
                                }
                                IconButton(
                                    onClick = { ingredients.removeAt(index) },
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(
                                        Icons.Default.Delete,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.error
                                    )
                                }
                            }
                        }
                        TextButton(
                            onClick = { ingredients.add(Ingredient("")) },
                            modifier = Modifier.align(Alignment.Start)
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null)
                            Spacer(Modifier.width(4.dp))
                            Text(stringResource(R.string.add_ingredient))
                        }

                        // Allergens
                        if (availableAllergens.isNotEmpty()) {
                            Text(stringResource(R.string.allergens), fontWeight = FontWeight.Bold)
                            FlowRow(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                availableAllergens.forEach { key ->
                                    val isSelected = selectedAllergens.contains(key)
                                    val displayRes = context.resources.getIdentifier(
                                        "allergen_${key.lowercase()}",
                                        "string",
                                        context.packageName
                                    )
                                    val displayText =
                                        if (displayRes != 0) stringResource(displayRes) else key
                                    FilterChip(
                                        selected = isSelected,
                                        onClick = {
                                            selectedAllergens =
                                                if (isSelected) selectedAllergens - key else selectedAllergens + key
                                        },
                                        label = { Text(displayText) },
                                        colors = FilterChipDefaults.filterChipColors(
                                            selectedContainerColor = MaterialTheme.colorScheme.errorContainer,
                                            selectedLabelColor = MaterialTheme.colorScheme.onErrorContainer
                                        )
                                    )
                                }
                            }
                        } else {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    Icons.Default.Info,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp),
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    "Allergens tracking is disabled in settings.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        // FODMAPs
                        if (allFodmaps.isNotEmpty()) {
                            Text(stringResource(R.string.fodmaps), fontWeight = FontWeight.Bold)
                            FlowRow(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                allFodmaps.forEach { (key, displayRes) ->
                                    val isSelected = selectedFodmaps.contains(key)
                                    FilterChip(
                                        selected = isSelected,
                                        onClick = {
                                            selectedFodmaps =
                                                if (isSelected) selectedFodmaps - key else selectedFodmaps + key
                                        },
                                        label = { Text(stringResource(displayRes)) },
                                        colors = FilterChipDefaults.filterChipColors(
                                            selectedContainerColor = MaterialTheme.colorScheme.tertiaryContainer,
                                            selectedLabelColor = MaterialTheme.colorScheme.onTertiaryContainer
                                        )
                                    )
                                }
                            }
                        } else {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    Icons.Default.Info,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp),
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    "FODMAP tracking is disabled in settings.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        // Macros
                        Text(stringResource(R.string.nutrition_label), fontWeight = FontWeight.Bold)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(
                                value = proteins,
                                onValueChange = { proteins = it },
                                label = { Text(stringResource(R.string.proteins_label)) },
                                modifier = Modifier.weight(1f),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                suffix = { Text("g") },
                                singleLine = true
                            )
                            OutlinedTextField(
                                value = carbs,
                                onValueChange = { carbs = it },
                                label = { Text(stringResource(R.string.carbohydrates_label)) },
                                modifier = Modifier.weight(1f),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                suffix = { Text("g") },
                                singleLine = true
                            )
                            OutlinedTextField(
                                value = lipids,
                                onValueChange = { lipids = it },
                                label = { Text(stringResource(R.string.lipids_label)) },
                                modifier = Modifier.weight(1f),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                suffix = { Text("g") },
                                singleLine = true
                            )
                        }
                    }
                }
            }

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
