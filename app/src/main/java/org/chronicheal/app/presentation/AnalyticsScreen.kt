package org.chronicheal.app.presentation

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.patrykandpatrick.vico.compose.axis.horizontal.rememberBottomAxis
import com.patrykandpatrick.vico.compose.axis.vertical.rememberStartAxis
import com.patrykandpatrick.vico.compose.chart.Chart
import com.patrykandpatrick.vico.compose.chart.column.columnChart
import com.patrykandpatrick.vico.compose.chart.line.lineChart
import com.patrykandpatrick.vico.compose.component.lineComponent
import com.patrykandpatrick.vico.compose.component.shape.shader.verticalGradient
import com.patrykandpatrick.vico.core.axis.AxisItemPlacer
import com.patrykandpatrick.vico.core.axis.AxisPosition
import com.patrykandpatrick.vico.core.axis.formatter.AxisValueFormatter
import com.patrykandpatrick.vico.core.chart.line.LineChart
import com.patrykandpatrick.vico.core.component.shape.Shapes
import com.patrykandpatrick.vico.core.entry.FloatEntry
import com.patrykandpatrick.vico.core.entry.entryModelOf
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import org.chronicheal.app.ui.theme.HeaderBlue
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import kotlin.math.ceil

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun AnalyticsScreen(
    onBackClick: () -> Unit,
    viewModel: AnalyticsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val timeRange by viewModel.timeRange.collectAsState()
    val startDate by viewModel.startDate.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    val palette = remember {
        listOf(
            Color(0xFF0072B2), // Blue
            Color(0xFFD55E00), // Vermillion
            Color(0xFF009E73), // Bluish Green
            Color(0xFFCC79A7), // Reddish Purple
            Color(0xFFF0E442), // Yellow
            Color(0xFF56B4E9), // Sky Blue
            Color(0xFFE69F00), // Orange
        )
    }

    val createPdfLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/pdf")
    ) { uri ->
        uri?.let {
            scope.launch {
                context.contentResolver.openOutputStream(it)?.use { outputStream ->
                    viewModel.exportPdf(outputStream)
                }
            }
        }
    }

    LaunchedEffect(Unit) {
        viewModel.message.collectLatest {
            snackbarHostState.showSnackbar(it)
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("Analytics") },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(
                        onClick = { createPdfLauncher.launch("chronicheal_report_${startDate}.pdf") },
                        enabled = !isLoading
                    ) {
                        Icon(Icons.Default.PictureAsPdf, contentDescription = "Export PDF")
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
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            TimeRangeSelector(
                timeRange = timeRange,
                startDate = startDate,
                onRangeChange = viewModel::setTimeRange,
                onMovePeriod = viewModel::movePeriod
            )

            if (isLoading) {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            }

            HorizontalDivider()

            Text(text = "Pain Evolution", style = MaterialTheme.typography.titleLarge)
            if (uiState.painData.isNotEmpty()) {
                val locations = uiState.painData.keys.toList()
                val dates = uiState.painData.values.first().keys.toList()
                
                // Stack the series manually
                val rawSeries = uiState.painData.values.toList()
                val stackedSeries = remember(uiState.painData) {
                    val result = mutableListOf<List<FloatEntry>>()
                    for (i in rawSeries.indices) {
                        val currentRaw = rawSeries[i].values.toList()
                        val currentStacked = currentRaw.mapIndexed { j, value ->
                            val previousValue = if (i > 0) result[i - 1][j].y else 0f
                            FloatEntry(j.toFloat(), previousValue + value.toFloat())
                        }
                        result.add(currentStacked)
                    }
                    result
                }

                // Plot stacked areas. Reverse so largest area is drawn first
                val model = remember(stackedSeries) {
                    entryModelOf(*stackedSeries.reversed().toTypedArray())
                }
                
                val bottomAxisFormatter = remember(dates) {
                    AxisValueFormatter<AxisPosition.Horizontal.Bottom> { value, _ ->
                        dates.getOrNull(value.toInt())?.format(DateTimeFormatter.ofPattern("MMM dd")) ?: ""
                    }
                }

                val maxTotalPain = remember(stackedSeries) {
                    (stackedSeries.lastOrNull()?.maxOfOrNull { it.y } ?: 10f).coerceAtLeast(1f)
                }

                Chart(
                    chart = lineChart(
                        lines = locations.mapIndexed { index, _ ->
                            val color = palette[index % palette.size]
                            LineChart.LineSpec(
                                lineColor = color.toArgb(),
                                lineBackgroundShader = verticalGradient(
                                    arrayOf(color.copy(alpha = 0.8f), color.copy(alpha = 0.8f))
                                )
                            )
                        }.reversed()
                    ),
                    model = model,
                    startAxis = rememberStartAxis(
                        itemPlacer = AxisItemPlacer.Vertical.default(
                            maxItemCount = (ceil(maxTotalPain).toInt() + 1).coerceAtMost(30)
                        )
                    ),
                    bottomAxis = rememberBottomAxis(
                        valueFormatter = bottomAxisFormatter,
                        labelRotationDegrees = 45f
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(280.dp)
                )

                // Legend
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    locations.forEachIndexed { index, location ->
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(12.dp)
                                    .clip(CircleShape)
                                    .background(palette[index % palette.size])
                            )
                            Spacer(Modifier.width(4.dp))
                            Text(text = location, style = MaterialTheme.typography.labelSmall)
                        }
                    }
                }
            } else {
                Text("No pain data for this period.", style = MaterialTheme.typography.bodyMedium)
            }

            HorizontalDivider()

            Text(text = "Symptoms Impact (Severity Sum)", style = MaterialTheme.typography.titleLarge)
            if (uiState.symptomSeveritySum.isNotEmpty()) {
                val symptomNames = uiState.symptomSeveritySum.keys.toList()
                val symptomEntries = uiState.symptomSeveritySum.entries.mapIndexed { index, entry ->
                    listOf(FloatEntry(index.toFloat(), entry.value.toFloat()))
                }
                val model = entryModelOf(*symptomEntries.toTypedArray())

                val bottomAxisFormatter = remember(symptomNames) {
                    AxisValueFormatter<AxisPosition.Horizontal.Bottom> { value, _ ->
                        symptomNames.getOrNull(value.toInt()) ?: ""
                    }
                }

                val maxFreq = remember(uiState.symptomSeveritySum) {
                    (uiState.symptomSeveritySum.values.maxOfOrNull { it.toFloat() } ?: 5f).coerceAtLeast(1f)
                }

                Chart(
                    chart = columnChart(
                        columns = symptomNames.mapIndexed { index, _ ->
                            lineComponent(
                                color = palette[index % palette.size],
                                thickness = 16.dp,
                                shape = Shapes.roundedCornerShape(allPercent = 40)
                            )
                        }
                    ),
                    model = model,
                    startAxis = rememberStartAxis(
                        itemPlacer = AxisItemPlacer.Vertical.default(
                            maxItemCount = (ceil(maxFreq).toInt() + 1).coerceAtMost(30)
                        )
                    ),
                    bottomAxis = rememberBottomAxis(
                        valueFormatter = bottomAxisFormatter,
                        labelRotationDegrees = 45f
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(240.dp)
                )
            } else {
                Text("No symptoms recorded for this period.", style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}

@Composable
fun TimeRangeSelector(
    timeRange: TimeRange,
    startDate: LocalDate,
    onRangeChange: (TimeRange) -> Unit,
    onMovePeriod: (Int) -> Unit
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth()
        ) {
            IconButton(onClick = { onMovePeriod(-1) }) {
                Icon(Icons.Default.ChevronLeft, contentDescription = "Previous")
            }
            
            val formatter = DateTimeFormatter.ofPattern("MMM d, yyyy")
            Text(
                text = "${startDate.format(formatter)} - ${getEndDate(startDate, timeRange).format(formatter)}",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold
            )

            IconButton(onClick = { onMovePeriod(1) }) {
                Icon(Icons.Default.ChevronRight, contentDescription = "Next")
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            TimeRange.entries.forEach { range ->
                FilterChip(
                    selected = timeRange == range,
                    onClick = { onRangeChange(range) },
                    label = { Text(range.name.lowercase().replaceFirstChar { it.uppercase() }) }
                )
            }
        }
    }
}

private fun getEndDate(start: LocalDate, range: TimeRange): LocalDate {
    return when (range) {
        TimeRange.WEEK -> start.plusDays(6)
        TimeRange.MONTH -> start.plusMonths(1).minusDays(1)
        TimeRange.YEAR -> start.plusYears(1).minusDays(1)
    }
}
