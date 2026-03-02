package org.chronicheal.app.presentation

import android.graphics.PointF
import android.graphics.RectF
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.rememberTransformableState
import androidx.compose.foundation.gestures.transformable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asComposePath
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.chronicheal.app.domain.model.EntryType
import org.chronicheal.app.domain.model.HealthEntry
import org.chronicheal.app.presentation.util.SvgBodyParser
import org.chronicheal.app.presentation.util.SvgPath
import org.chronicheal.app.ui.theme.HeaderBlue
import org.chronicheal.app.ui.theme.PrimaryDark
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BodyScanScreen(
    onBackClick: () -> Unit,
    onRemindersClick: () -> Unit,
    viewModel: TimelineViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val sheetState = rememberModalBottomSheetState()
    var showBottomSheet by remember { mutableStateOf(false) }
    var selectedRegion by remember { mutableStateOf<String?>(null) }
    var existingEntryId by remember { mutableStateOf<Long?>(null) }
    var showMenu by remember { mutableStateOf(false) }

    // State for entry form
    var intensity by remember { mutableFloatStateOf(5f) }
    var note by remember { mutableStateOf("") }
    var logDate by remember { mutableStateOf(LocalDate.now()) }
    var startTime by remember { mutableStateOf(LocalTime.now()) }

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
                title = { Text("Body Scan") },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    Box {
                        IconButton(onClick = { showMenu = true }) {
                            Icon(Icons.Default.MoreVert, contentDescription = "Menu")
                        }
                        DropdownMenu(
                            expanded = showMenu,
                            onDismissRequest = { showMenu = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("Reminders") },
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
        Box(modifier = Modifier.padding(innerPadding).fillMaxSize().background(HeaderBlue)) {
            BodySilhouette(
                modifier = Modifier.fillMaxSize().padding(top = 16.dp),
                onRegionHold = { region, currentIntensity ->
                    val existing = uiState.entries.find { 
                        it.type == EntryType.PAIN && 
                        it.location?.equals(region, ignoreCase = true) == true && 
                        it.timestamp.atZone(ZoneId.systemDefault()).toLocalDate() == LocalDate.now() 
                    }
                    
                    if (existing != null) {
                        existingEntryId = existing.id
                        note = existing.note
                        logDate = existing.timestamp.atZone(ZoneId.systemDefault()).toLocalDate()
                        startTime = existing.timestamp.atZone(ZoneId.systemDefault()).toLocalTime()
                    } else {
                        existingEntryId = null
                        note = ""
                        logDate = LocalDate.now()
                        startTime = LocalTime.now()
                    }
                    
                    intensity = currentIntensity
                    selectedRegion = region
                },
                onRelease = {
                    showBottomSheet = true
                },
                painEntries = uiState.entries
                    .filter { it.type == EntryType.PAIN && it.timestamp.atZone(ZoneId.systemDefault()).toLocalDate() == LocalDate.now() }
            )

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
                                    intensity = intensity.toInt(),
                                    location = selectedRegion,
                                    note = note,
                                    timestamp = logDate.atTime(startTime).atZone(ZoneId.systemDefault()).toInstant()
                                )
                                if (existingEntryId == null) {
                                    viewModel.addEntry(entry)
                                    viewModel.showMessage("Pain log added for $selectedRegion")
                                } else {
                                    viewModel.updateEntry(entry)
                                    viewModel.showMessage("Pain log updated for $selectedRegion")
                                }
                                showBottomSheet = false
                            },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(if (existingEntryId == null) "Save Pain Log" else "Update Pain Log")
                        }

                        if (existingEntryId != null) {
                            FilledTonalIconButton(
                                onClick = {
                                    uiState.entries.find { it.id == existingEntryId }?.let {
                                        viewModel.deleteEntry(it)
                                        viewModel.showMessage("Pain log deleted for $selectedRegion")
                                    }
                                    showBottomSheet = false
                                },
                                colors = IconButtonDefaults.filledTonalIconButtonColors(
                                    containerColor = MaterialTheme.colorScheme.errorContainer,
                                    contentColor = MaterialTheme.colorScheme.error
                                )
                            ) {
                                Icon(Icons.Default.Delete, contentDescription = "Delete Pain Log")
                            }
                        }
                    }

                    PainEntryForm(
                        intensity = intensity,
                        onIntensityChange = { intensity = it },
                        location = selectedRegion ?: "",
                        onLocationChange = { selectedRegion = it },
                        note = note,
                        onNoteChange = { note = it },
                        logDate = logDate,
                        onDateChange = { logDate = it },
                        startTime = startTime,
                        onStartTimeChange = { startTime = it },
                        viewModel = viewModel
                    )
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

    var scale by remember { mutableFloatStateOf(1f) }
    var offset by remember { mutableStateOf(Offset.Zero) }
    val transformState = rememberTransformableState { zoomChange, offsetChange, _ ->
        scale = (scale * zoomChange).coerceIn(1f, 5f)
        offset += offsetChange
    }

    var currentHoldRegion by remember { mutableStateOf<String?>(null) }
    var currentHoldIntensity by remember { mutableFloatStateOf(1f) }
    val scope = rememberCoroutineScope()

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
                .transformable(state = transformState)
                .graphicsLayer(
                    scaleX = scale,
                    scaleY = scale,
                    translationX = offset.x,
                    translationY = offset.y
                )
                .pointerInput(svgPaths, bounds) {
                    detectTapGestures(
                        onPress = { tapOffset ->
                            val adjustedX = (tapOffset.x - offset.x) / scale
                            val adjustedY = (tapOffset.y - offset.y) / scale
                            
                            val scaleX = size.width / bounds.width()
                            val scaleY = size.height / bounds.height()
                            val baseScale = minOf(scaleX, scaleY)
                            
                            val offsetX = (size.width - bounds.width() * baseScale) / 2f
                            val offsetY = (size.height - bounds.height() * baseScale) / 2f

                            val invertedX = (adjustedX - offsetX) / baseScale + bounds.left
                            val invertedY = (adjustedY - offsetY) / baseScale + bounds.top

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
                            
                            tappedPath?.let { svgPath ->
                                val region = formatId(svgPath.id)
                                currentHoldRegion = region
                                val existing = painEntries.find { it.location?.equals(region, ignoreCase = true) == true }
                                currentHoldIntensity = existing?.intensity?.toFloat() ?: 1f
                                
                                onRegionHold(region, currentHoldIntensity)
                                
                                val job = scope.launch {
                                    while (currentHoldRegion != null) {
                                        delay(200)
                                        if (currentHoldIntensity < 10f) {
                                            currentHoldIntensity += 1f
                                            onRegionHold(region, currentHoldIntensity)
                                        }
                                    }
                                }
                                
                                tryAwaitRelease()
                                job.cancel()
                                currentHoldRegion = null
                                onRelease()
                            }
                        }
                    )
                }
        ) {
            val scaleX = size.width / bounds.width()
            val scaleY = size.height / bounds.height()
            val baseScale = minOf(scaleX, scaleY)
            
            val offsetX = (size.width - bounds.width() * baseScale) / 2f
            val offsetY = (size.height - bounds.height() * baseScale) / 2f

            val baseStrokeWidth = 1.dp.toPx()
            
            drawContext.canvas.nativeCanvas.save()
            drawContext.canvas.nativeCanvas.translate(offsetX, offsetY)
            drawContext.canvas.nativeCanvas.scale(baseScale, baseScale)
            drawContext.canvas.nativeCanvas.translate(-bounds.left, -bounds.top)

            svgPaths.forEach { svgPath ->
                val composePath = svgPath.path.asComposePath()
                val formattedId = formatId(svgPath.id)
                
                val intensity = if (currentHoldRegion == formattedId) {
                    currentHoldIntensity.toInt()
                } else {
                    painEntries.find { it.location?.equals(formattedId, ignoreCase = true) == true }?.intensity ?: 0
                }

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

        // Overlay intensity gauge when holding
        AnimatedVisibility(
            visible = currentHoldRegion != null,
            enter = fadeIn() + expandVertically(),
            exit = fadeOut() + shrinkVertically(),
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(16.dp)
                .height(200.dp)
                .width(50.dp)
        ) {
            VerticalIntensityGauge(
                intensity = currentHoldIntensity.toInt(),
                maxVal = 10,
                color = Color.Red,
                label = "INT",
                modifier = Modifier.background(Color.White.copy(alpha = 0.8f), RoundedCornerShape(8.dp))
            )
        }
    }
}

private fun parseSvgColor(colorStr: String?): Color? {
    if (colorStr == null || colorStr == "none") return null
    return try {
        // Handle hex colors specifically as parseColor expects them correctly
        if (colorStr.startsWith("#")) {
            // Fix for 3-digit hex if present (though not used in our current SVG)
            val normalized = if (colorStr.length == 4) {
                "#" + colorStr[1] + colorStr[1] + colorStr[2] + colorStr[2] + colorStr[3] + colorStr[3]
            } else colorStr
            Color(android.graphics.Color.parseColor(normalized))
        } else {
            // Try standard color names
            Color(android.graphics.Color.parseColor(colorStr))
        }
    } catch (_: Exception) {
        null
    }
}

private fun formatId(id: String): String {
    return id.replace("-", " ")
        .replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
        .replace(" R", " (Right)")
        .replace(" L", " (Left)")
}
