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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
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
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
import org.chronicheal.app.domain.model.HealthEntry
import org.chronicheal.app.presentation.util.VoiceCommandParser
import org.chronicheal.app.presentation.util.VoiceToTextManager
import org.chronicheal.app.ui.theme.HeaderBlue

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
    val parser = remember { VoiceCommandParser() }
    
    var parsedEntry by remember { mutableStateOf<HealthEntry?>(null) }
    var lastSpokenText by remember { mutableStateOf("") }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            voiceManager.startListening()
        }
    }

    LaunchedEffect(voiceState.spokenText) {
        if (voiceState.spokenText.isNotEmpty()) {
            val text = voiceState.spokenText.lowercase()
            lastSpokenText = voiceState.spokenText
            
            // These keywords might need localization too in a real app, 
            // but for now keeping them as they are or adding localized ones.
            if (text.contains("save") || text.contains("confirm") || text.contains("enregistrer") || text.contains("confirmer")) {
                parsedEntry?.let {
                    viewModel.addEntry(it)
                    viewModel.showMessage(context.getString(R.string.voice_saved_message))
                    onSaveSuccess()
                }
            } else if (text.contains("cancel") || text.contains("back") || text.contains("annuler") || text.contains("retour")) {
                onBackClick()
            } else {
                parsedEntry = parser.parse(voiceState.spokenText)
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
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = if (voiceState.isSpeaking) stringResource(R.string.voice_listening) else stringResource(R.string.voice_tap_to_speak),
                style = MaterialTheme.typography.headlineSmall,
                color = if (voiceState.isSpeaking) MaterialTheme.colorScheme.primary else Color.Gray,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(48.dp))

            VoiceMicButton(
                isListening = voiceState.isSpeaking,
                onClick = {
                    if (voiceState.isSpeaking) {
                        voiceManager.stopListening()
                    } else {
                        when (ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO)) {
                            PackageManager.PERMISSION_GRANTED -> voiceManager.startListening()
                            else -> permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                        }
                    }
                }
            )

            Spacer(modifier = Modifier.height(48.dp))

            if (voiceState.error != null) {
                ErrorDisplay(error = voiceState.error!!)
            } else {
                AnimatedVisibility(visible = lastSpokenText.isNotEmpty()) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "\"$lastSpokenText\"",
                            style = MaterialTheme.typography.bodyLarge,
                            fontStyle = androidx.compose.ui.text.font.FontStyle.Italic,
                            textAlign = TextAlign.Center
                        )
                        
                        Spacer(modifier = Modifier.height(24.dp))
                        
                        parsedEntry?.let { entry ->
                            ParsedEntryPreview(entry)
                            
                            Spacer(modifier = Modifier.height(32.dp))
                            
                            Text(
                                text = stringResource(R.string.voice_save_hint),
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.secondary
                            )
                            
                            Row(modifier = Modifier.padding(top = 16.dp), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                                Button(onClick = { 
                                    viewModel.addEntry(entry)
                                    onSaveSuccess()
                                }) {
                                    Text(stringResource(R.string.voice_save_now))
                                }
                            }
                        }
                    }
                }
            }
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
            .size(120.dp)
            .scale(scale)
            .clip(CircleShape)
            .background(if (isListening) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant)
            .clickable { onClick() }
    ) {
        Icon(
            imageVector = Icons.Default.Mic,
            contentDescription = "Microphone",
            modifier = Modifier.size(60.dp),
            tint = if (isListening) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
fun ParsedEntryPreview(entry: HealthEntry) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.4f))
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(text = stringResource(R.string.voice_detected_log), style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(8.dp))
        Text(text = "${entry.type.emoji} ${entry.type.name}", style = MaterialTheme.typography.titleMedium)
        
        entry.intensity?.let { Text(stringResource(R.string.intensity_label, it)) }
        entry.location?.let { Text(stringResource(R.string.location_label_format, it)) }
        entry.name?.let { Text(stringResource(R.string.name_label_format, it)) }
        entry.durationMinutes?.let { Text(stringResource(R.string.duration_mins_label, it)) }
        entry.unit?.let { Text(stringResource(R.string.dosage_label_format, it)) }
    }
}
