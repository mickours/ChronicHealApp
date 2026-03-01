package org.chronicheal.app.presentation

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import org.chronicheal.app.domain.model.HealthEntry
import java.time.LocalTime

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEntryScaffold(
    title: String,
    id: Long?,
    onBackClick: () -> Unit,
    onDeleteClick: () -> Unit,
    onSaveClick: () -> Unit,
    saveButtonEnabled: Boolean,
    saveButtonText: String = if (id == null) "Save" else "Update",
    viewModel: TimelineViewModel,
    content: @Composable (PaddingValues) -> Unit
) {
    var showDeleteConfirmation by remember { mutableStateOf(false) }

    val handleBack = {
        if (id != null) {
            viewModel.showMessage("Edition canceled")
        }
        onBackClick()
    }

    BackHandler(onBack = handleBack)

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(title) },
                navigationIcon = {
                    IconButton(onClick = handleBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    if (id != null) {
                        IconButton(onClick = { showDeleteConfirmation = true }) {
                            Icon(Icons.Default.Delete, contentDescription = "Delete")
                        }
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(modifier = Modifier.fillMaxSize()) {
            Box(modifier = Modifier.weight(1f)) {
                content(innerPadding)
            }
            
            Button(
                onClick = onSaveClick,
                enabled = saveButtonEnabled,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Text(saveButtonText)
            }
        }

        if (showDeleteConfirmation) {
            AlertDialog(
                onDismissRequest = { showDeleteConfirmation = false },
                title = { Text("Delete Entry") },
                text = { Text("Are you sure you want to delete this log?") },
                confirmButton = {
                    TextButton(
                        onClick = {
                            onDeleteClick()
                            showDeleteConfirmation = false
                        }
                    ) {
                        Text("Delete", color = MaterialTheme.colorScheme.error)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showDeleteConfirmation = false }) {
                        Text("Cancel")
                    }
                }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TimePickerDialog(
    onDismissRequest: () -> Unit,
    confirmButton: @Composable () -> Unit,
    dismissButton: @Composable () -> Unit,
    content: @Composable () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismissRequest,
        confirmButton = confirmButton,
        dismissButton = dismissButton,
        text = { content() }
    )
}
