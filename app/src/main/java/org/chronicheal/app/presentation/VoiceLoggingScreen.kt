package org.chronicheal.app.presentation

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import kotlinx.coroutines.launch
import org.chronicheal.app.R
import org.chronicheal.app.presentation.components.VoiceEnabledTextField
import org.chronicheal.app.presentation.util.VoiceToTextManager
import org.chronicheal.app.ui.theme.HeaderBlue
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VoiceLoggingScreen(
    onBackClick: () -> Unit,
    onSaveSuccess: () -> Unit,
    viewModel: AddEntryViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val voiceToTextManager = remember { VoiceToTextManager(context) }
    val voiceState by voiceToTextManager.state.collectAsState()

    var note by rememberSaveable { mutableStateOf("") }
    var isAnalyzing by remember { mutableStateOf(false) }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            voiceToTextManager.startListening(Locale.getDefault().toLanguageTag())
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            voiceToTextManager.destroy()
        }
    }

    LaunchedEffect(voiceState.spokenText) {
        if (voiceState.spokenText.isNotBlank()) {
            note = voiceState.spokenText
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.voice_logging)) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back))
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
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = stringResource(R.string.voice_logging_desc),
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Direct voice acquisition button
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 16.dp),
                contentAlignment = Alignment.Center
            ) {
                Surface(
                    onClick = {
                        if (voiceState.isSpeaking) {
                            voiceToTextManager.stopListening()
                        } else {
                            permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                        }
                    },
                    shape = CircleShape,
                    color = if (voiceState.isSpeaking) MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.primaryContainer,
                    modifier = Modifier.size(80.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = if (voiceState.isSpeaking) Icons.Default.Stop else Icons.Default.Mic,
                            contentDescription = if (voiceState.isSpeaking) stringResource(R.string.voice_stop) else stringResource(
                                R.string.voice_start
                            ),
                            modifier = Modifier.size(40.dp),
                            tint = if (voiceState.isSpeaking) MaterialTheme.colorScheme.onErrorContainer else MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }

                if (voiceState.isSpeaking) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(90.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }

            AnimatedVisibility(visible = voiceState.isSpeaking) {
                Text(
                    text = stringResource(R.string.voice_listening),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(bottom = 16.dp)
                )
            }

            VoiceEnabledTextField(
                value = note,
                onValueChange = { note = it },
                label = stringResource(R.string.transcript_label),
                minLines = 6,
                modifier = Modifier.weight(1f)
            )

            voiceState.error?.let {
                Text(
                    text = it,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(vertical = 8.dp)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = {
                    scope.launch {
                        isAnalyzing = true
                        val entries = viewModel.processLog(note)
                        if (entries != null) {
                            viewModel.saveEntries(entries) {
                                onSaveSuccess()
                            }
                        } else {
                            viewModel.showMessage(context.getString(R.string.ai_error_parsing))
                        }
                        isAnalyzing = false
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = note.isNotBlank() && !isAnalyzing && !voiceState.isSpeaking
            ) {
                if (isAnalyzing) {
                    CircularProgressIndicator(
                        modifier = Modifier.padding(end = 8.dp),
                        color = MaterialTheme.colorScheme.onPrimary,
                        strokeWidth = 2.dp
                    )
                }
                Text(stringResource(R.string.analyze_and_save))
            }
        }
    }
}
