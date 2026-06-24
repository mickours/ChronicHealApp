package org.chronicheal.app.presentation

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateListOf
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import org.chronicheal.app.R
import org.chronicheal.app.domain.model.EntryType
import org.chronicheal.app.domain.model.HealthEntry
import org.chronicheal.app.presentation.components.BodySilhouette
import org.chronicheal.app.presentation.components.EntryDateTimePicker
import org.chronicheal.app.presentation.components.VerticalIntensityGauge
import org.chronicheal.app.presentation.components.formatId
import org.chronicheal.app.ui.theme.HeaderBlue
import org.chronicheal.app.ui.theme.PrimaryDark
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BodyScanScreen(
    dateString: String? = null,
    onBackClick: () -> Unit,
    onSaveSuccess: () -> Unit,
    viewModel: AddEntryViewModel = hiltViewModel(),
) {
    val context = LocalContext.current
    var logDate by rememberSaveable { mutableStateOf(if (dateString != null) LocalDate.parse(dateString) else LocalDate.now()) }
    var startTime by rememberSaveable { mutableStateOf(LocalTime.now()) }
    var isSaving by remember { mutableStateOf(value = false) }

    val painEntries = remember { mutableStateListOf<HealthEntry>() }
    var selectedRegion by remember { mutableStateOf<String?>(null) }
    var currentIntensity by remember { mutableFloatStateOf(0f) }
    var isListExpanded by rememberSaveable { mutableStateOf(true) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.body_scan)) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back))
                    }
                },
                actions = {
                    Button(
                        onClick = {
                            isSaving = true
                            val timestamp =
                                logDate.atTime(startTime).atZone(ZoneId.systemDefault()).toInstant()
                            painEntries.forEach {
                                viewModel.saveEntry(it.copy(timestamp = timestamp), null)
                            }
                            onSaveSuccess()
                        },
                        modifier = Modifier.padding(end = 8.dp),
                        enabled = !isSaving && painEntries.isNotEmpty(),
                    ) {
                        if (isSaving) {
                            CircularProgressIndicator(
                                modifier = Modifier.padding(end = 8.dp),
                                color = MaterialTheme.colorScheme.onPrimary
                            )
                        }
                        Text(stringResource(R.string.save))
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
        ) {
            Box(
                modifier = Modifier.padding(16.dp)
            ) {
                EntryDateTimePicker(
                    date = logDate,
                    onDateChange = { logDate = it },
                    startTime = startTime
                ) { startTime = it }
            }

            Text(
                text = stringResource(R.string.body_scan_instruction),
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
            )

            Spacer(modifier = Modifier.height(8.dp))

            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                BodySilhouette(
                    modifier = Modifier.fillMaxSize(),
                    painEntries = painEntries,
                    onRegionHold = { regionId, intensity ->
                        selectedRegion = regionId
                        currentIntensity = intensity
                    },
                    onRelease = {
                        selectedRegion?.let { regionId ->
                            val formattedLocation = formatId(context, regionId)
                            val existingIndex = painEntries.indexOfFirst {
                                it.location.equals(
                                    regionId,
                                    ignoreCase = true
                                )
                            }

                            if (currentIntensity > 0) {
                                if (existingIndex >= 0) {
                                    painEntries[existingIndex] = painEntries[existingIndex].copy(
                                        intensity = currentIntensity.roundToInt()
                                    )
                                } else {
                                    painEntries.add(
                                        HealthEntry(
                                            type = EntryType.PAIN,
                                            location = regionId,
                                            note = formattedLocation,
                                            intensity = currentIntensity.roundToInt()
                                        )
                                    )
                                }
                            } else if (existingIndex >= 0) {
                                painEntries.removeAt(existingIndex)
                            }
                        }
                        selectedRegion = null
                        currentIntensity = 0f
                    }
                )

                if (selectedRegion != null) {
                    VerticalIntensityGauge(
                        intensity = currentIntensity.roundToInt(),
                        maxVal = 10,
                        color = PrimaryDark,
                        label = stringResource(R.string.intensity_short_label),
                        modifier = Modifier
                            .align(Alignment.CenterEnd)
                            .padding(end = 16.dp, top = 32.dp, bottom = 32.dp)
                    )
                }

                if (painEntries.isNotEmpty()) {
                    Card(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .fillMaxWidth()
                            .padding(16.dp)
                            .height(if (isListExpanded) 220.dp else 48.dp),
                        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f)
                        )
                    ) {
                        Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { isListExpanded = !isListExpanded },
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = stringResource(R.string.recorded_pain_points),
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold
                                )
                                Icon(
                                    if (isListExpanded) Icons.Default.KeyboardArrowDown else Icons.Default.KeyboardArrowUp,
                                    contentDescription = null
                                )
                            }

                            if (isListExpanded) {
                                Spacer(modifier = Modifier.height(8.dp))
                                LazyColumn(modifier = Modifier.weight(1f)) {
                                    itemsIndexed(painEntries) { index, entry ->
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(vertical = 4.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            TextField(
                                                value = entry.note,
                                                onValueChange = {
                                                    painEntries[index] = entry.copy(note = it)
                                                },
                                                modifier = Modifier.weight(1f),
                                                textStyle = MaterialTheme.typography.bodyMedium,
                                                singleLine = true,
                                                colors = TextFieldDefaults.colors(
                                                    focusedContainerColor = Color.Transparent,
                                                    unfocusedContainerColor = Color.Transparent,
                                                    disabledContainerColor = Color.Transparent,
                                                    focusedIndicatorColor = MaterialTheme.colorScheme.primary,
                                                    unfocusedIndicatorColor = Color.Transparent
                                                )
                                            )
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Surface(
                                                color = Color.Red.copy(alpha = 0.1f),
                                                shape = MaterialTheme.shapes.small
                                            ) {
                                                Text(
                                                    text = (entry.intensity ?: 0).toString(),
                                                    modifier = Modifier.padding(
                                                        horizontal = 8.dp,
                                                        vertical = 4.dp
                                                    ),
                                                    fontWeight = FontWeight.Bold,
                                                    color = Color.Red
                                                )
                                            }
                                            IconButton(
                                                onClick = { painEntries.removeAt(index) }
                                            ) {
                                                Icon(
                                                    Icons.Default.Delete,
                                                    contentDescription = stringResource(R.string.delete),
                                                    tint = MaterialTheme.colorScheme.error,
                                                    modifier = Modifier.size(20.dp)
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
