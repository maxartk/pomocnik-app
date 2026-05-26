package cz.kovmak.pomocnik.ui.screens

import android.content.Intent
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.rememberScrollState
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
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
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
fun HistoryScreen(viewModel: HistoryViewModel = viewModel(), onRepeatEntry: (WorkEntry) -> Unit = {}) {
    val context = LocalContext.current
    val entries by viewModel.entries.collectAsState()
    val allEntries by viewModel.allEntries.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val entryCount by viewModel.entryCount.collectAsState()
    var showDeleteDialog by remember { mutableStateOf(false) }
    var entryToDelete by remember { mutableStateOf<WorkEntry?>(null) }
    var selectedEntry by remember { mutableStateOf<WorkEntry?>(null) }
    var entryToRepeat by remember { mutableStateOf<WorkEntry?>(null) }

    // Navigate to Home tab with pre-filled data
    val onRepeat: (WorkEntry) -> Unit = { entry ->
        onRepeatEntry(entry)
        entryToRepeat = entry
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBg)
    ) {
        // Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 20.dp, top = 20.dp, end = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "Історія",
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                color = NeonOrange,
                letterSpacing = 8.sp
            )
            IconButton(
                onClick = { shareAllEntries(context, allEntries) },
                enabled = allEntries.isNotEmpty(),
                modifier = Modifier.size(40.dp)
            ) {
                Icon(Icons.Filled.Share, contentDescription = "Експорт", tint = if (allEntries.isNotEmpty()) NeonBlue else TextGray)
            }
        }

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
                    EntryCard(
                        entry = entry,
                        onClick = { selectedEntry = entry },
                        onDelete = {
                            entryToDelete = entry
                            showDeleteDialog = true
                        },
                        onShare = { shareEntry(context, entry) },
                        onRepeat = { onRepeat(entry) }
                    )
                }
            }
        }
    }

    // Show toast when entry is marked for repeat
    entryToRepeat?.let { entry ->
        LaunchedEffect(entry) {
            android.widget.Toast.makeText(
                context,
                "Zkopírováno: ${entry.descriptionUa.take(40)}",
                android.widget.Toast.LENGTH_SHORT
            ).show()
            entryToRepeat = null
        }
    }

    // Detail dialog
    selectedEntry?.let { entry ->
        EntryDetailDialog(
            entry = entry,
            onDismiss = { selectedEntry = null },
            onShare = { shareEntry(context, entry) },
            onRepeat = { onRepeat(entry); selectedEntry = null }
        )
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
fun EntryDetailDialog(entry: WorkEntry, onDismiss: () -> Unit, onShare: () -> Unit, onRepeat: () -> Unit = {}) {
    val context = LocalContext.current
    val dateFormat = SimpleDateFormat("dd.MM.yyyy  HH:mm", Locale.getDefault())
    val workTypeLabel = if (entry.workType == "E") "⚡ Elektrická" else "🔧 Mechanická"
    val workTypeColor = if (entry.workType == "E") NeonOrange else NeonBlue

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp)
                .fillMaxHeight(0.85f),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = DarkBg)
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Header with close button
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 20.dp, end = 8.dp, top = 16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = workTypeLabel,
                        color = workTypeColor,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Filled.Close, null, tint = TextGray, modifier = Modifier.size(24.dp))
                    }
                }

                // Scrollable content
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 20.dp)
                ) {
                    // Order ID
                    if (entry.orderId.isNotEmpty()) {
                        DetailRow("📋 Замовлення", "#${entry.orderId}")
                    }

                    // Date & time
                    DetailRow("📅 Дата", dateFormat.format(Date(entry.timestamp)))

                    // Time & hours
                    if (entry.startTime.isNotEmpty() || entry.endTime.isNotEmpty()) {
                        DetailRow("🕐 Час", "${entry.startTime}–${entry.endTime} (${entry.hours}h)")
                    }

                    // Materials
                    if (entry.materials.isNotEmpty()) {
                        DetailRow("🔧 Матеріали", entry.materials)
                    }

                    // UA description
                    if (entry.descriptionUa.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(16.dp))
                        SectionHeader("🇺🇦 Українською")
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = DarkCard)
                        ) {
                            Text(
                                text = entry.descriptionUa,
                                color = TextWhite,
                                fontSize = 15.sp,
                                lineHeight = 22.sp,
                                modifier = Modifier.padding(14.dp)
                            )
                        }
                    }

                    // CZ translation
                    if (entry.descriptionCz.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(16.dp))
                        SectionHeader("🇨🇿 Překlad")
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = DarkCard)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(10.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = entry.descriptionCz,
                                    color = NeonBlue,
                                    fontSize = 15.sp,
                                    lineHeight = 22.sp,
                                    modifier = Modifier.weight(1f)
                                )
                                IconButton(
                                    onClick = {
                                        val clipboard = context.getSystemService(android.content.Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
                                        clipboard.setPrimaryClip(android.content.ClipData.newPlainText("translation", entry.descriptionCz))
                                    },
                                    modifier = Modifier.size(36.dp)
                                ) {
                                    Icon(Icons.Outlined.ContentCopy, "Копіювати", tint = TextGray, modifier = Modifier.size(18.dp))
                                }
                            }
                        }
                    }

                    // Technical report
                    if (entry.technicalReport.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(16.dp))
                        SectionHeader("📋 Zápis pro SAP")
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = DarkCard)
                        ) {
                            Text(
                                text = entry.technicalReport,
                                color = TextWhite,
                                fontSize = 14.sp,
                                lineHeight = 20.sp,
                                modifier = Modifier.padding(14.dp)
                            )
                        }
                    }

                    // SAP fields
                    if (entry.sapObjectPart.isNotEmpty() || entry.sapDamageDesc.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(16.dp))
                        Divider(color = TextGray.copy(alpha = 0.2f))
                        Spacer(modifier = Modifier.height(8.dp))
                        SectionHeader("🔧 SAP PM")
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = DarkCard)
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                if (entry.sapObjectPart.isNotEmpty()) {
                                    Text("Část obj.: MGLC ${entry.sapObjectPart}", color = TextWhite, fontSize = 13.sp)
                                }
                                if (entry.sapDamageDesc.isNotEmpty()) {
                                    Text("Popis škody: MCZ001 ${entry.sapDamageDesc}", color = TextWhite, fontSize = 13.sp)
                                }
                                if (entry.sapDamageText.isNotEmpty()) {
                                    Text("Text: ${entry.sapDamageText}", color = TextWhite, fontSize = 13.sp)
                                }
                                if (entry.sapCause.isNotEmpty()) {
                                    Text("Příčina: MGLO ${entry.sapCause}", color = TextWhite, fontSize = 13.sp)
                                }
                                if (entry.sapCauseText.isNotEmpty()) {
                                    Text("Text příčiny: ${entry.sapCauseText}", color = TextWhite, fontSize = 13.sp)
                                }
                                if (entry.sapImpact.isNotEmpty()) {
                                    Text("Dopad: ${entry.sapImpact}", color = TextWhite, fontSize = 13.sp)
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                }

                // Bottom action row
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = onRepeat,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = NeonOrange)
                    ) {
                        Icon(Icons.Filled.Replay, null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Повторити", fontSize = 13.sp)
                    }
                    OutlinedButton(
                        onClick = onShare,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = NeonBlue)
                    ) {
                        Icon(Icons.Outlined.Share, null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Поділитися", fontSize = 13.sp)
                    }
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = TextGray)
                    ) {
                        Text("Закрити", fontSize = 13.sp)
                    }
                }
            }
        }
    }
}

