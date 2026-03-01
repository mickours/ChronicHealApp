package org.chronicheal.app.presentation

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp

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
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Tap on a body part to log pain or symptom",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.outline
            )
            
            Spacer(modifier = Modifier.height(32.dp))

            BodySilhouette(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                onRegionClick = onRegionClick
            )
        }
    }
}

@Composable
fun BodySilhouette(
    modifier: Modifier = Modifier,
    onRegionClick: (String) -> Unit
) {
    Canvas(
        modifier = modifier
            .pointerInput(Unit) {
                detectTapGestures { offset ->
                    val canvasSize = Size(size.width.toFloat(), size.height.toFloat())
                    val region = detectRegion(offset, canvasSize)
                    if (region != null) {
                        onRegionClick(region)
                    }
                }
            }
    ) {
        val strokeWidth = 2.dp.toPx()
        val bodyColor = Color.LightGray
        
        drawBody(bodyColor, strokeWidth)
    }
}

private fun DrawScope.drawBody(color: Color, strokeWidth: Float) {
    val centerX = size.width / 2
    val headRadius = size.width * 0.1f
    val bodyWidth = size.width * 0.3f
    val bodyHeight = size.height * 0.4f
    
    // Head
    drawCircle(
        color = color,
        radius = headRadius,
        center = Offset(centerX, margin + headRadius),
        style = Stroke(width = strokeWidth)
    )
    
    // Torso
    drawRect(
        color = color,
        topLeft = Offset(centerX - bodyWidth / 2, margin + headRadius * 2 + 10f),
        size = Size(bodyWidth, bodyHeight),
        style = Stroke(width = strokeWidth)
    )
    
    // Arms
    val armWidth = 20f
    val armHeight = bodyHeight * 0.8f
    drawRect(
        color = color,
        topLeft = Offset(centerX - bodyWidth / 2 - armWidth - 10f, margin + headRadius * 2 + 20f),
        size = Size(armWidth, armHeight),
        style = Stroke(width = strokeWidth)
    )
    drawRect(
        color = color,
        topLeft = Offset(centerX + bodyWidth / 2 + 10f, margin + headRadius * 2 + 20f),
        size = Size(armWidth, armHeight),
        style = Stroke(width = strokeWidth)
    )
    
    // Legs
    val legWidth = bodyWidth * 0.4f
    val legHeight = size.height * 0.35f
    drawRect(
        color = color,
        topLeft = Offset(centerX - bodyWidth / 2, margin + headRadius * 2 + bodyHeight + 20f),
        size = Size(legWidth, legHeight),
        style = Stroke(width = strokeWidth)
    )
    drawRect(
        color = color,
        topLeft = Offset(centerX + bodyWidth / 2 - legWidth, margin + headRadius * 2 + bodyHeight + 20f),
        size = Size(legWidth, legHeight),
        style = Stroke(width = strokeWidth)
    )
}

private val margin = 40f

private fun detectRegion(offset: Offset, size: Size): String? {
    val centerX = size.width / 2
    val headRadius = size.width * 0.1f
    val bodyWidth = size.width * 0.3f
    val bodyHeight = size.height * 0.4f
    
    // Check Head
    val headCenter = Offset(centerX, margin + headRadius)
    if (offset.distanceTo(headCenter) <= headRadius) return "Head"
    
    // Check Torso
    val torsoTop = margin + headRadius * 2 + 10f
    if (offset.x in (centerX - bodyWidth / 2)..(centerX + bodyWidth / 2) &&
        offset.y in torsoTop..(torsoTop + bodyHeight)) return "Torso"
        
    // Simple check for arms and legs
    if (offset.y > torsoTop && offset.y < torsoTop + bodyHeight) {
        if (offset.x < centerX - bodyWidth / 2) return "Left Arm"
        if (offset.x > centerX + bodyWidth / 2) return "Right Arm"
    }
    
    if (offset.y > torsoTop + bodyHeight) {
        if (offset.x in (centerX - bodyWidth / 2)..(centerX)) return "Left Leg"
        if (offset.x in (centerX)..(centerX + bodyWidth / 2)) return "Right Leg"
    }

    return null
}

private fun Offset.distanceTo(other: Offset): Float {
    return kotlin.math.sqrt((x - other.x) * (x - other.x) + (y - other.y) * (y - other.y))
}
