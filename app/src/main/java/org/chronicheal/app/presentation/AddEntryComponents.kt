package org.chronicheal.app.presentation

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.RectF
import android.os.Build
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asComposePath
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.PointerInputChange
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalViewConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.core.graphics.toColorInt
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.chronicheal.app.R
import org.chronicheal.app.domain.model.EntryType
import org.chronicheal.app.domain.model.HealthEntry
import org.chronicheal.app.domain.model.Reminder
import org.chronicheal.app.presentation.util.SvgBodyParser
import org.chronicheal.app.presentation.util.SvgPath
import org.chronicheal.app.ui.theme.HeaderBlue
import org.chronicheal.app.ui.theme.OnPrimaryContainerLight
import org.chronicheal.app.ui.theme.PrimaryContainerLight
import org.chronicheal.app.ui.theme.PrimaryDark
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import kotlin.math.abs
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
fun VoiceEnabledTextField(
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
            Text(stringResource(R.string.mood_summary_format, stringResource(R.string.type_mood), moodLabel, intensity.roundToInt()))
            Slider(value = intensity, onValueChange = onIntensityChange, valueRange = 1f..10f, steps = 8)
            VoiceEnabledTextField(value = note, onValueChange = onNoteChange, label = stringResource(R.string.section_mood))
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
                            when (ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS)) {
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
                text = if (isUpdate) stringResource(R.string.update_daily_reminder) else stringResource(R.string.set_daily_reminder),
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
    viewModel: TimelineViewModel,
    onEntryFound: (HealthEntry, Boolean) -> Unit,
    onReminderTimeFound: ((LocalTime) -> Unit)? = null
) {
    LaunchedEffect(id, reminderId) {
        if (id != null) {
            val entry = viewModel.getEntryById(id)
            if (entry != null) {
                onEntryFound(entry, false)
            }
        } else if (reminderId != null) {
            val entry = viewModel.getEntryByReminderId(reminderId)
            if (entry != null) {
                onEntryFound(entry, true)
                if (onReminderTimeFound != null) {
                    viewModel.getReminderById(reminderId)?.let { reminder ->
                        onReminderTimeFound(reminder.time)
                    }
                }
            }
        }
    }
}

fun handleEntrySave(
    viewModel: TimelineViewModel,
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
        if (currentEntry.id == 0L) {
            viewModel.addEntryWithReminder(currentEntry, reminder)
        } else {
            viewModel.updateEntryWithReminder(currentEntry, reminder)
        }
    } else {
        viewModel.saveEntryAndNotify(if (isNewFromTemplate) null else existingEntry, currentEntry)
    }
    onSaveSuccess()
}

@Composable
fun BodySilhouette(
    modifier: Modifier = Modifier,
    onRegionHold: (String, Float) -> Unit,
    onRelease: () -> Unit,
    painEntries: List<HealthEntry>
) {
    val context = LocalContext.current
    var svgPaths by remember { mutableStateOf<List<SvgPath>>(emptyList()) }
    var bounds by remember { mutableStateOf(RectF()) }

    val scope = rememberCoroutineScope()
    val viewConfiguration = LocalViewConfiguration.current

    LaunchedEffect(Unit) {
        val parser = SvgBodyParser(context)
        val paths = parser.parse("body-shape.svg")
        svgPaths = paths
        
        val allBounds = RectF()
        paths.forEach { 
            val pBounds = RectF()
            @Suppress("DEPRECATION")
            it.path.computeBounds(pBounds, true)
            allBounds.union(pBounds)
        }
        bounds = allBounds
    }

    if (svgPaths.isEmpty()) return

    Box(modifier = modifier) {
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(svgPaths, bounds) {
                    awaitEachGesture {
                        val down = awaitFirstDown(requireUnconsumed = false)
                        var isScrolling = false
                        var intensityJob: Job? = null
                        var currentHoldIntensity = 1f
                        
                        val scaleX = size.width / bounds.width()
                        val scaleY = size.height / bounds.height()
                        val baseScale = minOf(scaleX, scaleY)
                        
                        val offsetX = (size.width - bounds.width() * baseScale) / 2
                        val offsetY = (size.height - bounds.height() * baseScale) / 2

                        val invertedX = (down.position.x - offsetX) / baseScale + bounds.left
                        val invertedY = (down.position.y - offsetY) / baseScale + bounds.top

                        val tappedPath = svgPaths.findLast { svgPath ->
                            val region = android.graphics.Region()
                            val clip = android.graphics.Region(
                                (invertedX - 1).toInt(), 
                                (invertedY - 1).toInt(), 
                                (invertedX + 1).toInt(), 
                                (invertedY + 1).toInt()
                            )
                            region.setPath(svgPath.path, clip)
                            !region.isEmpty
                        }

                        if (tappedPath != null) {
                            val regionId = tappedPath.id
                            
                            // Start hold logic
                            intensityJob = scope.launch {
                                // Short delay to allow for scroll detection
                                delay(150)
                                if (!isScrolling) {
                                    val existing = painEntries.find { it.location?.equals(regionId, ignoreCase = true) == true }
                                    currentHoldIntensity = existing?.intensity?.toFloat() ?: 1f
                                    onRegionHold(regionId, currentHoldIntensity)
                                    
                                    while (true) {
                                        delay(200)
                                        if (currentHoldIntensity < 10f) {
                                            currentHoldIntensity += 1f
                                            onRegionHold(regionId, currentHoldIntensity)
                                        }
                                    }
                                }
                            }

                            // Tracking movement
                            var pointerChange: PointerInputChange? = null
                            do {
                                val event = awaitPointerEvent()
                                pointerChange = event.changes.firstOrNull { it.id == down.id }
                                if (pointerChange != null) {
                                    if (pointerChange.positionChange() != Offset.Zero) {
                                        val diff = pointerChange.position - down.position
                                        if (abs(diff.x) > viewConfiguration.touchSlop || abs(diff.y) > viewConfiguration.touchSlop) {
                                            isScrolling = true
                                            intensityJob.cancel()
                                        }
                                    }
                                }
                            } while (pointerChange != null && pointerChange.pressed)

                            intensityJob.cancel()
                            if (!isScrolling) {
                                onRelease()
                            }
                        }
                    }
                }
        ) {
            val scaleX = size.width / bounds.width()
            val scaleY = size.height / bounds.height()
            val baseScale = minOf(scaleX, scaleY)
            
            val offsetX = (size.width - bounds.width() * baseScale) / 2
            val offsetY = (size.height - bounds.height() * baseScale) / 2

            val baseStrokeWidth = 1.dp.toPx()
            
            drawContext.canvas.nativeCanvas.save()
            drawContext.canvas.nativeCanvas.translate(offsetX, offsetY)
            drawContext.canvas.nativeCanvas.scale(baseScale, baseScale)
            drawContext.canvas.nativeCanvas.translate(-bounds.left, -bounds.top)

            svgPaths.forEach { svgPath ->
                val composePath = svgPath.path.asComposePath()
                val regionId = svgPath.id
                
                val intensity = painEntries.find { it.location?.equals(regionId, ignoreCase = true) == true }?.intensity ?: 0

                val strokeColor = parseSvgColor(svgPath.stroke) ?: PrimaryDark
                val pathStrokeWidth = svgPath.strokeWidth ?: 1f

                if (intensity > 0) {
                    val alphaBase = 0.3f + (intensity / 10f) * 0.7f
                    drawPath(
                        path = composePath,
                        brush = Brush.verticalGradient(
                            listOf(
                                Color.Red.copy(alpha = alphaBase),
                                Color.Red.copy(alpha = alphaBase * 0.6f)
                            )
                        )
                    )
                } else {
                    val svgFill = parseSvgColor(svgPath.fill) ?: Color.White
                    drawPath(
                        path = composePath,
                        color = svgFill
                    )
                }

                drawPath(
                    path = composePath,
                    color = strokeColor,
                    style = Stroke(width = (pathStrokeWidth * baseStrokeWidth) / baseScale)
                )
                
                if (intensity > 0) {
                    val pBounds = RectF()
                    @Suppress("DEPRECATION")
                    svgPath.path.computeBounds(pBounds, true)
                    drawCircle(
                        color = Color.Red.copy(alpha = 0.8f),
                        radius = (4.dp + (intensity.dp * 0.8f)).toPx() / baseScale,
                        center = Offset(pBounds.centerX(), pBounds.centerY())
                    )
                }
            }
            drawContext.canvas.nativeCanvas.restore()
        }
    }
}

