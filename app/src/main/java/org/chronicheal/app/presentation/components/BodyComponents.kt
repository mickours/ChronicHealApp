package org.chronicheal.app.presentation.components

import android.content.Context
import android.graphics.RectF
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.graphics.toColorInt
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.chronicheal.app.R
import org.chronicheal.app.domain.model.HealthEntry
import org.chronicheal.app.presentation.util.SvgBodyParser
import org.chronicheal.app.presentation.util.SvgPath
import org.chronicheal.app.ui.theme.PrimaryDark
import kotlin.math.abs

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
                color,
                color.copy(alpha = 0.4f)
            )
        )
    }

    Column(
        modifier = modifier
            .fillMaxHeight()
            .width(40.dp)
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
                .width(16.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(
                    if (isDark) Color.DarkGray.copy(alpha = 0.3f) else Color.LightGray.copy(
                        alpha = 0.3f
                    )
                ),
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
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Black,
            color = color,
            modifier = Modifier.padding(vertical = 4.dp)
        )
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

                            intensityJob = scope.launch {
                                delay(150)
                                if (!isScrolling) {
                                    val existing = painEntries.find {
                                        it.location?.equals(
                                            regionId,
                                            ignoreCase = true
                                        ) == true
                                    }
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

                val intensity = painEntries.find {
                    it.location?.equals(
                        regionId,
                        ignoreCase = true
                    ) == true
                }?.intensity ?: 0

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
    val resId = when (id.lowercase().trim()) {
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
