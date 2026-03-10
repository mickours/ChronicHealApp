package org.chronicheal.app.presentation

import android.content.Context
import android.graphics.RectF
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
import androidx.hilt.navigation.compose.hiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.chronicheal.app.R
import org.chronicheal.app.domain.model.EntryType
import org.chronicheal.app.domain.model.HealthEntry
import org.chronicheal.app.presentation.util.SvgBodyParser
import org.chronicheal.app.presentation.util.SvgPath
import org.chronicheal.app.ui.theme.HeaderBlue
import org.chronicheal.app.ui.theme.PrimaryDark
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import kotlin.math.abs
import androidx.core.graphics.toColorInt
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BodyScanScreen(
    dateString: String? = null,
    onBackClick: () -> Unit,
    onRemindersClick: () -> Unit,
    viewModel: TimelineViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsState()
    val sheetState = rememberModalBottomSheetState()
    var showBottomSheet by remember { mutableStateOf(false) }
    var selectedRegionId by remember { mutableStateOf<String?>(null) }
    var existingEntryId by remember { mutableStateOf<Long?>(null) }
    var showMenu by remember { mutableStateOf(false) }

    var logDate by rememberSaveable { mutableStateOf(if (dateString != null) LocalDate.parse(dateString) else LocalDate.now()) }
    var startTime by rememberSaveable { mutableStateOf(LocalTime.now()) }

    // Pain State (Temporary Bottom Sheet State)
    var painIntensity by remember { mutableFloatStateOf(5f) }
    var painNote by remember { mutableStateOf("") }

    var currentHoldRegionId by remember { mutableStateOf<String?>(null) }
    var currentHoldIntensity by remember { mutableFloatStateOf(1f) }

    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(uiState.message) {
        uiState.message?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearMessage()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.type_pain)) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back))
                    }
                },
                actions = {
                    Box {
                        IconButton(onClick = { showMenu = true }) {
                            Icon(Icons.Default.MoreVert, contentDescription = stringResource(R.string.menu))
                        }
                        DropdownMenu(
                            expanded = showMenu,
                            onDismissRequest = { showMenu = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.reminders_title)) },
                                onClick = {
                                    showMenu = false
                                    onRemindersClick()
                                },
                                leadingIcon = {
                                    Icon(Icons.Default.Notifications, contentDescription = null)
                                }
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = HeaderBlue,
                    titleContentColor = Color.Black,
                    navigationIconContentColor = Color.Black,
                    actionIconContentColor = Color.Black
                )
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { innerPadding ->
        Box(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.surface)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                EntryDateTimePicker(
                    date = logDate,
                    onDateChange = { newDate -> logDate = newDate },
                    startTime = startTime,
                    onStartTimeChange = { newTime -> startTime = newTime }
                )

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    colors = CardDefaults.cardColors(containerColor = HeaderBlue),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    BodySilhouette(
                        modifier = Modifier.fillMaxSize(),
                        onRegionHold = { regionId, currentIntensity ->
                            val existing = uiState.entries.find { 
                                it.type == EntryType.PAIN && 
                                it.location?.equals(regionId, ignoreCase = true) == true && 
                                it.timestamp.atZone(ZoneId.systemDefault()).toLocalDate() == logDate 
                            }
                            
                            if (existing != null) {
                                existingEntryId = existing.id
                                painNote = existing.note
                            } else {
                                existingEntryId = null
                                painNote = ""
                            }
                            
                            painIntensity = currentIntensity
                            selectedRegionId = regionId
                            
                            currentHoldRegionId = regionId
                            currentHoldIntensity = currentIntensity
                        },
                        onRelease = {
                            currentHoldRegionId = null
                            showBottomSheet = true
                        },
                        painEntries = uiState.entries
                            .filter { it.type == EntryType.PAIN && it.timestamp.atZone(ZoneId.systemDefault()).toLocalDate() == logDate }
                    )
                }
            }

            // Intensity gauge fixed at top right
            AnimatedVisibility(
                visible = currentHoldRegionId != null,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically(),
                modifier = Modifier
                    .padding(16.dp)
                    .height(200.dp)
                    .width(50.dp)
                    .align(Alignment.TopEnd)
            ) {
                VerticalIntensityGauge(
                    intensity = currentHoldIntensity.toInt(),
                    maxVal = 10,
                    color = Color.Red,
                    label = stringResource(R.string.intensity_short_label),
                    modifier = Modifier.background(Color.White.copy(alpha = 0.8f), RoundedCornerShape(8.dp))
                )
            }
        }

        if (showBottomSheet) {
            ModalBottomSheet(
                onDismissRequest = { showBottomSheet = false },
                sheetState = sheetState
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = {
                            val entry = HealthEntry(
                                id = existingEntryId ?: 0,
                                type = EntryType.PAIN,
                                intensity = painIntensity.toInt(),
                                location = selectedRegionId, // Save technical ID
                                note = painNote,
                                timestamp = logDate.atTime(startTime).atZone(ZoneId.systemDefault()).toInstant()
                            )
                            if (existingEntryId == null) {
                                viewModel.addEntry(entry)
                                viewModel.showMessage(context.getString(R.string.pain_log_added, formatId(context, selectedRegionId ?: "")))
                            } else {
                                viewModel.updateEntry(entry)
                                viewModel.showMessage(context.getString(R.string.pain_log_updated, formatId(context, selectedRegionId ?: "")))
                            }
                            showBottomSheet = false
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(if (existingEntryId == null) stringResource(R.string.save_pain_log) else stringResource(R.string.update_pain_log))
                    }

                    if (existingEntryId != null) {
                        FilledTonalIconButton(
                            onClick = {
                                uiState.entries.find { it.id == existingEntryId }?.let {
                                    viewModel.deleteEntry(it)
                                    viewModel.showMessage(context.getString(R.string.pain_log_deleted, formatId(context, selectedRegionId ?: "")))
                                }
                                showBottomSheet = false
                            },
                            colors = IconButtonDefaults.filledTonalIconButtonColors(
                                containerColor = MaterialTheme.colorScheme.errorContainer,
                                contentColor = MaterialTheme.colorScheme.error
                            )
                        ) {
                            Icon(Icons.Default.Delete, contentDescription = stringResource(R.string.delete))
                        }
                    }
                }

                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = formatId(context, selectedRegionId ?: ""),
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(Modifier.height(16.dp))
                    Text(stringResource(R.string.intensity_label, painIntensity.roundToInt()))
                    Slider(value = painIntensity, onValueChange = { newPainIntensity -> painIntensity = newPainIntensity }, valueRange = 1f..10f, steps = 8)
                    VoiceEnabledTextField(value = painNote, onValueChange = { newPainNote -> painNote = newPainNote }, label = stringResource(R.string.notes_label))
                    Spacer(Modifier.height(32.dp))
                }
            }
        }
    }
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
                                            intensityJob?.cancel()
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
        "helbow-r" -> R.string.body_region_helbow_R
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
        "ear-r-5" -> R.string.body_region_ear_R_5
        "eye-r" -> R.string.body_region_eye_R
        "eye-l" -> R.string.body_region_eye_L
        else -> null
    }
    
    return if (resId != null) context.getString(resId) else {
        id.replace("-", " ")
            .replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
    }
}
