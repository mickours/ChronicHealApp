package org.chronicheal.app.presentation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import kotlinx.coroutines.launch
import org.chronicheal.app.domain.model.HealthEntry
import org.chronicheal.app.ui.theme.HeaderBlue
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DayViewScreen(
    navController: NavController,
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
    
    val dayEntries = remember(uiState.entries, date) {
        uiState.entries.filter { 
            it.timestamp.atZone(ZoneId.systemDefault()).toLocalDate() == date 
        }
    }

    // Observe messages from SavedStateHandle (e.g. "Edition canceled")
    val savedStateHandle = navController.currentBackStackEntry?.savedStateHandle
    val navMessageFlow = remember(savedStateHandle) {
        savedStateHandle?.getStateFlow<String?>("message", null)
    }
    val navMessage by (navMessageFlow?.collectAsState() ?: remember { mutableStateOf<String?>(null) })

    LaunchedEffect(navMessage) {
        navMessage?.let { msg ->
            scope.launch {
                snackbarHostState.showSnackbar(msg)
                savedStateHandle?.remove<String>("message")
            }
        }
    }

    LaunchedEffect(uiState.message) {
        uiState.message?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearMessage()
        }
    }

    val titleDateFormatter = remember { DateTimeFormatter.ofLocalizedDate(FormatStyle.LONG) }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text(date.format(titleDateFormatter)) },
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
                items(dayEntries, key = { it.id }) { entry ->
                    SwipeableEntryItem(
                        entry = entry,
                        onDelete = {
                            viewModel.deleteEntry(entry)
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
                        },
                        onMarkFinished = { viewModel.markEntryAsFinished(entry) },
                        onClick = { onEntryClick(entry) }
                    )
                }
            }
        }
    }
}
