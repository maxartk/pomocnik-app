package cz.kovmak.pomocnik.ui.screens

import android.Manifest
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.rememberMultiplePermissionsState
import cz.kovmak.pomocnik.viewmodel.ShiftScheduleViewModel
import cz.kovmak.pomocnik.viewmodel.ShiftType
import java.io.File
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.TextStyle
import java.util.Locale

private val NeonOrange = Color(0xFFFFB000)
private val NeonBlue = Color(0xFF00D4FF)
private val DarkBg = Color(0xFF080B12)
private val DarkCard = Color(0xFF1B2433)
private val DarkSurface = Color(0xFF111827)
private val TextWhite = Color(0xFFE8E8E8)
private val TextGray = Color(0xFF94A3B8)

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun ShiftScheduleScreen(viewModel: ShiftScheduleViewModel = viewModel()) {
    val context = LocalContext.current
    val state by viewModel.state.collectAsState()
    val profile by viewModel.userProfile.collectAsState()
    val apiKey = profile?.openRouterApiKey ?: ""

    val permissionsState = rememberMultiplePermissionsState(listOf(Manifest.permission.CAMERA))
    var cameraImageUri by remember { mutableStateOf<Uri?>(null) }
    var launchCameraAfterPermission by remember { mutableStateOf(false) }

    val galleryLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let { viewModel.setPhotoUri(it.toString()) }
    }

    val cameraLauncher = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { success ->
        if (success && cameraImageUri != null) viewModel.setPhotoUri(cameraImageUri.toString())
    }

    fun startCamera() {
        val photoFile = File(context.cacheDir, "schedule/schedule_${System.currentTimeMillis()}.jpg").also {
            it.parentFile?.mkdirs()
        }
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", photoFile)
        cameraImageUri = uri
        cameraLauncher.launch(uri)
    }

    LaunchedEffect(permissionsState.allPermissionsGranted) {
        if (launchCameraAfterPermission && permissionsState.allPermissionsGranted) {
            launchCameraAfterPermission = false
            startCamera()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBg)
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        HeaderCard()
        Spacer(Modifier.height(16.dp))
        ImportPhotoCard(
            photoUri = state.selectedPhotoUri,
            isImporting = state.isImporting,
            apiKeyAvailable = apiKey.isNotBlank(),
            onGallery = { galleryLauncher.launch("image/*") },
            onCamera = {
                if (!permissionsState.allPermissionsGranted) {
                    launchCameraAfterPermission = true
                    permissionsState.launchMultiplePermissionRequest()
                } else startCamera()
            },
            onClearPhoto = { viewModel.setPhotoUri(null) },
            onImport = { viewModel.importFromPhoto(apiKey) }
        )
        Spacer(Modifier.height(16.dp))
        CalendarCard(
            month = state.selectedMonth,
            shifts = state.shifts,
            onPrevious = viewModel::previousMonth,
            onNext = viewModel::nextMonth,
            onToday = viewModel::currentMonth,
            onCycleShift = viewModel::cycleShift,
            onClearMonth = viewModel::clearMonth
        )
        Spacer(Modifier.height(12.dp))
        LegendCard()
        AnimatedVisibility(state.statusMessage != null) {
            MessageCard(text = state.statusMessage.orEmpty(), color = NeonBlue, icon = "✅")
        }
        AnimatedVisibility(state.error != null) {
            MessageCard(text = state.error.orEmpty(), color = Color(0xFFFF6666), icon = "⚠️")
        }
        Spacer(Modifier.height(40.dp))
    }
}

@Composable
private fun HeaderCard() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = DarkCard),
        border = BorderStroke(1.dp, Color(0x3300D4FF))
    ) {
        Row(Modifier.fillMaxWidth().padding(18.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier.size(52.dp).clip(RoundedCornerShape(16.dp)).background(NeonBlue.copy(alpha = 0.16f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Filled.CalendarMonth, contentDescription = null, tint = NeonBlue, modifier = Modifier.size(32.dp))
            }
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text("ГРАФІК ЗМІН", color = TextWhite, fontSize = 24.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = 2.sp)
                Text("фото → календар → твої зміни", color = TextGray, fontSize = 12.sp)
            }
        }
    }
}

