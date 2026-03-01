package org.chronicheal.app.presentation

import android.graphics.PointF
import android.graphics.RectF
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.rememberTransformableState
import androidx.compose.foundation.gestures.transformable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
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
import androidx.compose.runtime.setValue
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
import org.chronicheal.app.domain.model.EntryType
import org.chronicheal.app.presentation.util.SvgBodyParser
import org.chronicheal.app.presentation.util.SvgPath
import org.chronicheal.app.ui.theme.HeaderBlue
import org.chronicheal.app.ui.theme.PrimaryOrange
import java.time.LocalDate
import java.time.LocalTime

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BodyScanScreen(
    onBackClick: () -> Unit,
    viewModel: TimelineViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val sheetState = rememberModalBottomSheetState()
    var showBottomSheet by remember { mutableStateOf(false) }
    var selectedRegion by remember { mutableStateOf<String?>(null) }
    var tapLocation by remember { mutableStateOf<PointF?>(null) }

    // State for entry form
    var intensity by remember { mutableFloatStateOf(5f) }
    var note by remember { mutableStateOf("") }
    var logDate by remember { mutableStateOf(LocalDate.now()) }
    var startTime by remember { mutableStateOf(LocalTime.now()) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Body Scan") },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = HeaderBlue,
                    titleContentColor = Color.Black,
                    navigationIconContentColor = Color.Black
                )
            )
        }
    ) { innerPadding ->
        Box(modifier = Modifier.padding(innerPadding).fillMaxSize()) {
            BodySilhouette(
                modifier = Modifier.fillMaxSize(),
                onRegionClick = { region, point ->
                    selectedRegion = region
                    tapLocation = point
                    showBottomSheet = true
                },
                painMarkers = uiState.entries
                    .filter { it.type == EntryType.PAIN && it.timestamp.atZone(java.time.ZoneId.systemDefault()).toLocalDate() == LocalDate.now() }
                    .mapNotNull { it.location }
            )

            if (showBottomSheet) {
                ModalBottomSheet(
                    onDismissRequest = { showBottomSheet = false },
                    sheetState = sheetState
                ) {
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
                    androidx.compose.material3.Button(
                        onClick = {
                            viewModel.addEntry(
                                org.chronicheal.app.domain.model.HealthEntry(
                                    type = EntryType.PAIN,
                                    intensity = intensity.toInt(),
                                    location = selectedRegion,
                                    note = note,
                                    timestamp = logDate.atTime(startTime).atZone(java.time.ZoneId.systemDefault()).toInstant()
                                )
                            )
                            showBottomSheet = false
                        },
                        modifier = Modifier.fillMaxWidth().padding(16.dp)
                    ) {
                        Text("Save Pain Log")
                    }
                }
            }
        }
    }
}

@Composable
fun BodySilhouette(
    modifier: Modifier = Modifier,
    onRegionClick: (String, PointF) -> Unit,
    painMarkers: List<String>
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

    Canvas(
        modifier = modifier
            .transformable(state = transformState)
            .graphicsLayer(
                scaleX = scale,
                scaleY = scale,
                translationX = offset.x,
                translationY = offset.y
            )
            .pointerInput(svgPaths, bounds) {
                detectTapGestures { tapOffset ->
                    // Adjust tap offset for zoom/pan
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
                    
                    tappedPath?.let { 
                        onRegionClick(formatId(it.id), PointF(invertedX, invertedY)) 
                    }
                }
            }
    ) {
        val scaleX = size.width / bounds.width()
        val scaleY = size.height / bounds.height()
        val baseScale = minOf(scaleX, scaleY)
        
        val offsetX = (size.width - bounds.width() * baseScale) / 2f
        val offsetY = (size.height - bounds.height() * baseScale) / 2f

        val strokeWidth = 1.dp.toPx()
        val fillBrush = Brush.verticalGradient(
            colors = listOf(
                PrimaryOrange.copy(alpha = 0.9f),
                PrimaryOrange.copy(alpha = 0.4f)
            )
        )

        drawContext.canvas.nativeCanvas.save()
        drawContext.canvas.nativeCanvas.translate(offsetX, offsetY)
        drawContext.canvas.nativeCanvas.scale(baseScale, baseScale)
        drawContext.canvas.nativeCanvas.translate(-bounds.left, -bounds.top)

        svgPaths.forEach { svgPath ->
            val composePath = svgPath.path.asComposePath()
            val isPained = painMarkers.any { marker ->
                formatId(svgPath.id).contains(marker, ignoreCase = true) || 
                marker.contains(formatId(svgPath.id), ignoreCase = true)
            }

            drawPath(
                path = composePath,
                brush = if (isPained) Brush.verticalGradient(listOf(Color.Red, Color.Red.copy(alpha = 0.5f))) else fillBrush
            )
            drawPath(
                path = composePath,
                color = Color.Black,
                style = Stroke(width = strokeWidth / baseScale)
            )
            
            // Draw pain circle indicator if this path is marked
            if (isPained) {
                val pBounds = RectF()
                svgPath.path.computeBounds(pBounds, true)
                drawCircle(
                    color = Color.Red,
                    radius = 8.dp.toPx() / baseScale,
                    center = Offset(pBounds.centerX(), pBounds.centerY())
                )
            }
        }
        drawContext.canvas.nativeCanvas.restore()
    }
}

private fun formatId(id: String): String {
    return id.replace("-", " ")
        .replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
        .replace(" R", " (Right)")
        .replace(" L", " (Left)")
}
