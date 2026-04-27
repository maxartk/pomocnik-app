package cz.kovmak.pomocnik.ui.screens

import android.content.Intent
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import cz.kovmak.pomocnik.data.database.WorkEntry
import cz.kovmak.pomocnik.viewmodel.HistoryViewModel
import java.text.SimpleDateFormat
import java.util.*

private val NeonOrange = Color(0xFFFF6B35)
private val NeonBlue = Color(0xFF00B4D8)
private val DarkBg = Color(0xFF0A0E21)
private val DarkCard = Color(0xFF1A1F35)
private val DarkSurface = Color(0xFF16213E)
private val TextWhite = Color(0xFFE8E8E8)
private val TextGray = Color(0xFF8892B0)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreen(viewModel: HistoryViewModel = viewModel()) {
    val context = LocalContext.current
    val entries by viewModel.entries.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val entryCount by viewModel.entryCount.collectAsState()
    var showDeleteDialog by remember { mutableStateOf(false) }
    var entryToDelete by remember { mutableStateOf<WorkEntry?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBg)
    ) {
        // Header
        Text(
            text = "Історія",
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            color = NeonOrange,
            letterSpacing = 8.sp,
            modifier = Modifier.padding(start = 20.dp, top = 20.dp)
        )

        // Search bar
        OutlinedTextField(
            value = searchQuery,
            onValueChange = viewModel::updateSearchQuery,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 12.dp),
            placeholder = { Text("Пошук...", color = TextGray.copy(alpha = 0.4f)) },
            leadingIcon = { Icon(Icons.Filled.Search, null, tint = TextGray) },
            trailingIcon = {
                if (searchQuery.isNotEmpty())
                    IconButton(onClick = { viewModel.updateSearchQuery("") }) {
                        Icon(Icons.Filled.Close, null, tint = TextGray)
                    }
            },
            singleLine = true,
            shape = RoundedCornerShape(14.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = NeonOrange,
                unfocusedBorderColor = TextGray.copy(alpha = 0.15f),
                focusedTextColor = TextWhite,
                unfocusedTextColor = TextWhite,
                cursorColor = NeonOrange,
                focusedContainerColor = DarkCard,
                unfocusedContainerColor = DarkCard
            )
        )

        // Count
        Text(
            text = "Знайдено: $entryCount",
            color = TextGray,
            fontSize = 12.sp,
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp)
        )

        if (entries.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Outlined.EventNote, null, modifier = Modifier.size(64.dp), tint = TextGray.copy(alpha = 0.3f))
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("Порожньо", color = TextGray.copy(alpha = 0.5f), fontSize = 16.sp)
                    Text("Створи перший запис!", color = TextGray.copy(alpha = 0.3f), fontSize = 13.sp)
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(entries, key = { it.id }) { entry ->
                    EntryCard(entry, onDelete = {
                        entryToDelete = entry
                        showDeleteDialog = true
                    }, onShare = { shareEntry(context, entry) })
                }
            }
        }
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false; entryToDelete = null },
            containerColor = DarkCard,
            title = { Text("Видалити?", color = TextWhite) },
            text = { Text("Запис буде видалено назавжди", color = TextGray) },
            confirmButton = {
                TextButton(onClick = {
                    entryToDelete?.let { viewModel.deleteEntry(it) }
                    showDeleteDialog = false; entryToDelete = null
                }) { Text("Видалити", color = NeonOrange) }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) { Text("Скасувати", color = TextGray) }
            }
        )
    }
}

@Composable
fun EntryCard(entry: WorkEntry, onDelete: () -> Unit, onShare: () -> Unit) {
    val dateFormat = SimpleDateFormat("dd.MM.yyyy  HH:mm", Locale.getDefault())
    val workTypeColor = if (entry.workType == "E") NeonOrange else NeonBlue
    val workTypeIcon = if (entry.workType == "E") "⚡" else "🔧"

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = DarkCard)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Top row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(workTypeIcon, fontSize = 14.sp)
                    Spacer(modifier = Modifier.width(6.dp))
                    if (entry.orderId.isNotEmpty()) {
                        Text("#${entry.orderId}", color = workTypeColor, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                    }
                }
                Text(
                    dateFormat.format(Date(entry.timestamp)),
                    color = TextGray,
                    fontSize = 11.sp
                )
            }

            // Description
            if (entry.descriptionCz.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    entry.descriptionCz,
                    color = TextWhite,
                    fontSize = 14.sp,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis,
                    lineHeight = 20.sp
                )
            }

            // Technical report
            if (entry.technicalReport.isNotEmpty()) {
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    entry.technicalReport,
                    color = NeonBlue,
                    fontSize = 12.sp,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    lineHeight = 16.sp
                )
            }

            // Meta row
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (entry.hours > 0) {
                        Text("🕐 ${entry.hours}h", color = TextGray, fontSize = 12.sp)
                    }
                    if (entry.photoUri != null) {
                        Spacer(modifier = Modifier.width(12.dp))
                        Icon(Icons.Outlined.Image, null, tint = NeonOrange, modifier = Modifier.size(14.dp))
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    IconButton(onClick = onShare, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Outlined.Share, null, tint = NeonOrange, modifier = Modifier.size(18.dp))
                    }
                    IconButton(onClick = onDelete, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Outlined.Delete, null, tint = TextGray, modifier = Modifier.size(18.dp))
                    }
                }
            }
        }
    }
}

private fun shareEntry(context: android.content.Context, entry: WorkEntry) {
    val reportSection = if (entry.technicalReport.isNotEmpty()) {
        "\n\n📋 Technická zpráva:\n${entry.technicalReport}"
    } else ""
    val text = """
⚡ ${if (entry.workType == "E") "Elektrická" else "Mechanická"} | #${entry.orderId}
🕐 ${entry.startTime}-${entry.endTime} (${entry.hours}h)

${entry.descriptionCz}

🇺🇦 ${entry.descriptionUa}$reportSection
""".trimIndent()

    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_TEXT, text)
    }
    context.startActivity(Intent.createChooser(intent, "Поділитися"))
}
