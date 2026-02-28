package org.chronicheal.app.presentation

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Card
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import org.chronicheal.app.domain.model.HealthEntry
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

@Composable
fun TimelineScreen(
    onAddEntryClick: () -> Unit,
    viewModel: TimelineViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(onClick = onAddEntryClick) {
                Icon(Icons.Default.Add, contentDescription = "Add Entry")
            }
        }
    ) { innerPadding ->
        if (uiState.entries.isEmpty()) {
            Text(
                text = "No entries yet. Tap + to start tracking.",
                modifier = Modifier
                    .padding(innerPadding)
                    .padding(16.dp)
            )
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                items(uiState.entries) { entry ->
                    EntryItem(entry = entry)
                }
            }
        }
    }
}

@Composable
fun EntryItem(entry: HealthEntry) {
    val formatter = DateTimeFormatter
        .ofLocalizedDateTime(FormatStyle.MEDIUM)
        .withZone(ZoneId.systemDefault())

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = entry.type.name.replace("_", " ").lowercase().capitalize(),
                style = MaterialTheme.typography.titleMedium
            )
            Text(
                text = formatter.format(entry.timestamp),
                style = MaterialTheme.typography.bodySmall
            )
            if (entry.note.isNotEmpty()) {
                Text(
                    text = entry.note,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }
            entry.intensity?.let {
                Text(text = "Intensity: $it/10", style = MaterialTheme.typography.bodyMedium)
            }
            entry.name?.let {
                Text(text = "Name: $it", style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}
