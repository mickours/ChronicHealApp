package org.chronicheal.app.presentation

import android.content.Intent
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.NoFood
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.InputChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.net.toUri
import androidx.hilt.navigation.compose.hiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.chronicheal.app.R
import org.chronicheal.app.domain.model.Allergen
import org.chronicheal.app.domain.model.Fodmap
import org.chronicheal.app.ui.theme.HeaderBlue

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
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
    
    var isProfileExpanded by remember { mutableStateOf(false) }
    var isAllergensExpanded by remember { mutableStateOf(false) }
    var isFodmapsExpanded by remember { mutableStateOf(false) }

    var ageInput by remember { mutableStateOf(if (uiState.userAge > 0) uiState.userAge.toString() else "") }
    var weightInput by remember { mutableStateOf(if (uiState.userWeight > 0) uiState.userWeight.toString() else "") }
    var heightInput by remember { mutableStateOf(if (uiState.userHeight > 0) uiState.userHeight.toString() else "") }

    LaunchedEffect(uiState.userAge) {
        if (uiState.userAge != (ageInput.toIntOrNull() ?: 0)) {
            ageInput = if (uiState.userAge > 0) uiState.userAge.toString() else ""
        }
    }
    LaunchedEffect(uiState.userWeight) {
        if (uiState.userWeight != (weightInput.replace(",", ".").toFloatOrNull() ?: 0f)) {
            weightInput = if (uiState.userWeight > 0) uiState.userWeight.toString() else ""
        }
    }
    LaunchedEffect(uiState.userHeight) {
        if (uiState.userHeight != (heightInput.toIntOrNull() ?: 0)) {
            heightInput = if (uiState.userHeight > 0) uiState.userHeight.toString() else ""
        }
    }

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

    val scope = rememberCoroutineScope()

    // Launcher for picking JSON file
    val openDocumentLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let {
            scope.launch {
                // Show loader immediately
                viewModel.setLoading(true)

                val content = withContext(Dispatchers.IO) {
                    val cr = context.applicationContext.contentResolver
                    var lastError: Exception? = null

                    // SAF and cloud providers (like Nextcloud) can be flaky or slow to download.
                    // We try to open the stream with a few retries and a delay.
                    for (attempt in 1..3) {
                        try {
                            cr.openInputStream(it)?.use { stream ->
                                return@withContext stream.bufferedReader().use { it.readText() }
                            }
                        } catch (e: Exception) {
                            lastError = e
                            val errorMsg = e.message ?: ""

                            // Specific check for Nextcloud "Error downloading file"
                            if (it.authority == "org.nextcloud.documents" &&
                                (errorMsg.contains("download", ignoreCase = true) ||
                                        errorMsg.contains("FileNotFound", ignoreCase = true))
                            ) {
                                Log.e("SettingsScreen", "Nextcloud download error detected", e)
                                // We can't fix Nextcloud internally, so we'll have to warn the user
                            }

                            Log.w(
                                "SettingsScreen",
                                "Attempt $attempt: Failed to open stream for $it",
                                e
                            )

                            if (attempt < 3) {
                                delay(2000L * attempt) // Increasing delay
                            }
                        }
                    }

                    Log.e("SettingsScreen", "All read attempts failed for $it", lastError)
                    null
                }

                if (content != null) {
                    viewModel.importData(content)
                } else {
                    viewModel.setLoading(false)
                    val isNextcloud = it.authority == "org.nextcloud.documents"
                    val message = if (isNextcloud) {
                        // Custom hint for Nextcloud users
                        context.getString(R.string.import_read_error) + " (Nextcloud: try 'Available offline')"
                    } else {
                        context.getString(R.string.import_read_error)
                    }
                    snackbarHostState.showSnackbar(message)
                }
            }
        }
    }

    // Launcher for picking backup directory
    val pickDirectoryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocumentTree()
    ) { uri ->
        uri?.let {
            // Take persistable permission for background work
            context.contentResolver.takePersistableUriPermission(
                it,
                Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
            )
            viewModel.setBackupDirectory(it.toString())
        }
    }

    LaunchedEffect(uiState.message) {
        uiState.message?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearMessage()
        }
    }

    Scaffold(
        modifier = Modifier.systemBarsPadding(),
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.settings_title)) },
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
            // PROFILE SECTION
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { isProfileExpanded = !isProfileExpanded },
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Person, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            Spacer(Modifier.width(12.dp))
                            Text(text = stringResource(R.string.profile_title), style = MaterialTheme.typography.titleMedium)
                        }
                        Icon(
                            imageVector = if (isProfileExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                            contentDescription = null
                        )
                    }
                    
                    AnimatedVisibility(visible = isProfileExpanded) {
                        Column(modifier = Modifier.padding(top = 16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                                OutlinedTextField(
                                    value = ageInput,
                                    onValueChange = {
                                        ageInput = it
                                        viewModel.setUserAge(it.toIntOrNull() ?: 0)
                                    },
                                    label = { Text(stringResource(R.string.age_label)) },
                                    modifier = Modifier.weight(1f),
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    singleLine = true
                                )
                                
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(stringResource(R.string.sex_label), style = MaterialTheme.typography.labelSmall)
                                    Row(modifier = Modifier.fillMaxWidth()) {
                                        listOf(stringResource(R.string.sex_male), stringResource(R.string.sex_female), stringResource(R.string.sex_other)).forEach { option ->
                                            val isSelected = uiState.userSex == option
                                            Surface(
                                                onClick = { viewModel.setUserSex(option) },
                                                shape = CircleShape,
                                                color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                                                modifier = Modifier
                                                    .size(40.dp)
                                                    .padding(4.dp)
                                            ) {
                                                Box(contentAlignment = Alignment.Center) {
                                                    Text(
                                                        text = option.take(1),
                                                        color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                                                        fontSize = 12.sp,
                                                        fontWeight = FontWeight.Bold
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                            
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                                OutlinedTextField(
                                    value = weightInput,
                                    onValueChange = {
                                        weightInput = it
                                        viewModel.setUserWeight(
                                            it.replace(",", ".").toFloatOrNull() ?: 0f
                                        )
                                    },
                                    label = { Text(stringResource(R.string.weight_label)) },
                                    modifier = Modifier.weight(1f),
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                    singleLine = true
                                )
                                OutlinedTextField(
                                    value = heightInput,
                                    onValueChange = {
                                        heightInput = it
                                        viewModel.setUserHeight(it.toIntOrNull() ?: 0)
                                    },
                                    label = { Text(stringResource(R.string.height_label)) },
                                    modifier = Modifier.weight(1f),
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    singleLine = true
                                )
                            }
                            
                            Text(
                                text = stringResource(R.string.chronic_diseases_title),
                                style = MaterialTheme.typography.labelMedium,
                                modifier = Modifier.padding(top = 8.dp)
                            )
                            
                            var newDisease by remember { mutableStateOf("") }
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                OutlinedTextField(
                                    value = newDisease,
                                    onValueChange = { newDisease = it },
                                    placeholder = { Text(stringResource(R.string.add_disease_hint)) },
                                    modifier = Modifier.weight(1f),
                                    singleLine = true
                                )
                                IconButton(onClick = { 
                                    if (newDisease.isNotBlank()) {
                                        viewModel.setChronicDiseases(uiState.chronicDiseases + newDisease)
                                        newDisease = ""
                                    }
                                }) {
                                    Icon(Icons.Default.Add, contentDescription = stringResource(R.string.add))
                                }
                            }
                            
                            FlowRow(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                uiState.chronicDiseases.forEach { disease ->
                                    InputChip(
                                        selected = true,
                                        onClick = { viewModel.setChronicDiseases(uiState.chronicDiseases - disease) },
                                        label = { Text(disease) },
                                        trailingIcon = { Icon(Icons.Default.Close, contentDescription = null, modifier = Modifier.size(16.dp)) }
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // ALLERGENS SECTION
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { isAllergensExpanded = !isAllergensExpanded },
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.NoFood, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            Spacer(Modifier.width(12.dp))
                            Text(text = stringResource(R.string.allergens), style = MaterialTheme.typography.titleMedium)
                        }
                        Icon(
                            imageVector = if (isAllergensExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                            contentDescription = null
                        )
                    }
                    
                    AnimatedVisibility(visible = isAllergensExpanded) {
                        Column(modifier = Modifier.padding(top = 16.dp)) {
                            FlowRow(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Allergen.entries.forEach { allergen ->
                                    val isDeactivated = allergen.id in uiState.deactivatedAllergens
                                    FilterChip(
                                        selected = !isDeactivated,
                                        onClick = {
                                            val current = uiState.deactivatedAllergens
                                            val next = if (isDeactivated) current - allergen.id else current + allergen.id
                                            viewModel.setDeactivatedAllergens(next)
                                        },
                                        label = { Text(stringResource(allergen.displayRes)) }
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // FODMAPs SECTION
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(
                        alpha = 0.5f
                    )
                )
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { isFodmapsExpanded = !isFodmapsExpanded },
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            @Suppress("DEPRECATION")
                            Text(text = "🍎", fontSize = 20.sp)
                            Spacer(Modifier.width(12.dp))
                            Text(
                                text = stringResource(R.string.fodmaps),
                                style = MaterialTheme.typography.titleMedium
                            )
                        }
                        Icon(
                            imageVector = if (isFodmapsExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                            contentDescription = null
                        )
                    }

                    AnimatedVisibility(visible = isFodmapsExpanded) {
                        Column(modifier = Modifier.padding(top = 16.dp)) {
                            FlowRow(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Fodmap.entries.forEach { fodmap ->
                                    val isDeactivated = fodmap.id in uiState.deactivatedFodmaps
                                    FilterChip(
                                        selected = !isDeactivated,
                                        onClick = {
                                            val current = uiState.deactivatedFodmaps
                                            val next =
                                                if (isDeactivated) current - fodmap.id else current + fodmap.id
                                            viewModel.setDeactivatedFodmaps(next)
                                        },
                                        label = { Text(stringResource(fodmap.displayRes)) }
                                    )
                                }
                            }
                        }
                    }
                }
            }

            HorizontalDivider()

            Text(text = stringResource(R.string.settings_security), style = MaterialTheme.typography.titleMedium)
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(R.string.biometric_lock),
                        style = MaterialTheme.typography.bodyLarge,
                        color = if (isBiometricAvailable) Color.Unspecified else MaterialTheme.colorScheme.outline
                    )
                    Text(
                        text = if (isBiometricAvailable) 
                            stringResource(R.string.biometric_lock_desc)
                            else stringResource(R.string.biometric_not_available),
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

            Text(
                text = stringResource(R.string.notifications_title),
                style = MaterialTheme.typography.titleMedium
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(R.string.missing_entry_notif),
                        style = MaterialTheme.typography.bodyLarge
                    )
                    Text(
                        text = stringResource(R.string.missing_entry_notif_desc),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.outline
                    )
                }
                Switch(
                    checked = uiState.isMissingEntryNotificationEnabled,
                    onCheckedChange = { viewModel.setMissingEntryNotificationEnabled(it) }
                )
            }

            HorizontalDivider()

            Text(text = stringResource(R.string.onboarding), style = MaterialTheme.typography.titleMedium)
            
            Button(
                onClick = { viewModel.resetWelcomeWizard() },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondaryContainer, contentColor = MaterialTheme.colorScheme.onSecondaryContainer)
            ) {
                @Suppress("DEPRECATION")
                Text(stringResource(R.string.reset_wizard))
            }

            HorizontalDivider()

            Text(text = stringResource(R.string.data_management), style = MaterialTheme.typography.titleMedium)
            
            // AUTOMATED BACKUP TOGGLE
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(R.string.auto_backup),
                        style = MaterialTheme.typography.bodyLarge
                    )
                    Text(
                        text = stringResource(R.string.auto_backup_desc),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.outline
                    )
                }
                Switch(
                    checked = uiState.isAutoBackupEnabled,
                    onCheckedChange = { viewModel.toggleAutoBackup(it) }
                )
            }

            // BACKUP DIRECTORY SELECTION
            if (uiState.isAutoBackupEnabled) {
                Column(modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 16.dp)) {
                    Text(
                        text = stringResource(R.string.backup_directory),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = if (uiState.backupDirectoryUri == null) 
                            stringResource(R.string.backup_directory_default)
                        else uiState.backupDirectoryUri!!.toUri().path
                            ?: uiState.backupDirectoryUri!!,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        TextButton(onClick = { pickDirectoryLauncher.launch(null) }) {
                            Text(stringResource(R.string.backup_directory_change))
                        }
                        if (uiState.backupDirectoryUri != null) {
                            TextButton(onClick = { viewModel.setBackupDirectory(null) }) {
                                Text(stringResource(R.string.backup_directory_reset))
                            }
                        }
                    }
                }
            }

            Button(
                onClick = { createJsonLauncher.launch("chronicheal_backup.json") },
                modifier = Modifier.fillMaxWidth(),
                enabled = !uiState.isLoading
            ) {
                Text(stringResource(R.string.export_json))
            }

            Button(
                onClick = { openDocumentLauncher.launch("*/*") },
                modifier = Modifier.fillMaxWidth(),
                enabled = !uiState.isLoading
            ) {
                Text(stringResource(R.string.import_json))
            }
            
            if (uiState.isLoading) {
                CircularProgressIndicator(modifier = Modifier.padding(top = 16.dp))
            }
            
            Spacer(modifier = Modifier.height(24.dp))

            @Suppress("DEPRECATION")
            Text(
                text = stringResource(R.string.app_version_info),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.outline,
                modifier = Modifier.align(Alignment.CenterHorizontally)
            )
        }
    }
}
