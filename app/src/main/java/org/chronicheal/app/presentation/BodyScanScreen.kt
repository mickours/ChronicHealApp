package org.chronicheal.app.presentation

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asComposePath
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import org.chronicheal.app.presentation.util.SvgBodyParser
import org.chronicheal.app.presentation.util.SvgPath
import org.chronicheal.app.ui.theme.HeaderBlue
import org.chronicheal.app.ui.theme.PrimaryOrange
import android.graphics.RectF
import androidx.compose.ui.graphics.nativeCanvas

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BodyScanScreen(
    onBackClick: () -> Unit,
    onRegionClick: (String) -> Unit
) {
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
        BodySilhouette(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp),
            onRegionClick = onRegionClick
        )
    }
}

@Composable
fun BodySilhouette(
    modifier: Modifier = Modifier,
    onRegionClick: (String) -> Unit
) {
    val context = LocalContext.current
    var svgPaths by remember { mutableStateOf<List<SvgPath>>(emptyList()) }
    var bounds by remember { mutableStateOf(RectF()) }

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
            .pointerInput(svgPaths, bounds) {
                detectTapGestures { offset ->
                    val scaleX = size.width / bounds.width()
                    val scaleY = size.height / bounds.height()
                    val scale = minOf(scaleX, scaleY)
                    
                    val offsetX = (size.width - bounds.width() * scale) / 2f
                    val offsetY = (size.height - bounds.height() * scale) / 2f

                    val invertedX = (offset.x - offsetX) / scale + bounds.left
                    val invertedY = (offset.y - offsetY) / scale + bounds.top

                    val region = svgPaths.findLast { svgPath ->
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
                    
                    region?.let { onRegionClick(formatId(it.id)) }
                }
            }
    ) {
        val scaleX = size.width / bounds.width()
        val scaleY = size.height / bounds.height()
        val scale = minOf(scaleX, scaleY)
        
        val offsetX = (size.width - bounds.width() * scale) / 2f
        val offsetY = (size.height - bounds.height() * scale) / 2f

        val strokeWidth = 1.dp.toPx()
        val fillBrush = Brush.verticalGradient(
            colors = listOf(
                PrimaryOrange.copy(alpha = 0.9f),
                PrimaryOrange.copy(alpha = 0.4f)
            )
        )

        drawContext.canvas.nativeCanvas.save()
        drawContext.canvas.nativeCanvas.translate(offsetX, offsetY)
        drawContext.canvas.nativeCanvas.scale(scale, scale)
        drawContext.canvas.nativeCanvas.translate(-bounds.left, -bounds.top)

        svgPaths.forEach { svgPath ->
            val composePath = svgPath.path.asComposePath()
            // Draw fill
            drawPath(
                path = composePath,
                brush = fillBrush
            )
            // Draw stroke
            drawPath(
                path = composePath,
                color = Color.Black,
                style = Stroke(width = strokeWidth / scale)
            )
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
