package org.chronicheal.app.presentation

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import org.chronicheal.app.ui.theme.HeaderBlue
import java.io.BufferedReader
import java.io.InputStreamReader

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onBackClick: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel(),
    securityViewModel: SecurityViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val isBiometricLockEnabled by securityViewModel.isBiometricLockEnabled.collectAsState()
    val isBiometricAvailable by securityViewModel.isBiometricAvailable.collectAsState()
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }

    // Launcher for saving JSON file
    val createJsonLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        uri?.let {
            viewModel.exportData { json ->
                context.contentResolver.openOutputStream(it)?.use { outputStream ->
                    outputStream.write(json.toByteArray())
                }
            }
        }
    }

    // Launcher for picking JSON file
    val openDocumentLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        uri?.let {
            context.contentResolver.openInputStream(it)?.use { inputStream ->
                val reader = BufferedReader(InputStreamReader(inputStream))
                val content = reader.readText()
                viewModel.importData(content)
            }
        }
    }

    LaunchedEffect(uiState.message) {
        uiState.message?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearMessage()
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("Settings") },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
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
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(text = "Privacy & Security", style = MaterialTheme.typography.titleMedium)
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Biometric Lock",
                        style = MaterialTheme.typography.bodyLarge,
                        color = if (isBiometricAvailable) Color.Unspecified else MaterialTheme.colorScheme.outline
                    )
                    Text(
                        text = if (isBiometricAvailable) 
                            "Require biometric authentication to open the app" 
                            else "Biometric authentication is not available on this device",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.outline
                    )
                }
                Switch(
                    checked = isBiometricLockEnabled,
                    onCheckedChange = { securityViewModel.setBiometricLockEnabled(it) },
                    enabled = isBiometricAvailable
                )
            }

            HorizontalDivider()

            Text(text = "Onboarding", style = MaterialTheme.typography.titleMedium)
            
            Button(
                onClick = { viewModel.resetWelcomeWizard() },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondaryContainer, contentColor = MaterialTheme.colorScheme.onSecondaryContainer)
            ) {
                Text("Reset Welcome Wizard")
            }

            HorizontalDivider()

            Text(text = "Data Management", style = MaterialTheme.typography.titleMedium)
            
            Button(
                onClick = { createJsonLauncher.launch("chronicheal_backup.json") },
                modifier = Modifier.fillMaxWidth(),
                enabled = !uiState.isLoading
            ) {
                Text("Export Data to JSON")
            }

            Button(
                onClick = { openDocumentLauncher.launch(arrayOf("application/json")) },
                modifier = Modifier.fillMaxWidth(),
                enabled = !uiState.isLoading
            ) {
                Text("Import Data from JSON")
            }
            
            if (uiState.isLoading) {
                CircularProgressIndicator(modifier = Modifier.padding(top = 16.dp))
            }
            
            Spacer(modifier = Modifier.weight(1f))
            
            Text(
                text = "ChronicHeal v1.0 - Open Source & Privacy First",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.outline
            )
        }
    }
}
