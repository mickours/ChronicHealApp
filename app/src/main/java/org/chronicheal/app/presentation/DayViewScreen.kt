package org.chronicheal.app.presentation

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DayViewScreen(
    dateString: String,
    onBackClick: () -> Unit,
    viewModel: TimelineViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val date = remember(dateString) { LocalDate.parse(dateString) }
    
    val dayEntries = remember(uiState.entries, date) {
        uiState.entries.filter { 
            it.timestamp.atZone(ZoneId.systemDefault()).toLocalDate() == date 
        }
    }

    val titleDateFormatter = DateTimeFormatter.ofLocalizedDate(FormatStyle.LONG)

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(date.format(titleDateFormatter)) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
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
                        onDeleteClick = { viewModel.deleteEntry(entry) }
                    )
                }
            }
        }
    }
}
