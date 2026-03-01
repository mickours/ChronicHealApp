package org.chronicheal.app.presentation

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.patrykandpatrick.vico.compose.axis.horizontal.rememberBottomAxis
import com.patrykandpatrick.vico.compose.axis.vertical.rememberStartAxis
import com.patrykandpatrick.vico.compose.chart.Chart
import com.patrykandpatrick.vico.compose.chart.column.columnChart
import com.patrykandpatrick.vico.compose.component.lineComponent
import com.patrykandpatrick.vico.core.axis.AxisPosition
import com.patrykandpatrick.vico.core.axis.formatter.AxisValueFormatter
import com.patrykandpatrick.vico.core.chart.column.ColumnChart
import com.patrykandpatrick.vico.core.component.shape.Shapes
import com.patrykandpatrick.vico.core.entry.FloatEntry
import com.patrykandpatrick.vico.core.entry.entryModelOf
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import org.chronicheal.app.ui.theme.HeaderBlue
import java.time.LocalDate
import java.time.format.DateTimeFormatter

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
                
                val painEntries = uiState.painData.values.map { dayMap ->
                    dayMap.entries.mapIndexed { index, entry ->
                        FloatEntry(index.toFloat(), entry.value.toFloat())
                    }
                }
                val model = entryModelOf(*painEntries.toTypedArray())
                
                val bottomAxisFormatter = AxisValueFormatter<AxisPosition.Horizontal.Bottom> { value, _ ->
                    dates.getOrNull(value.toInt())?.format(DateTimeFormatter.ofPattern("MM-dd")) ?: ""
                }

                Chart(
                    chart = columnChart(
                        mergeMode = ColumnChart.MergeMode.Stack,
                        columns = locations.mapIndexed { index, _ ->
                            lineComponent(
                                color = palette[index % palette.size],
                                thickness = 8.dp,
                                shape = Shapes.roundedCornerShape(allPercent = 20)
                            )
                        }
                    ),
                    model = model,
                    startAxis = rememberStartAxis(),
                    bottomAxis = rememberBottomAxis(valueFormatter = bottomAxisFormatter),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(240.dp)
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

            Text(text = "Top Symptoms", style = MaterialTheme.typography.titleLarge)
            if (uiState.symptomFrequency.isNotEmpty()) {
                val symptomNames = uiState.symptomFrequency.keys.toList()
                val symptomEntries = uiState.symptomFrequency.entries.mapIndexed { index, entry ->
                    FloatEntry(index.toFloat(), entry.value.toFloat())
                }
                val model = entryModelOf(symptomEntries)

                val bottomAxisFormatter = AxisValueFormatter<AxisPosition.Horizontal.Bottom> { value, _ ->
                    symptomNames.getOrNull(value.toInt()) ?: ""
                }

                Chart(
                    chart = columnChart(
                        columns = listOf(
                            lineComponent(
                                color = Color(0xFFD55E00), // Color-blind friendly Vermillion
                                thickness = 16.dp,
                                shape = Shapes.roundedCornerShape(allPercent = 40)
                            )
                        )
                    ),
                    model = model,
                    startAxis = rememberStartAxis(),
                    bottomAxis = rememberBottomAxis(valueFormatter = bottomAxisFormatter),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp)
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
