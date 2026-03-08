package org.chronicheal.app.presentation

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
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
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.CompareArrows
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
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
import com.patrykandpatrick.vico.compose.axis.vertical.rememberEndAxis
import com.patrykandpatrick.vico.compose.axis.vertical.rememberStartAxis
import com.patrykandpatrick.vico.compose.chart.Chart
import com.patrykandpatrick.vico.compose.chart.line.lineChart
import com.patrykandpatrick.vico.compose.component.textComponent
import com.patrykandpatrick.vico.core.axis.AxisItemPlacer
import com.patrykandpatrick.vico.core.axis.AxisPosition
import com.patrykandpatrick.vico.core.axis.formatter.AxisValueFormatter
import com.patrykandpatrick.vico.core.chart.line.LineChart
import com.patrykandpatrick.vico.core.entry.FloatEntry
import com.patrykandpatrick.vico.core.entry.entryModelOf
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import org.chronicheal.app.domain.model.EntryType
import org.chronicheal.app.ui.theme.HeaderBlue
import java.time.LocalDate
import java.util.Locale
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
    val type1 by viewModel.correlationType1.collectAsState()
    val type2 by viewModel.correlationType2.collectAsState()
    val selectedPainLocations by viewModel.selectedPainLocations.collectAsState()
    val selectedSymptoms by viewModel.selectedSymptoms.collectAsState()

    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val isDark = isSystemInDarkTheme()
    val axisLabelColor = if (isDark) Color.White else Color.Black

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
        modifier = Modifier.systemBarsPadding(),
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

            // 1. Pain Evolution
            EvolutionChart(
                title = "Pain Evolution",
                data = uiState.painData,
                selectedItems = selectedPainLocations,
                onToggleItem = viewModel::togglePainLocation,
                palette = palette,
                axisLabelColor = axisLabelColor,
                emptyMessage = "No pain data for this period."
            )

            HorizontalDivider()

            // 2. Symptoms Evolution
            EvolutionChart(
                title = "Symptoms Evolution",
                data = uiState.symptomEvolutionData,
                selectedItems = selectedSymptoms,
                onToggleItem = viewModel::toggleSymptom,
                palette = palette,
                axisLabelColor = axisLabelColor,
                emptyMessage = "No symptom data for this period."
            )

            HorizontalDivider()

            // 3. Correlation Analysis
            Text(text = "Correlation Analysis", style = MaterialTheme.typography.titleLarge)
            
            CorrelationSelectors(
                type1 = type1,
                type2 = type2,
                onTypesChange = viewModel::setCorrelationTypes
            )

            CorrelationChart(
                correlationData = uiState.correlationData,
                type1 = type1,
                type2 = type2,
                color1 = palette[0],
                color2 = palette[1],
                axisLabelColor = axisLabelColor
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun EvolutionChart(
    title: String,
    data: Map<String, Map<String, Int>>,
    selectedItems: Set<String>,
    onToggleItem: (String) -> Unit,
    palette: List<Color>,
    axisLabelColor: Color,
    emptyMessage: String
) {
    Text(text = title, style = MaterialTheme.typography.titleLarge)
    if (data.isNotEmpty()) {
        val allKeys = data.keys.toList()
        val visibleKeys = allKeys.filter { it in selectedItems }
        
        if (visibleKeys.isNotEmpty()) {
            val labels = data.values.first().keys.toList()
            val model = remember(data, visibleKeys) {
                val series = visibleKeys.map { key ->
                    data[key]!!.values.mapIndexed { index, value -> FloatEntry(index.toFloat(), value.toFloat()) }
                }
                entryModelOf(*series.toTypedArray())
            }

            val bottomAxisFormatter = remember(labels) {
                AxisValueFormatter<AxisPosition.Horizontal.Bottom> { value, _ ->
                    labels.getOrNull(value.toInt()) ?: ""
                }
            }

            val maxValue = remember(data, visibleKeys) {
                visibleKeys.maxOf { key -> data[key]!!.values.maxOfOrNull { it } ?: 0 }.toFloat().coerceAtLeast(1f)
            }

            Chart(
                chart = lineChart(
                    lines = visibleKeys.map { key ->
                        val index = allKeys.indexOf(key)
                        val color = palette[index % palette.size]
                        LineChart.LineSpec(lineColor = color.toArgb())
                    }
                ),
                model = model,
                startAxis = rememberStartAxis(
                    label = textComponent(color = axisLabelColor),
                    itemPlacer = AxisItemPlacer.Vertical.default(
                        maxItemCount = (ceil(maxValue.toDouble()).toInt() + 1).coerceAtMost(11)
                    )
                ),
                bottomAxis = rememberBottomAxis(
                    label = textComponent(color = axisLabelColor, lineCount = 1),
                    valueFormatter = bottomAxisFormatter,
                    labelRotationDegrees = 45f,
                    itemPlacer = AxisItemPlacer.Horizontal.default(spacing = 1, offset = 0)
                ),
                modifier = Modifier.fillMaxWidth().height(200.dp)
            )
        } else {
            Box(modifier = Modifier.fillMaxWidth().height(200.dp), contentAlignment = Alignment.Center) {
                Text("Select items from the legend to display data", style = MaterialTheme.typography.bodyMedium)
            }
        }

        // Clickable Legend
        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            allKeys.forEachIndexed { index, key ->
                val isSelected = key in selectedItems
                val color = palette[index % palette.size]
                
                Surface(
                    onClick = { onToggleItem(key) },
                    shape = RoundedCornerShape(16.dp),
                    color = if (isSelected) color.copy(alpha = 0.1f) else Color.Transparent,
                    border = if (isSelected) null else androidx.compose.foundation.BorderStroke(1.dp, Color.Gray.copy(alpha = 0.5f))
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(10.dp)
                                .clip(CircleShape)
                                .background(if (isSelected) color else Color.Gray)
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(
                            text = key,
                            style = MaterialTheme.typography.labelSmall,
                            color = if (isSelected) MaterialTheme.colorScheme.onSurface else Color.Gray,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                        )
                    }
                }
            }
        }
    } else {
        Text(emptyMessage, style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
fun CorrelationChart(
    correlationData: CorrelationData,
    type1: EntryType,
    type2: EntryType,
    color1: Color,
    color2: Color,
    axisLabelColor: Color
) {
    if (correlationData.labels.isEmpty()) return

    val series1 = correlationData.series1.mapIndexed { index, value -> FloatEntry(index.toFloat(), value) }
    val series2 = correlationData.series2.mapIndexed { index, value -> FloatEntry(index.toFloat(), value) }
    
    val model = entryModelOf(series1, series2)

    val bottomAxisFormatter = remember(correlationData.labels) {
        AxisValueFormatter<AxisPosition.Horizontal.Bottom> { value, _ ->
            correlationData.labels.getOrNull(value.toInt()) ?: ""
        }
    }

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Chart(
            chart = lineChart(
                lines = listOf(
                    LineChart.LineSpec(lineColor = color1.toArgb()),
                    LineChart.LineSpec(lineColor = color2.toArgb())
                )
            ),
            model = model,
            startAxis = rememberStartAxis(
                label = textComponent(color = color1),
                title = "${type1.emoji} Scale",
                titleComponent = textComponent(color = color1),
                itemPlacer = AxisItemPlacer.Vertical.default(maxItemCount = 6)
            ),
            endAxis = rememberEndAxis(
                label = textComponent(color = color2),
                title = "${type2.emoji} Scale",
                titleComponent = textComponent(color = color2),
                itemPlacer = AxisItemPlacer.Vertical.default(maxItemCount = 6)
            ),
            bottomAxis = rememberBottomAxis(
                label = textComponent(color = axisLabelColor),
                valueFormatter = bottomAxisFormatter,
                labelRotationDegrees = 45f,
                itemPlacer = AxisItemPlacer.Horizontal.default(spacing = 1, offset = 0)
            ),
            modifier = Modifier
                .fillMaxWidth()
                .height(240.dp)
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            LegendItem(color = color1, label = "${type1.emoji} ${type1.name}")
            Spacer(Modifier.width(16.dp))
            LegendItem(color = color2, label = "${type2.emoji} ${type2.name}")
        }

        // Correlation Score and Insight
        correlationData.pearsonCorrelation?.let { score ->
            val (strength, color) = when {
                score > 0.7 -> "Strong positive correlation" to Color(0xFF2E7D32)
                score > 0.3 -> "Moderate positive correlation" to Color(0xFF4CAF50)
                score > -0.3 -> "No clear correlation" to Color.Gray
                score > -0.7 -> "Moderate negative correlation" to Color(0xFFFF9800)
                else -> "Strong negative correlation" to Color(0xFFD32F2F)
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(color.copy(alpha = 0.1f))
                    .padding(12.dp)
            ) {
                Column {
                    Text(
                        text = "Correlation Insight",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = color
                    )
                    Text(
                        text = "$strength (${String.format(Locale.getDefault(), "%.2f", score)})",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    
                    val insight = when {
                        score > 0.5 -> "When ${type1.name.lowercase()} increases, ${type2.name.lowercase()} also tends to increase."
                        score < -0.5 -> "When ${type1.name.lowercase()} increases, ${type2.name.lowercase()} tends to decrease."
                        else -> "No strong linear relationship found between ${type1.name.lowercase()} and ${type2.name.lowercase()} for this period."
                    }
                    Text(
                        text = insight,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun LegendItem(color: Color, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(12.dp)
                .clip(CircleShape)
                .background(color)
        )
        Spacer(Modifier.width(4.dp))
        @Suppress("DEPRECATION")
        Text(text = label, style = MaterialTheme.typography.labelSmall)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CorrelationSelectors(
    type1: EntryType,
    type2: EntryType,
    onTypesChange: (EntryType, EntryType) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        TypeDropdown(
            selectedType = type1,
            onTypeSelected = { onTypesChange(it, type2) },
            modifier = Modifier.weight(1f),
            label = "Metric 1"
        )
        Icon(Icons.AutoMirrored.Filled.CompareArrows, contentDescription = null)
        TypeDropdown(
            selectedType = type2,
            onTypeSelected = { onTypesChange(type1, it) },
            modifier = Modifier.weight(1f),
            label = "Metric 2"
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TypeDropdown(
    selectedType: EntryType,
    onTypeSelected: (EntryType) -> Unit,
    modifier: Modifier = Modifier,
    label: String
) {
    var expanded by remember { mutableStateOf(false) }

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = it },
        modifier = modifier
    ) {
        OutlinedTextField(
            value = "${selectedType.emoji} ${selectedType.name.lowercase().replaceFirstChar { it.uppercase() }.replace("_", " ")}",
            onValueChange = {},
            readOnly = true,
            label = { Text(label) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable),
            shape = RoundedCornerShape(12.dp),
            textStyle = MaterialTheme.typography.bodySmall
        )
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            EntryType.entries.forEach { type ->
                DropdownMenuItem(
                    text = { Text("${type.emoji} ${type.name.lowercase().replaceFirstChar { it.uppercase() }.replace("_", " ")}") },
                    onClick = {
                        onTypeSelected(type)
                        expanded = false
                    }
                )
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
            
            val formatter = java.time.format.DateTimeFormatter.ofPattern("MMM d, yyyy")
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