@Composable
private fun SectionHeader(title: String) {
    Text(
        text = title,
        color = TextGray,
        fontSize = 12.sp,
        fontWeight = FontWeight.SemiBold,
        letterSpacing = 1.sp,
        modifier = Modifier.padding(bottom = 6.dp)
    )
}

@Composable
private fun DetailRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 3.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, color = TextGray, fontSize = 13.sp)
        Text(value, color = TextWhite, fontSize = 13.sp, fontWeight = FontWeight.Medium)
    }
}

@Composable
fun EntryCard(entry: WorkEntry, onClick: () -> Unit, onDelete: () -> Unit, onShare: () -> Unit, onRepeat: () -> Unit = {}) {
    val dateFormat = SimpleDateFormat("dd.MM.yyyy  HH:mm", Locale.getDefault())
    val workTypeColor = if (entry.workType == "E") NeonOrange else NeonBlue
    val workTypeIcon = if (entry.workType == "E") "⚡" else "🔧"

    Card(
        onClick = onClick,
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
                    IconButton(onClick = onRepeat, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Filled.Replay, "Повторити", tint = NeonOrange, modifier = Modifier.size(18.dp))
                    }
                    IconButton(onClick = onShare, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Outlined.Share, null, tint = NeonBlue, modifier = Modifier.size(18.dp))
                    }
                    IconButton(onClick = onDelete, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Outlined.Delete, null, tint = TextGray, modifier = Modifier.size(18.dp))
                    }
                }
            }
        }
    }
}

