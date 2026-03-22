package org.chronicheal.app.presentation

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import kotlinx.coroutines.launch
import org.chronicheal.app.R
import org.chronicheal.app.domain.model.Allergen
import org.chronicheal.app.domain.model.EntryType
import org.chronicheal.app.domain.model.Fodmap
import org.chronicheal.app.domain.model.HealthEntry
import org.chronicheal.app.domain.model.Ingredient
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddMealScreen(
    dateString: String? = null,
    id: Long? = null,
    reminderId: Long? = null,
    onBackClick: () -> Unit,
    onSaveSuccess: () -> Unit,
    viewModel: TimelineViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var name by rememberSaveable { mutableStateOf("") }
    var note by rememberSaveable { mutableStateOf("") }
    var logDate by rememberSaveable { mutableStateOf(if (dateString != null) LocalDate.parse(dateString) else LocalDate.now()) }
    var startTime by rememberSaveable { mutableStateOf(LocalTime.now()) }
    var existingEntry by remember { mutableStateOf<HealthEntry?>(null) }
    var isNewFromTemplate by remember { mutableStateOf(false) }

    val ingredients = remember { mutableStateListOf<Ingredient>() }
    val selectedAllergenIds = remember { mutableStateListOf<String>() }
    val selectedFodmapIds = remember { mutableStateListOf<String>() }

    var proteins by rememberSaveable { mutableStateOf<Double?>(null) }
    var carbohydrates by rememberSaveable { mutableStateOf<Double?>(null) }
    var lipids by rememberSaveable { mutableStateOf<Double?>(null) }

    var setReminder by rememberSaveable { mutableStateOf(false) }
    var reminderTime by rememberSaveable { mutableStateOf(LocalTime.now()) }
    var advancedOptionsExpanded by rememberSaveable { mutableStateOf(false) }

    val uiState by viewModel.uiState.collectAsState()
    val allergenOrderFromSettings by viewModel.allergenOrder.collectAsState()
    val deactivatedAllergenIds by viewModel.deactivatedAllergens.collectAsState()
    val deactivatedFodmapIds by viewModel.deactivatedFodmaps.collectAsState()
    
    var currentVisibleAllergenIds by remember { mutableStateOf(emptyList<String>()) }
    var currentVisibleFodmapIds by remember { mutableStateOf(emptyList<String>()) }

    // AI State
    var isAnalyzing by remember { mutableStateOf(false) }
    var showDownloadDialog by remember { mutableStateOf(false) }
    var showMeteredWarning by remember { mutableStateOf(false) }
    val isDownloading by viewModel.isDownloadingModel.collectAsState()
    val downloadProgress by viewModel.modelDownloadProgress.collectAsState()

    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) viewModel.downloadModel()
    }

    LaunchedEffect(uiState.message) {
        uiState.message?.let {
            Toast.makeText(context, it, Toast.LENGTH_SHORT).show()
            viewModel.clearMessage()
        }
    }

    LaunchedEffect(allergenOrderFromSettings, deactivatedAllergenIds) {
        val baseOrder = if (allergenOrderFromSettings.isEmpty()) {
            Allergen.allIds
        } else {
            val saved = allergenOrderFromSettings
            val extras = Allergen.allIds.filter { it !in saved }
            saved + extras
        }
        currentVisibleAllergenIds = baseOrder.filter { it !in deactivatedAllergenIds }
    }

    LaunchedEffect(deactivatedFodmapIds) {
        currentVisibleFodmapIds = Fodmap.allIds.filter { it !in deactivatedFodmapIds }
    }

    val focusManager = LocalFocusManager.current
    val lastItemFocusRequester = remember { FocusRequester() }
    var shouldFocusNewIngredient by remember { mutableStateOf(false) }

    LogNowEffect(
        id = id, 
        reminderId = reminderId,
        viewModel = viewModel,
        onEntryFound = { entry, fromTemplate ->
            existingEntry = entry
            isNewFromTemplate = fromTemplate
            name = entry.name ?: ""
            note = entry.note
            if (!isNewFromTemplate) {
                logDate = entry.timestamp.atZone(ZoneId.systemDefault()).toLocalDate()
                startTime = entry.timestamp.atZone(ZoneId.systemDefault()).toLocalTime()
            }
            setReminder = entry.hasReminder
            proteins = entry.proteins
            carbohydrates = entry.carbohydrates
            lipids = entry.lipids
            
            ingredients.clear()
            entry.ingredients?.let { ingredients.addAll(it) }
            
            selectedAllergenIds.clear()
            entry.allergens?.let { selectedAllergenIds.addAll(it) }

            selectedFodmapIds.clear()
            entry.fodmaps?.let { selectedFodmapIds.addAll(it) }
        },
        onReminderTimeFound = { reminderTime = it }
    )

    LaunchedEffect(ingredients.size) {
        if (shouldFocusNewIngredient && ingredients.isNotEmpty()) {
            try {
                lastItemFocusRequester.requestFocus()
            } catch (_: Exception) {
                // Focus request might fail if not attached yet
            }
            shouldFocusNewIngredient = false
        }
    }

    val addNewIngredient = {
        ingredients.add(Ingredient("", unit = "g"))
        shouldFocusNewIngredient = true
    }

    val createEntry = {
        HealthEntry(
            id = if (isNewFromTemplate) 0 else (existingEntry?.id ?: 0),
            timestamp = logDate.atTime(startTime).atZone(ZoneId.systemDefault()).toInstant(),
            type = EntryType.MEAL,
            name = name.trim(),
            note = note,
            hasReminder = setReminder,
            reminderId = existingEntry?.reminderId,
            durationMinutes = existingEntry?.durationMinutes,
            ingredients = if (ingredients.isEmpty()) null else ingredients.toList(),
            allergens = if (selectedAllergenIds.isEmpty()) null else selectedAllergenIds.toList(),
            fodmaps = if (selectedFodmapIds.isEmpty()) null else selectedFodmapIds.toList(),
            proteins = proteins,
            carbohydrates = carbohydrates,
            lipids = lipids
        )
    }

    val handleSave: () -> Unit = {
        handleEntrySave(
            viewModel = viewModel,
            existingEntry = existingEntry,
            isNewFromTemplate = isNewFromTemplate,
            currentEntry = createEntry(),
            setReminder = setReminder,
            reminderTime = reminderTime,
            reminderTitle = name.trim().ifBlank { context.getString(R.string.type_meal) },
            onSaveSuccess = onSaveSuccess
        )
    }

    val startDownloadProcess = {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.POST_NOTIFICATIONS
                ) == PackageManager.PERMISSION_GRANTED
            ) {
                viewModel.downloadModel()
            } else {
                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        } else {
            viewModel.downloadModel()
        }
    }

    val handleAiAssist: () -> Unit = {
        if (viewModel.isModelPresent) {
            scope.launch {
                isAnalyzing = true
                val analysis = viewModel.analyzeMeal(name.ifBlank { note })
                if (analysis != null) {
                    if (name.isBlank()) name = analysis.name ?: ""
                    proteins = analysis.proteins
                    carbohydrates = analysis.carbohydrates
                    lipids = analysis.lipids
                    
                    analysis.ingredients?.let { aiIngs ->
                        aiIngs.forEach { aiIng ->
                            if (ingredients.none {
                                    it.name.equals(
                                        aiIng.name,
                                        ignoreCase = true
                                    )
                                }) {
                                ingredients.add(Ingredient(aiIng.name, aiIng.quantity, aiIng.unit))
                            }
                        }
                    }

                    // Only add allergens if they are currently visible/activated in settings
                    if (currentVisibleAllergenIds.isNotEmpty()) {
                        analysis.allergens?.let { aiAllergens ->
                            aiAllergens.forEach { allergenId ->
                                if (allergenId in currentVisibleAllergenIds && allergenId !in selectedAllergenIds) {
                                    selectedAllergenIds.add(allergenId)
                                }
                            }
                        }
                    }

                    // Only add fodmaps if they are currently visible/activated in settings
                    if (currentVisibleFodmapIds.isNotEmpty()) {
                        analysis.fodmaps?.let { aiFodmaps ->
                            aiFodmaps.forEach { fodmapId ->
                                if (fodmapId in currentVisibleFodmapIds && fodmapId !in selectedFodmapIds) {
                                    selectedFodmapIds.add(fodmapId)
                                }
                            }
                        }
                    }
                } else {
                    viewModel.showMessage(context.getString(R.string.ai_error_parsing))
                }
                isAnalyzing = false
            }
        } else {
            showDownloadDialog = true
        }
    }

    if (showDownloadDialog) {
        AlertDialog(
            onDismissRequest = { if (!isDownloading) showDownloadDialog = false },
            title = { Text(stringResource(R.string.ai_download_now_confirm)) },
            text = {
                Column {
                    Text(stringResource(R.string.ai_download_model_desc))

                    Spacer(modifier = Modifier.height(16.dp))

                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    Icons.Default.Info,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    stringResource(R.string.ai_model_info_title),
                                    style = MaterialTheme.typography.titleSmall
                                )
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                stringResource(R.string.ai_model_name),
                                style = MaterialTheme.typography.bodySmall
                            )
                            Text(
                                stringResource(R.string.ai_model_source),
                                style = MaterialTheme.typography.bodySmall
                            )
                            Text(
                                stringResource(R.string.ai_model_license),
                                style = MaterialTheme.typography.bodySmall
                            )
                            Text(
                                stringResource(R.string.ai_model_url),
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }

                    if (isDownloading) {
                        Spacer(modifier = Modifier.height(16.dp))
                        if (downloadProgress >= 0) {
                            LinearProgressIndicator(
                                progress = { downloadProgress },
                                modifier = Modifier.fillMaxWidth(),
                            )
                            Text(
                                text = stringResource(
                                    R.string.ai_downloading,
                                    (downloadProgress * 100).toInt()
                                ),
                                style = MaterialTheme.typography.bodySmall,
                                modifier = Modifier.align(Alignment.End)
                            )
                        } else {
                            LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                            Text(
                                text = stringResource(R.string.ai_downloading_indeterminate),
                                style = MaterialTheme.typography.bodySmall,
                                modifier = Modifier.align(Alignment.End)
                            )
                        }
                    }
                }
            },
            confirmButton = {
                if (!isDownloading) {
                    Button(onClick = {
                        if (viewModel.isMeteredConnection()) {
                            showMeteredWarning = true
                        } else {
                            startDownloadProcess()
                        }
                    }) {
                        Text(stringResource(R.string.ai_download_now))
                    }
                }
            },
            dismissButton = {
                if (!isDownloading) {
                    TextButton(onClick = { }) {
                        Text(stringResource(R.string.cancel))
                    }
                }
            }
        )
    }

    if (showMeteredWarning) {
        AlertDialog(
            onDismissRequest = { showMeteredWarning = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.Warning,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.error
                    )
                    Spacer(Modifier.width(8.dp))
                    Text("Mobile Data Warning")
                }
            },
            text = { Text("You are on a metered connection (Mobile Data). This download is large (~1GB) and may incur charges. Do you want to proceed?") },
            confirmButton = {
                Button(
                    onClick = {
                        showMeteredWarning = false
                        startDownloadProcess()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Download anyway")
                }
            },
            dismissButton = {
                TextButton(onClick = { showMeteredWarning = false }) {
                    Text("Wait for Wi-Fi")
                }
            }
        )
    }

    AddEntryScaffold(
        title = if (id == null || isNewFromTemplate) stringResource(R.string.log_meal) else stringResource(R.string.edit_meal),
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
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp)
        ) {
            item {
                EntryDateTimePicker(
                    date = logDate,
                    onDateChange = { logDate = it },
                    startTime = startTime,
                    onStartTimeChange = { startTime = it }
                )

                Spacer(modifier = Modifier.height(16.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text(stringResource(R.string.meal_description_label)) },
                        minLines = 2,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                        keyboardActions = KeyboardActions(onNext = {
                            focusManager.moveFocus(
                                FocusDirection.Down
                            )
                        }),
                        modifier = Modifier.weight(1f)
                    )

                    Spacer(modifier = Modifier.width(8.dp))

                    IconButton(
                        onClick = handleAiAssist,
                        enabled = !isAnalyzing && (name.isNotBlank() || note.isNotBlank())
                    ) {
                        if (isAnalyzing) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(24.dp),
                                strokeWidth = 2.dp
                            )
                        } else {
                            Icon(
                                Icons.Default.AutoAwesome,
                                contentDescription = stringResource(R.string.ai_assist),
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))
                Text(
                    text = stringResource(R.string.nutrition_label),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = proteins?.toString() ?: "",
                        onValueChange = { proteins = it.toDoubleOrNull() },
                        label = { Text(stringResource(R.string.proteins_label)) },
                        modifier = Modifier.weight(1f),
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Decimal,
                            imeAction = ImeAction.Next
                        ),
                        suffix = { Text("g") }
                    )
                    OutlinedTextField(
                        value = carbohydrates?.toString() ?: "",
                        onValueChange = { carbohydrates = it.toDoubleOrNull() },
                        label = { Text(stringResource(R.string.carbohydrates_label)) },
                        modifier = Modifier.weight(1f),
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Decimal,
                            imeAction = ImeAction.Next
                        ),
                        suffix = { Text("g") }
                    )
                    OutlinedTextField(
                        value = lipids?.toString() ?: "",
                        onValueChange = { lipids = it.toDoubleOrNull() },
                        label = { Text(stringResource(R.string.lipids_label)) },
                        modifier = Modifier.weight(1f),
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Decimal,
                            imeAction = ImeAction.Next
                        ),
                        suffix = { Text("g") }
                    )
                }

                if (currentVisibleAllergenIds.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(24.dp))
                    Text(
                        text = stringResource(R.string.allergens),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }

            if (currentVisibleAllergenIds.isNotEmpty()) {
                itemsIndexed(currentVisibleAllergenIds) { index, allergenId ->
                    val allergen = Allergen.fromId(allergenId) ?: return@itemsIndexed
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Checkbox(
                            checked = allergenId in selectedAllergenIds,
                            onCheckedChange = {
                                if (it) selectedAllergenIds.add(allergenId) else selectedAllergenIds.remove(
                                    allergenId
                                )
                            }
                        )
                        Text(
                            text = stringResource(allergen.displayRes),
                            modifier = Modifier.weight(1f),
                            style = MaterialTheme.typography.bodyLarge
                        )
                        IconButton(
                            onClick = {
                                if (index > 0) {
                                    val newList = currentVisibleAllergenIds.toMutableList()
                                    val temp = newList[index]
                                    newList[index] = newList[index - 1]
                                    newList[index - 1] = temp
                                    currentVisibleAllergenIds = newList
                                    viewModel.setAllergenOrder(newList)
                                }
                            },
                            enabled = index > 0,
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(
                                Icons.Default.ArrowUpward,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                        IconButton(
                            onClick = {
                                if (index < currentVisibleAllergenIds.size - 1) {
                                    val newList = currentVisibleAllergenIds.toMutableList()
                                    val temp = newList[index]
                                    newList[index] = newList[index + 1]
                                    newList[index + 1] = temp
                                    currentVisibleAllergenIds = newList
                                    viewModel.setAllergenOrder(newList)
                                }
                            },
                            enabled = index < currentVisibleAllergenIds.size - 1,
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(
                                Icons.Default.ArrowDownward,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }

            if (currentVisibleFodmapIds.isNotEmpty()) {
                item {
                    Spacer(modifier = Modifier.height(24.dp))
                    Text(
                        text = stringResource(R.string.fodmaps),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                }

                itemsIndexed(currentVisibleFodmapIds) { _, fodmapId ->
                    val fodmap = Fodmap.fromId(fodmapId) ?: return@itemsIndexed
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Checkbox(
                            checked = fodmapId in selectedFodmapIds,
                            onCheckedChange = {
                                if (it) selectedFodmapIds.add(fodmapId) else selectedFodmapIds.remove(
                                    fodmapId
                                )
                            }
                        )
                        Text(
                            text = stringResource(fodmap.displayRes),
                            modifier = Modifier.weight(1f),
                            style = MaterialTheme.typography.bodyLarge
                        )
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(24.dp))
                Text(
                    text = stringResource(R.string.ingredients_label),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(8.dp))
            }

            itemsIndexed(ingredients) { index, ingredient ->
                IngredientItem(
                    ingredient = ingredient,
                    onIngredientChange = { updated -> ingredients[index] = updated },
                    onRemove = { ingredients.removeAt(index) },
                    isLast = index == ingredients.size - 1,
                    onAddNew = { addNewIngredient() },
                    nameFocusRequester = if (index == ingredients.size - 1 && shouldFocusNewIngredient) lastItemFocusRequester else null
                )
                Spacer(modifier = Modifier.height(8.dp))
            }

            item {
                OutlinedButton(
                    onClick = { addNewIngredient() },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.Add, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(stringResource(R.string.add_ingredient))
                }
                
                Spacer(modifier = Modifier.height(24.dp))

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                    )
                ) {
                    Column(modifier = Modifier.padding(8.dp)) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { advancedOptionsExpanded = !advancedOptionsExpanded }
                                .padding(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = stringResource(R.string.advanced_options),
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.weight(1f)
                            )
                            Icon(
                                imageVector = if (advancedOptionsExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }

                        AnimatedVisibility(visible = advancedOptionsExpanded) {
                            Column(modifier = Modifier.padding(8.dp)) {
                                VoiceEnabledTextField(
                                    value = note,
                                    onValueChange = { note = it },
                                    label = stringResource(R.string.notes_label),
                                    minLines = 3,
                                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                                    keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Down) })
                                )

                                Spacer(modifier = Modifier.height(16.dp))

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
                }

                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }
}

@Composable
fun IngredientItem(
    ingredient: Ingredient,
    onIngredientChange: (Ingredient) -> Unit,
    onRemove: () -> Unit,
    isLast: Boolean,
    onAddNew: () -> Unit,
    nameFocusRequester: FocusRequester? = null
) {
    val focusManager = LocalFocusManager.current
    
    Column {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            val nameModifier = if (nameFocusRequester != null) {
                Modifier
                    .weight(2f)
                    .focusRequester(nameFocusRequester)
            } else {
                Modifier.weight(2f)
            }

            OutlinedTextField(
                value = ingredient.name,
                onValueChange = { onIngredientChange(ingredient.copy(name = it)) },
                label = { Text(stringResource(R.string.ingredient_name)) },
                modifier = nameModifier,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Next) })
            )

            OutlinedTextField(
                value = ingredient.quantity?.let { if (it == it.toLong().toDouble()) it.toLong().toString() else it.toString() } ?: "",
                onValueChange = { 
                    val qty = it.toDoubleOrNull()
                    onIngredientChange(ingredient.copy(quantity = qty))
                },
                label = { Text(stringResource(R.string.quantity)) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal, imeAction = ImeAction.Next),
                keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Next) }),
                modifier = Modifier.weight(1f)
            )

            OutlinedTextField(
                value = ingredient.unit ?: "",
                onValueChange = { onIngredientChange(ingredient.copy(unit = it.ifBlank { null })) },
                label = { Text(stringResource(R.string.unit)) },
                modifier = Modifier.weight(1f),
                keyboardOptions = KeyboardOptions(imeAction = if (isLast) ImeAction.Done else ImeAction.Next),
                keyboardActions = KeyboardActions(
                    onNext = { focusManager.moveFocus(FocusDirection.Next) },
                    onDone = {
                        if (isLast) {
                            onAddNew()
                        } else {
                            focusManager.moveFocus(FocusDirection.Next)
                        }
                    }
                )
            )

            IconButton(
                onClick = onRemove,
                modifier = Modifier.size(24.dp)
            ) {
                Icon(Icons.Default.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error)
            }
        }
        HorizontalDivider(modifier = Modifier.padding(top = 8.dp))
    }
}
