package org.chronicheal.app.presentation

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
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
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import org.chronicheal.app.R
import org.chronicheal.app.domain.model.EntryType
import org.chronicheal.app.domain.model.HealthEntry
import org.chronicheal.app.ui.theme.HeaderBlue
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BodyScanScreen(
    dateString: String? = null,
    onBackClick: () -> Unit,
    viewModel: TimelineViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsState()
    val sheetState = rememberModalBottomSheetState()
    var showBottomSheet by remember { mutableStateOf(false) }
    var selectedRegionId by remember { mutableStateOf<String?>(null) }
    var existingEntryId by remember { mutableStateOf<Long?>(null) }

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
                        onRegionHold = { regionId, intensity ->
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
                            
                            painIntensity = intensity
                            selectedRegionId = regionId
                            
                            currentHoldRegionId = regionId
                            currentHoldIntensity = intensity
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
                    Slider(value = painIntensity, onValueChange = { painIntensity = it }, valueRange = 1f..10f, steps = 8)
                    VoiceEnabledTextField(value = painNote, onValueChange = { painNote = it }, label = stringResource(R.string.notes_label))
                    Spacer(Modifier.height(32.dp))
                }
            }
        }
    }
}
