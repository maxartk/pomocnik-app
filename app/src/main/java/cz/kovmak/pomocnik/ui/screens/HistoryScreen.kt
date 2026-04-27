package cz.kovmak.pomocnik.ui.screens

import android.content.Intent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import cz.kovmak.pomocnik.data.database.WorkEntry
import cz.kovmak.pomocnik.viewmodel.HistoryViewModel
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun HistoryScreen(viewModel: HistoryViewModel = viewModel()) {
    val context = LocalContext.current
    val entries by viewModel.entries.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val entryCount by viewModel.entryCount.collectAsState()

    var showDeleteDialog by remember { mutableStateOf(false) }
    var entryToDelete by remember { mutableStateOf<WorkEntry?>(null) }

    Column(modifier = Modifier.fillMaxSize()) {
        // Search bar
        OutlinedTextField(
            value = searchQuery,
            onValueChange = viewModel::updateSearchQuery,
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            placeholder = { Text("Hledat v historii...") },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
            trailingIcon = {
                if (searchQuery.isNotEmpty()) {
                    IconButton(onClick = { viewModel.updateSearchQuery("") }) {
                        Icon(Icons.Default.Clear, contentDescription = "Vymazat")
                    }
                }
            },
            singleLine = true
        )

        // Entry count
        PaddingValues(16.dp).let { padding ->
            Text(
                text = "Celkem záznamů: $entryCount",
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        if (entries.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalArrangement = Arrangement.Center) {
                    Icon(
                        Icons.Default.EventNote,
                        contentDescription = null,
                        modifier = Modifier.size(64.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Žádné záznamy",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(entries, key = { it.id }) { entry ->
                    EntryCard(
                        entry = entry,
                        onDelete = {
                            entryToDelete = entry
                            showDeleteDialog = true
                        },
                        onShare = { shareEntry(context, entry) }
                    )
                }
            }
        }

        // Delete all button
        if (entries.isNotEmpty()) {
            TextButton(
                onClick = { showDeleteDialog = true },
                modifier = Modifier.align(Alignment.CenterHorizontally)
            ) {
                Icon(Icons.Default.DeleteSweep, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Smazat všechny záznamy")
            }
        }
    }

    // Delete confirmation dialog
    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = {
                showDeleteDialog = false
                entryToDelete = null
            },
            title = { Text("Smazat záznam?") },
            text = { Text("Opravdu chcete smazat ${if (entryToDelete != null) "tento záznam" else "všechny záznamy"}?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (entryToDelete != null) {
                            viewModel.deleteEntry(entryToDelete!!)
                        } else {
                            viewModel.deleteAllEntries()
                        }
                        showDeleteDialog = false
                        entryToDelete = null
                    }
                ) {
                    Text("Smazat", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text("Zrušit")
                }
            }
        )
    }
}

@Composable
fun EntryCard(
    entry: WorkEntry,
    onDelete: () -> Unit,
    onShare: () -> Unit
) {
    val dateFormat = SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault())
    val dateStr = dateFormat.format(Date(entry.timestamp))

    val workTypeLabel = when (entry.workType) {
        "E" -> "⚡ Elektrická"
        "M" -> "🔧 Mechanická"
        else -> "⚡🔧 E+M"
    }

    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Header row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (entry.orderId.isNotEmpty()) {
                        Text(
                            text = "#${entry.orderId}",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                    }
                    Text(
                        text = workTypeLabel,
                        style = MaterialTheme.typography.labelSmall
                    )
                }
                Text(
                    text = dateStr,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Czech description
            if (entry.descriptionCz.isNotEmpty()) {
                Text(
                    text = entry.descriptionCz,
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis
                )
            }

            // Time and hours
            if (entry.startTime.isNotEmpty() || entry.endTime.isNotEmpty()) {
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "🕐 ${entry.startTime} - ${entry.endTime}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    if (entry.hours > 0) {
                        Text(
                            text = "(${entry.hours}h)",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }

            // Materials
            if (entry.materials.isNotEmpty()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "📦 ${entry.materials}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Photo indicator
            if (entry.photoUri != null) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "📸 Foto připojeno",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.tertiary
                )
            }

            // Action buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(onClick = onShare) {
                    Icon(Icons.Default.Share, contentDescription = "Sdílet")
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Sdílet")
                }
                TextButton(onClick = onDelete) {
                    Icon(Icons.Default.Delete, contentDescription = "Smazat")
                }
            }
        }
    }
}

private fun shareEntry(context: android.content.Context, entry: WorkEntry) {
    val workType = when (entry.workType) {
        "E" -> "Elektrická"
        "M" -> "Mechanická"
        else -> "E+M"
    }

    val text = """
⚡ HLÁŠENÍ ÚDRŽBY

Zakázka: #${entry.orderId}
Typ: $workType
Čas: ${entry.startTime} - ${entry.endTime} (${entry.hours}h)

🇺🇦 Popis (UA):
${entry.descriptionUa}

🇨🇿 Popis (CZ):
${entry.descriptionCz}

Materiály: ${entry.materials.ifBlank { "N/A" }}
""".trimIndent()

    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_SUBJECT, "Hlášení práce - Zakázka #${entry.orderId}")
        putExtra(Intent.EXTRA_TEXT, text)
    }

    context.startActivity(Intent.createChooser(intent, "Sdílet záznam"))
}