private fun shareAllEntries(context: android.content.Context, entries: List<WorkEntry>) {
    val header = "timestamp,orderId,workType,startTime,endTime,hours,ua,cz,materials,report"
    fun esc(value: String): String = "\"" + value.replace("\"", "\"\"") + "\""
    val csv = buildString {
        appendLine(header)
        entries.sortedByDescending { it.timestamp }.forEach { entry ->
            appendLine(
                listOf(
                    entry.timestamp.toString(),
                    esc(entry.orderId),
                    esc(entry.workType),
                    esc(entry.startTime),
                    esc(entry.endTime),
                    entry.hours.toString(),
                    esc(entry.descriptionUa),
                    esc(entry.descriptionCz),
                    esc(entry.materials),
                    esc(entry.technicalReport)
                ).joinToString(",")
            )
        }
    }
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "text/csv"
        putExtra(Intent.EXTRA_SUBJECT, "Pomocnik backup (${entries.size} záznamů)")
        putExtra(Intent.EXTRA_TEXT, csv)
    }
    context.startActivity(Intent.createChooser(intent, "Exportovat historii"))
}


private fun shareEntry(context: android.content.Context, entry: WorkEntry) {
    val reportSection = if (entry.technicalReport.isNotEmpty()) {
        "\n\n📋 Zápis pro SAP:\n${entry.technicalReport}"
    } else ""
    val sapSection = if (entry.sapObjectPart.isNotEmpty() || entry.sapDamageDesc.isNotEmpty()) {
        val sb = StringBuilder("\n\n🔧 SAP PM:")
        if (entry.sapObjectPart.isNotEmpty()) sb.append("\nČást obj.: MGLC ${entry.sapObjectPart}")
        if (entry.sapDamageDesc.isNotEmpty()) sb.append("\nPopis škody: MCZ001 ${entry.sapDamageDesc}")
        if (entry.sapDamageText.isNotEmpty()) sb.append("\nText: ${entry.sapDamageText}")
        if (entry.sapCause.isNotEmpty()) sb.append("\nPříčina: MGLO ${entry.sapCause}")
        if (entry.sapCauseText.isNotEmpty()) sb.append("\nText příčiny: ${entry.sapCauseText}")
        if (entry.sapImpact.isNotEmpty()) sb.append("\nDopad: ${entry.sapImpact}")
        sb.toString()
    } else ""
    val text = """
⚡ ${if (entry.workType == "E") "Elektrická" else "Mechanická"} | #${entry.orderId}
🕐 ${entry.startTime}-${entry.endTime} (${entry.hours}h)

${entry.descriptionCz}

🇺🇦 ${entry.descriptionUa}$reportSection$sapSection
""".trimIndent()

    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_TEXT, text)
    }
    context.startActivity(Intent.createChooser(intent, "Поділитися"))
}