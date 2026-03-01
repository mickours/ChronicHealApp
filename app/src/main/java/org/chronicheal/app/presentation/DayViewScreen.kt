package org.chronicheal.app.presentation

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import kotlinx.coroutines.launch
import org.chronicheal.app.domain.model.HealthEntry
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DayViewScreen(
    dateString: String,
    onBackClick: () -> Unit,
    onAddEntryClick: (LocalDate) -> Unit,
    onEntryClick: (HealthEntry) -> Unit,
    viewModel: TimelineViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val date = remember(dateString) { LocalDate.parse(dateString) }
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    
    var entryToDelete by remember { mutableStateOf<HealthEntry?>(null) }
    
    val dayEntries = remember(uiState.entries, date) {
        uiState.entries.filter { 
            it.timestamp.atZone(ZoneId.systemDefault()).toLocalDate() == date 
        }
    }

    val titleDateFormatter = DateTimeFormatter.ofLocalizedDate(FormatStyle.LONG)

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text(date.format(titleDateFormatter)) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { onAddEntryClick(date) }) {
                Icon(Icons.Default.Add, contentDescription = "Add Entry")
            }
        }
    ) { innerPadding ->
        if (dayEntries.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentAlignment = androidx.compose.ui.Alignment.Center
            ) {
                Text(
                    text = "No entries for this day.",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.outline
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                items(dayEntries) { entry ->
                    EntryItem(
                        entry = entry,
                        onDeleteClick = { entryToDelete = entry },
                        modifier = Modifier.clickable { onEntryClick(entry) }
                    )
                }
            }
        }
    }

    if (entryToDelete != null) {
        AlertDialog(
            onDismissRequest = { entryToDelete = null },
            title = { Text("Delete Entry") },
            text = { Text("Are you sure you want to delete this entry?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        val entry = entryToDelete!!
                        viewModel.deleteEntry(entry)
                        entryToDelete = null
                        scope.launch {
                            val result = snackbarHostState.showSnackbar(
                                message = "Entry deleted",
                                actionLabel = "Undo",
                                duration = SnackbarDuration.Short
                            )
                            if (result == SnackbarResult.ActionPerformed) {
                                viewModel.restoreDeletedEntry()
                            }
                        }
                    }
                ) {
                    Text("Delete", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { entryToDelete = null }) {
                    Text("Cancel")
                }
            }
        )
    }
}
