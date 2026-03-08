package org.chronicheal.app.presentation

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import org.chronicheal.app.R
import org.chronicheal.app.domain.model.EntryType
import org.chronicheal.app.domain.model.HealthEntry
import org.chronicheal.app.presentation.util.VoiceCommandParser
import org.chronicheal.app.presentation.util.VoiceToTextManager
import org.chronicheal.app.ui.theme.HeaderBlue
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VoiceLoggingScreen(
    onBackClick: () -> Unit,
    onSaveSuccess: () -> Unit,
    viewModel: TimelineViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val voiceManager = remember { VoiceToTextManager(context) }
    val voiceState by voiceManager.state.collectAsState()
    val uiState by viewModel.uiState.collectAsState()
    val parser = remember { VoiceCommandParser(context) }
    
    var parsedEntries by remember { mutableStateOf<List<HealthEntry>>(emptyList()) }
    var lastSpokenText by remember { mutableStateOf("") }
    var showRationale by rememberSaveable { mutableStateOf(false) }

    val startListening = {
        val permission = Manifest.permission.RECORD_AUDIO
        if (ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED) {
            voiceManager.startListening(Locale.getDefault().toLanguageTag())
        } else {
            // Handled by permissionLauncher
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            startListening()
        }
    }

    val requestPermissionWithRationale = {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) {
            startListening()
        } else if (!uiState.hasShownVoiceRationale) {
            showRationale = true
        } else {
            permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
        }
    }

    if (showRationale) {
        AlertDialog(
            onDismissRequest = { showRationale = false },
            title = { Text(stringResource(R.string.voice_permission_rationale_title)) },
            text = { Text(stringResource(R.string.voice_permission_rationale_msg)) },
            confirmButton = {
                TextButton(onClick = {
                    showRationale = false
                    viewModel.setHasShownVoiceRationale(true)
                    permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                }) {
                    Text(stringResource(R.string.ok))
                }
            },
            dismissButton = {
                TextButton(onClick = { showRationale = false }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }

    DisposableEffect(Unit) {
        onDispose {
            voiceManager.destroy()
        }
    }

    LaunchedEffect(voiceState.spokenText) {
        if (voiceState.spokenText.isNotEmpty()) {
            val text = voiceState.spokenText.lowercase()
            lastSpokenText = voiceState.spokenText
            
            // Check for voice keywords to save/cancel
            if (text.contains("save") || text.contains("confirm") || text.contains("enregistrer") || text.contains("confirmer")) {
                if (parsedEntries.isNotEmpty()) {
                    parsedEntries.forEach { viewModel.addEntry(it) }
                    viewModel.showMessage(context.getString(R.string.voice_saved_message))
                    onSaveSuccess()
                }
            } else if (text.contains("cancel") || text.contains("back") || text.contains("annuler") || text.contains("retour")) {
                onBackClick()
            } else {
                // Auto-parse the entries
                parsedEntries = parser.parse(voiceState.spokenText)
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.voice_logging_title)) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back))
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = HeaderBlue)
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(24.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Top
        ) {
            Text(
                text = if (voiceState.isSpeaking) stringResource(R.string.voice_listening) else stringResource(R.string.voice_tap_to_speak),
                style = MaterialTheme.typography.headlineSmall,
                color = if (voiceState.isSpeaking) MaterialTheme.colorScheme.primary else Color.Gray,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(16.dp))

            // LOCALIZED EXAMPLE SECTION
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.2f))
            ) {
                Text(
                    text = stringResource(R.string.voice_example_hint),
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(16.dp),
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }

            Spacer(modifier = Modifier.height(32.dp))

            VoiceMicButton(
                isListening = voiceState.isSpeaking,
                onClick = {
                    if (voiceState.isSpeaking) {
                        voiceManager.stopListening()
                    } else {
                        // Clear old result when starting a new recording
                        lastSpokenText = ""
                        parsedEntries = emptyList()
                        requestPermissionWithRationale()
                    }
                }
            )

            Spacer(modifier = Modifier.height(32.dp))

            if (voiceState.error != null) {
                ErrorDisplay(error = voiceState.error!!)
                Spacer(modifier = Modifier.height(16.dp))
                OutlinedButton(onClick = { 
                    lastSpokenText = ""
                    parsedEntries = emptyList()
                    requestPermissionWithRationale()
                }) {
                    Icon(Icons.Default.Refresh, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text(stringResource(R.string.retry))
                }
            } else if (parsedEntries.isNotEmpty()) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "\"$lastSpokenText\"",
                        style = MaterialTheme.typography.bodyLarge,
                        fontStyle = androidx.compose.ui.text.font.FontStyle.Italic,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )
                    
                    Spacer(modifier = Modifier.height(24.dp))
                    
                    parsedEntries.forEachIndexed { index, entry ->
                        ParsedEntryPreview(
                            entry = entry,
                            onTypeChange = { newType -> 
                                val newList = parsedEntries.toMutableList()
                                newList[index] = entry.copy(type = newType)
                                parsedEntries = newList
                            }
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                    }
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    Text(
                        text = stringResource(R.string.voice_save_hint),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.secondary,
                        textAlign = TextAlign.Center
                    )
                    
                    Spacer(modifier = Modifier.height(16.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterHorizontally)
                    ) {
                        OutlinedButton(
                            onClick = { 
                                lastSpokenText = ""
                                parsedEntries = emptyList()
                                requestPermissionWithRationale()
                            },
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.Refresh, contentDescription = null)
                            Spacer(Modifier.width(8.dp))
                            Text(stringResource(R.string.retry))
                        }
                        
                        Button(
                            onClick = { 
                                parsedEntries.forEach { viewModel.addEntry(it) }
                                onSaveSuccess()
                            },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(stringResource(R.string.voice_save_now))
                        }
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(48.dp))
        }
    }
}