private fun parseSvgColor(colorStr: String?): Color? {
    if (colorStr == null || colorStr == "none") return null
    return try {
        if (colorStr.startsWith("#")) {
            val normalized = if (colorStr.length == 4) {
                "#" + colorStr[1] + colorStr[1] + colorStr[2] + colorStr[2] + colorStr[3] + colorStr[3]
            } else colorStr
            Color(normalized.toColorInt())
        } else {
            Color(colorStr.toColorInt())
        }
    } catch (_: Exception) {
        null
    }
}

fun formatId(context: Context, id: String): String {
    val resId = when(id.lowercase().trim()) {
        "foot-r" -> R.string.body_region_foot_R
        "ankle-r" -> R.string.body_region_ankle_R
        "tibia-r" -> R.string.body_region_tibia_R
        "knee-r" -> R.string.body_region_knee_R
        "thigh-r" -> R.string.body_region_thigh_R
        "foot-l" -> R.string.body_region_foot_L
        "ankle-l" -> R.string.body_region_ankle_L
        "tibia-l" -> R.string.body_region_tibia_L
        "knee-l" -> R.string.body_region_knee_L
        "thigh-l" -> R.string.body_region_thigh_L
        "pelvis" -> R.string.body_region_pelvis
        "torso" -> R.string.body_region_torso
        "elbow-r" -> R.string.body_region_helbow_R
        "wrist-r" -> R.string.body_region_wrist_R
        "arm-r-path" -> R.string.body_region_arm_R_path
        "forearm-r" -> R.string.body_region_forearm_R
        "palm-r" -> R.string.body_region_palm_R
        "fingers-r" -> R.string.body_region_fingers_R
        "shoulder-r" -> R.string.body_region_shoulder_R
        "elbow-l" -> R.string.body_region_elbow_L
        "wrist-l" -> R.string.body_region_wrist_L
        "arm-l-path" -> R.string.body_region_arm_L_path
        "forearm-l" -> R.string.body_region_forearm_L
        "palm-l" -> R.string.body_region_palm_L
        "fingers-l" -> R.string.body_region_fingers_L
        "shoulder-l" -> R.string.body_region_shoulder_L
        "brest" -> R.string.body_region_brest
        "neck" -> R.string.body_region_neck
        "head-part" -> R.string.body_region_head_part
        "forehead" -> R.string.body_region_forehead
        "ear-r" -> R.string.body_region_ear_R
        "ear-l" -> R.string.body_region_ear_R_5
        "eye-r" -> R.string.body_region_eye_R
        "eye-l" -> R.string.body_region_eye_L
        else -> null
    }
    
    return if (resId != null) context.getString(resId) else {
        id.replace("-", " ")
            .replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
    }
}