@Composable
private fun ImportPhotoCard(
    photoUri: String?,
    isImporting: Boolean,
    apiKeyAvailable: Boolean,
    onGallery: () -> Unit,
    onCamera: () -> Unit,
    onClearPhoto: () -> Unit,
    onImport: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = DarkCard)
    ) {
        Column(Modifier.padding(16.dp)) {
            Text("📷 Імпорт з фото", color = NeonOrange, fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
            Spacer(Modifier.height(8.dp))
            Text(
                "Сфотографуй Excel-графік або вибери скрін. Найкраще — обрізати ближче до твого рядка Kovalevskyi.",
                color = TextGray,
                fontSize = 12.sp,
                lineHeight = 17.sp
            )
            Spacer(Modifier.height(12.dp))
            if (photoUri != null) {
                Box(Modifier.fillMaxWidth()) {
                    AsyncImage(
                        model = photoUri,
                        contentDescription = "Фото графіка",
                        modifier = Modifier.fillMaxWidth().height(180.dp).clip(RoundedCornerShape(14.dp)),
                        contentScale = ContentScale.Crop
                    )
                    IconButton(
                        onClick = onClearPhoto,
                        modifier = Modifier.align(Alignment.TopEnd).padding(6.dp).size(34.dp).clip(CircleShape).background(Color.Black.copy(alpha = 0.65f))
                    ) { Icon(Icons.Filled.Close, "Видалити", tint = Color.White) }
                }
                Spacer(Modifier.height(10.dp))
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(
                    onClick = onCamera,
                    modifier = Modifier.weight(1f).height(46.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = NeonBlue)
                ) {
                    Icon(Icons.Filled.CameraAlt, null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Камера", fontSize = 13.sp)
                }
                OutlinedButton(
                    onClick = onGallery,
                    modifier = Modifier.weight(1f).height(46.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = NeonOrange)
                ) {
                    Icon(Icons.Outlined.Image, null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Галерея", fontSize = 13.sp)
                }
            }
            Spacer(Modifier.height(10.dp))
            Button(
                onClick = onImport,
                enabled = !isImporting && photoUri != null && apiKeyAvailable,
                modifier = Modifier.fillMaxWidth().height(50.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = NeonOrange)
            ) {
                if (isImporting) {
                    CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Color.White, strokeWidth = 2.dp)
                    Spacer(Modifier.width(10.dp))
                    Text("Розпізнаю...", color = Color.White, fontWeight = FontWeight.Bold)
                } else {
                    Icon(Icons.Filled.AutoAwesome, null, tint = Color.White)
                    Spacer(Modifier.width(8.dp))
                    Text("РОЗПІЗНАТИ ГРАФІК", color = Color.White, fontWeight = FontWeight.Bold)
                }
            }
            if (!apiKeyAvailable) {
                Spacer(Modifier.height(8.dp))
                Text("Для AI-розпізнавання потрібен OpenRouter API ключ у налаштуваннях.", color = Color(0xFFFFCC66), fontSize = 11.sp)
            }
        }
    }
}

@Composable
private fun CalendarCard(
    month: YearMonth,
    shifts: Map<String, ShiftType>,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onToday: () -> Unit,
    onCycleShift: (LocalDate) -> Unit,
    onClearMonth: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = DarkCard)
    ) {
        Column(Modifier.padding(14.dp)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onPrevious) { Icon(Icons.Filled.ChevronLeft, null, tint = TextWhite) }
                Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        "${month.month.getDisplayName(TextStyle.FULL, Locale("uk")).replaceFirstChar { it.uppercase() }} ${month.year}",
                        color = TextWhite,
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp
                    )
                    Text("Натисни день, щоб вручну перемкнути зміну", color = TextGray, fontSize = 11.sp)
                }
                IconButton(onClick = onNext) { Icon(Icons.Filled.ChevronRight, null, tint = TextWhite) }
            }
            Spacer(Modifier.height(8.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = onToday, modifier = Modifier.weight(1f), colors = ButtonDefaults.outlinedButtonColors(contentColor = NeonBlue)) {
                    Text("Цей місяць", fontSize = 12.sp)
                }
                OutlinedButton(onClick = onClearMonth, modifier = Modifier.weight(1f), colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFFF6666))) {
                    Text("Очистити", fontSize = 12.sp)
                }
            }
            Spacer(Modifier.height(12.dp))
            WeekHeader()
            Spacer(Modifier.height(6.dp))
            val days = calendarCells(month)
            LazyVerticalGrid(
                columns = GridCells.Fixed(7),
                userScrollEnabled = false,
                modifier = Modifier.height(300.dp),
                horizontalArrangement = Arrangement.spacedBy(5.dp),
                verticalArrangement = Arrangement.spacedBy(5.dp)
            ) {
                items(days) { date ->
                    if (date == null) EmptyDayCell() else DayCell(date, shifts[date.toString()], onCycleShift)
                }
            }
        }
    }
}