@Composable
fun ErrorDisplay(error: String) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.2f))
            .padding(16.dp)
    ) {
        Icon(
            imageVector = Icons.Default.Warning,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.error,
            modifier = Modifier.size(32.dp)
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = error,
            color = MaterialTheme.colorScheme.error,
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = stringResource(R.string.voice_error_quiet_hint),
            style = MaterialTheme.typography.labelSmall,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
fun VoiceMicButton(isListening: Boolean, onClick: () -> Unit) {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val scale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = if (isListening) 1.2f else 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scale"
    )

    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .size(100.dp)
            .scale(scale)
            .clip(CircleShape)
            .background(if (isListening) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant)
            .clickable { onClick() }
    ) {
        Icon(
            imageVector = Icons.Default.Mic,
            contentDescription = "Microphone",
            modifier = Modifier.size(50.dp),
            tint = if (isListening) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ParsedEntryPreview(
    entry: HealthEntry,
    onTypeChange: (EntryType) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.3f))
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = stringResource(R.string.voice_detected_log), 
            style = MaterialTheme.typography.labelLarge, 
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSecondaryContainer
        )
        
        Spacer(Modifier.height(16.dp))

        // Type Selection Dropdown - Explicitly visible and interactive
        ExposedDropdownMenuBox(
            expanded = expanded,
            onExpandedChange = { expanded = !expanded },
            modifier = Modifier.fillMaxWidth()
        ) {
            OutlinedTextField(
                value = "${entry.type.emoji} ${stringResource(entry.type.displayRes)}",
                onValueChange = {},
                readOnly = true,
                label = { Text(stringResource(R.string.change_type)) },
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors(
                    focusedContainerColor = Color.White,
                    unfocusedContainerColor = Color.White
                ),
                modifier = Modifier
                    .menuAnchor()
                    .fillMaxWidth()
            )

            ExposedDropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false }
            ) {
                EntryType.entries.filter { it != EntryType.VOICE_LOGGING }.forEach { type ->
                    DropdownMenuItem(
                        text = { Text("${type.emoji} ${stringResource(type.displayRes)}") },
                        onClick = {
                            onTypeChange(type)
                            expanded = false
                        }
                    )
                }
            }
        }

        Spacer(Modifier.height(16.dp))
        
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.Start,
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            entry.intensity?.let { Text("• " + stringResource(R.string.intensity_label, it), style = MaterialTheme.typography.bodyMedium) }
            entry.location?.let { Text("• " + stringResource(R.string.location_label_format, it), style = MaterialTheme.typography.bodyMedium) }
            entry.name?.let { Text("• " + stringResource(R.string.name_label_format, it), style = MaterialTheme.typography.bodyMedium) }
            entry.durationMinutes?.let { Text("• " + stringResource(R.string.duration_mins_label, it), style = MaterialTheme.typography.bodyMedium) }
            entry.unit?.let { Text("• " + stringResource(R.string.dosage_label_format, it), style = MaterialTheme.typography.bodyMedium) }
        }
    }
}