@Composable
private fun WeekHeader() {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(5.dp)) {
        listOf("Пн", "Вт", "Ср", "Чт", "Пт", "Сб", "Нд").forEach { day ->
            Text(day, color = TextGray, fontSize = 11.sp, textAlign = TextAlign.Center, modifier = Modifier.weight(1f))
        }
    }
}

@Composable
private fun DayCell(date: LocalDate, shift: ShiftType?, onCycleShift: (LocalDate) -> Unit) {
    val today = date == LocalDate.now()
    val bg = shift?.let { Color(it.colorHex).copy(alpha = 0.22f) } ?: DarkSurface
    val border = when {
        today -> BorderStroke(1.dp, NeonOrange)
        shift != null -> BorderStroke(1.dp, Color(shift.colorHex).copy(alpha = 0.8f))
        else -> BorderStroke(1.dp, Color(0x22334455))
    }
    Card(
        modifier = Modifier.fillMaxWidth().height(40.dp).clickable { onCycleShift(date) },
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(containerColor = bg),
        border = border
    ) {
        Column(Modifier.fillMaxSize().padding(3.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
            Text(date.dayOfMonth.toString(), color = TextWhite, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            Text(shift?.code ?: "", color = shift?.let { Color(it.colorHex) } ?: TextGray, fontSize = 9.sp, fontWeight = FontWeight.Bold, maxLines = 1)
        }
    }
}

@Composable
private fun EmptyDayCell() {
    Box(Modifier.fillMaxWidth().height(40.dp))
}

@Composable
private fun LegendCard() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = DarkSurface)
    ) {
        Column(Modifier.padding(14.dp)) {
            Text("Типи змін", color = TextWhite, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
            Spacer(Modifier.height(8.dp))
            ShiftType.entries.forEach { type ->
                Row(Modifier.fillMaxWidth().padding(vertical = 3.dp), verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(14.dp).clip(CircleShape).background(Color(type.colorHex)))
                    Spacer(Modifier.width(8.dp))
                    Text(type.code, color = Color(type.colorHex), fontWeight = FontWeight.Bold, fontSize = 13.sp, modifier = Modifier.width(42.dp))
                    Text("${type.labelUa} • ${type.time}", color = TextGray, fontSize = 12.sp)
                }
            }
        }
    }
}

@Composable
private fun MessageCard(text: String, color: Color, icon: String) {
    Spacer(Modifier.height(10.dp))
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = color.copy(alpha = 0.12f))
    ) {
        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(icon, fontSize = 18.sp)
            Spacer(Modifier.width(8.dp))
            Text(text, color = color, fontSize = 13.sp, modifier = Modifier.weight(1f))
        }
    }
}

private fun calendarCells(month: YearMonth): List<LocalDate?> {
    val first = month.atDay(1)
    val leading = when (first.dayOfWeek) {
        DayOfWeek.MONDAY -> 0
        DayOfWeek.TUESDAY -> 1
        DayOfWeek.WEDNESDAY -> 2
        DayOfWeek.THURSDAY -> 3
        DayOfWeek.FRIDAY -> 4
        DayOfWeek.SATURDAY -> 5
        DayOfWeek.SUNDAY -> 6
    }
    val cells = mutableListOf<LocalDate?>()
    repeat(leading) { cells.add(null) }
    for (day in 1..month.lengthOfMonth()) cells.add(month.atDay(day))
    while (cells.size % 7 != 0) cells.add(null)
    return cells
}
