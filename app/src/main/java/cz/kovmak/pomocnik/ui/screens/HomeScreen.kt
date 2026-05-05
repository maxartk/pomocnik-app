package cz.kovmak.pomocnik.ui.screens

import android.Manifest
import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import cz.kovmak.pomocnik.viewmodel.WorkViewModel
import cz.kovmak.pomocnik.ui.components.VoiceInputButton
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.rememberMultiplePermissionsState
import java.io.File
import cz.kovmak.pomocnik.data.model.SapCatalogs

// Industrial color palette
private val NeonOrange = Color(0xFFFFB000)
private val NeonBlue = Color(0xFF00D4FF)
private val DarkBg = Color(0xFF080B12)
private val DarkCard = Color(0xFF1B2433)
private val DarkSurface = Color(0xFF111827)
private val TextWhite = Color(0xFFE8E8E8)
private val TextGray = Color(0xFF94A3B8)

@OptIn(ExperimentalMaterial3Api::class, ExperimentalPermissionsApi::class)
@Composable
fun HomeScreen(viewModel: WorkViewModel = viewModel()) {
    val context = LocalContext.current
    val formState by viewModel.formState.collectAsState()
    val translationResult by viewModel.translationResult.collectAsState()
    val advisorResult by viewModel.advisorResult.collectAsState()
    val technicalReport by viewModel.technicalReport.collectAsState()
    val profile by viewModel.userProfile.collectAsState()
    val apiKey = profile?.openRouterApiKey ?: ""

    val permissionsState = rememberMultiplePermissionsState(
        listOf(Manifest.permission.RECORD_AUDIO, Manifest.permission.CAMERA)
    )

    // Gallery picker
    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? -> uri?.let { viewModel.setPhotoUri(it.toString()) } }

    // Camera launcher
    var cameraImageUri by remember { mutableStateOf<Uri?>(null) }
    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success: Boolean ->
        if (success && cameraImageUri != null) {
            viewModel.setPhotoUri(cameraImageUri.toString())
        }
    }

    // Auto-launch camera after permission is granted
    var launchCameraAfterPermission by remember { mutableStateOf(false) }
    LaunchedEffect(permissionsState.allPermissionsGranted) {
        if (launchCameraAfterPermission && permissionsState.allPermissionsGranted) {
            launchCameraAfterPermission = false
            val photoFile = File(context.cacheDir, "camera/photo_${System.currentTimeMillis()}.jpg").also {
                it.parentFile?.mkdirs()
            }
            val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", photoFile)
            cameraImageUri = uri
            cameraLauncher.launch(uri)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBg)
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // ==================== HEADER ====================
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = DarkCard),
            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0x33FFB000))
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(18.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(52.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(NeonOrange.copy(alpha = 0.16f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Filled.ElectricBolt, contentDescription = null, tint = NeonOrange, modifier = Modifier.size(32.dp))
                }
                Spacer(modifier = Modifier.width(14.dp))
                Column {
                    Text(
                        text = "POMOCNÍK",
                        fontSize = 25.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = TextWhite,
                        letterSpacing = 3.sp
                    )
                    Text(
                        text = "elektro pracovní asistent",
                        fontSize = 12.sp,
                        color = TextGray,
                        letterSpacing = 1.sp
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(22.dp))

        // ==================== MODE SWITCH ====================
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(DarkSurface, RoundedCornerShape(14.dp))
                .padding(4.dp),
            horizontalArrangement = Arrangement.Center
        ) {
            FilterChip(
                selected = formState.mode == "submit",
                onClick = { viewModel.setMode("submit") },
                label = { Text("📝 Переклад", fontSize = 13.sp) },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = NeonOrange,
                    selectedLabelColor = Color.White,
                    containerColor = Color.Transparent
                )
            )
            Spacer(modifier = Modifier.width(8.dp))
            FilterChip(
                selected = formState.mode == "advisor",
                onClick = { viewModel.setMode("advisor") },
                label = { Text("🔧 Порадник", fontSize = 13.sp) },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = NeonBlue,
                    selectedLabelColor = Color.White,
                    containerColor = Color.Transparent
                )
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        // ==================== VOICE BUTTON ====================
        VoiceInputButton(
            onResult = { text ->
                // Only fill the text field — NO auto-translate
                // User presses "ПЕРЕКЛАСТИ" button to translate
                val current = formState.descriptionUa
                viewModel.updateDescriptionUa(if (current.isBlank()) text else "$current $text")
            },
            isProcessing = formState.isTranslating
        )

        Spacer(modifier = Modifier.height(28.dp))

        // ==================== TEXT INPUT ====================
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = DarkCard)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("🇺🇦 Українською", color = NeonOrange, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                    if (formState.descriptionUa.isNotEmpty()) {
                        IconButton(
                            onClick = { viewModel.updateDescriptionUa("") },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(Icons.Filled.Close, "Очистити", tint = Color(0xFFFF4444), modifier = Modifier.size(20.dp))
                        }
                    }
                }

                OutlinedTextField(
                    value = formState.descriptionUa,
                    onValueChange = viewModel::updateDescriptionUa,
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 120.dp),
                    placeholder = { Text("Опиши що зробив...", color = TextGray.copy(alpha = 0.3f)) },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = NeonOrange,
                        unfocusedBorderColor = TextGray.copy(alpha = 0.2f),
                        focusedTextColor = TextWhite,
                        unfocusedTextColor = TextWhite,
                        cursorColor = NeonOrange
                    ),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done)
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Quick templates
                Spacer(modifier = Modifier.height(10.dp))
                Text("Шаблони робіт", color = TextGray, fontSize = 11.sp)
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    val templates = listOf(
                        "Перевірив кабель і затягнув клеми",
                        "Замінено датчик та перевірено сигнал",
                        "Почищено щиток і відновлено контакт",
                        "Перевірено двигун, кабель і захист"
                    )
                    templates.forEach { template ->
                        FilterChip(
                            selected = false,
                            onClick = { viewModel.updateDescriptionUa(template) },
                            label = { Text(template, fontSize = 11.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                containerColor = DarkSurface,
                                selectedContainerColor = NeonOrange,
                                selectedLabelColor = Color.White
                            )
                        )
                    }
                }

                // Quick action chips
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = formState.workType == "E",
                        onClick = { viewModel.updateWorkType("E") },
                        label = { Text("⚡ E", fontSize = 13.sp) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = NeonOrange,
                            selectedLabelColor = Color.White
                        )
                    )
                    FilterChip(
                        selected = formState.workType == "M",
                        onClick = { viewModel.updateWorkType("M") },
                        label = { Text("🔧 M", fontSize = 13.sp) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = NeonBlue,
                            selectedLabelColor = Color.White
                        )
                    )

                    Spacer(modifier = Modifier.weight(1f))

                    // Photo button (gallery) - visible in both modes
                    IconButton(
                        onClick = { galleryLauncher.launch("image/*") },
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(DarkSurface)
                    ) {
                        Icon(
                            Icons.Outlined.Image,
                            contentDescription = "Галерея",
                            tint = if (formState.photoUri != null) NeonOrange else TextGray
                        )
                    }
                }
            }
        }

        // ==================== PHOTO DISPLAY + CAMERA (ADVISOR MODE) ====================
        if (formState.mode == "advisor") {
            Spacer(modifier = Modifier.height(16.dp))
            
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = DarkCard)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("📷 Фото проблеми", color = NeonBlue, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                    Spacer(modifier = Modifier.height(12.dp))

                    if (formState.photoUri != null) {
                        // Photo thumbnail with remove button
                        Box(
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            AsyncImage(
                                model = formState.photoUri,
                                contentDescription = "Фото",
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(200.dp)
                                    .clip(RoundedCornerShape(12.dp)),
                                contentScale = ContentScale.Crop
                            )
                            // Remove button overlay
                            IconButton(
                                onClick = { viewModel.setPhotoUri(null) },
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .padding(4.dp)
                                    .size(32.dp)
                                    .clip(CircleShape)
                                    .background(Color.Black.copy(alpha = 0.6f))
                            ) {
                                Icon(Icons.Filled.Close, "Видалити", tint = Color.White, modifier = Modifier.size(18.dp))
                            }
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("✅ Фото прикріплено", color = NeonBlue, fontSize = 12.sp, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
                    } else {
                        // Buttons to add photo
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            // Camera button — request permission first
                            OutlinedButton(
                                onClick = {
                                    if (!permissionsState.allPermissionsGranted) {
                                        launchCameraAfterPermission = true
                                        permissionsState.launchMultiplePermissionRequest()
                                    } else {
                                        val photoFile = File(context.cacheDir, "camera/photo_${System.currentTimeMillis()}.jpg").also {
                                            it.parentFile?.mkdirs()
                                        }
                                        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", photoFile)
                                        cameraImageUri = uri
                                        cameraLauncher.launch(uri)
                                    }
                                },
                                modifier = Modifier.weight(1f).height(48.dp),
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = NeonBlue)
                            ) {
                                Icon(Icons.Filled.CameraAlt, null, modifier = Modifier.size(20.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Камера", fontSize = 13.sp)
                            }
                            // Gallery button
                            OutlinedButton(
                                onClick = { galleryLauncher.launch("image/*") },
                                modifier = Modifier.weight(1f).height(48.dp),
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = NeonOrange)
                            ) {
                                Icon(Icons.Outlined.Image, null, modifier = Modifier.size(20.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Галерея", fontSize = 13.sp)
                            }
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("Прикріпи фото — AI побачить проблему", color = TextGray.copy(alpha = 0.5f), fontSize = 11.sp, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
                    }
                }
            }
        }

        // ==================== ACTION BUTTON (Translate OR Advisor) ====================
        if (formState.descriptionUa.isNotEmpty()) {
            Spacer(modifier = Modifier.height(16.dp))

            if (formState.mode == "advisor") {
                // Advisor mode button
                Button(
                    onClick = { viewModel.askAdvisor(apiKey) },
                    enabled = !formState.isTranslating && apiKey.isNotEmpty(),
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = NeonBlue)
                ) {
                    AnimatedContent(targetState = formState.isTranslating) { translating ->
                        if (translating) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Color.White, strokeWidth = 2.dp)
                                Spacer(modifier = Modifier.width(12.dp))
                                Text("Думаю...", color = Color.White, fontWeight = FontWeight.SemiBold)
                            }
                        } else {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Filled.Lightbulb, null, tint = Color.White)
                                Spacer(modifier = Modifier.width(10.dp))
                                val btnText = if (formState.photoUri != null) "ЗАПИТАТИ З ФОТО 📷" else "ЗАПИТАТИ ПОРАДУ"
                                Text(btnText, color = Color.White, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                            }
                        }
                    }
                }
            } else {
                // Translate mode button
                Button(
                    onClick = { viewModel.translate(apiKey) },
                    enabled = !formState.isTranslating && apiKey.isNotEmpty(),
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = NeonOrange)
                ) {
                    AnimatedContent(targetState = formState.isTranslating) { translating ->
                        if (translating) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Color.White, strokeWidth = 2.dp)
                                Spacer(modifier = Modifier.width(12.dp))
                                Text("Перекладаю...", color = Color.White, fontWeight = FontWeight.SemiBold)
                            }
                        } else {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Filled.Translate, null, tint = Color.White)
                                Spacer(modifier = Modifier.width(10.dp))
                                Text("ПЕРЕКЛАСТИ → CZ", color = Color.White, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                            }
                        }
                    }
                }
            }
        }

        // ==================== TRANSLATION RESULT ====================
        AnimatedVisibility(
            visible = translationResult != null,
            enter = fadeIn() + slideInVertically(initialOffsetY = { it / 4 }),
            exit = fadeOut()
        ) {
            translationResult?.let { text ->
                Column {
                    Spacer(modifier = Modifier.height(16.dp))
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = DarkCard)
                    ) {
                        Column(modifier = Modifier.padding(20.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("🇨🇿 Переклад", color = NeonBlue, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                    IconButton(
                                        onClick = {
                                            val clipboard = context.getSystemService(android.content.Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
                                            clipboard.setPrimaryClip(android.content.ClipData.newPlainText("translation", text))
                                        },
                                        modifier = Modifier.size(32.dp)
                                    ) { Icon(Icons.Outlined.ContentCopy, "Копіювати", tint = TextGray, modifier = Modifier.size(18.dp)) }
                                    IconButton(
                                        onClick = {
                                            val share = Intent(Intent.ACTION_SEND).apply { type = "text/plain"; putExtra(Intent.EXTRA_TEXT, text) }
                                            context.startActivity(Intent.createChooser(share, "Поділитися"))
                                        },
                                        modifier = Modifier.size(32.dp)
                                    ) { Icon(Icons.Outlined.Share, "Поділитися", tint = NeonOrange, modifier = Modifier.size(18.dp)) }
                                }
                            }
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(text = text, color = TextWhite, fontSize = 16.sp, lineHeight = 24.sp)
                        }
                    }

                    // Generate report button
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedButton(
                        onClick = { viewModel.generateReport(apiKey) },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = NeonOrange)
                    ) {
                        Icon(Icons.Filled.Description, null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Згенерувати технічний звіт", fontSize = 13.sp)
                    }
                }
            }
        }

        // ==================== SAP FIELDS ====================
        AnimatedVisibility(
            visible = translationResult != null,
            enter = fadeIn() + slideInVertically(initialOffsetY = { it / 4 }),
            exit = fadeOut()
        ) {
            Column {
                Spacer(modifier = Modifier.height(16.dp))
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = DarkCard)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("📋 SAP поля", color = NeonOrange, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                            OutlinedButton(
                                onClick = { viewModel.autoFillSapFields(apiKey) },
                                enabled = !formState.isAutoFilling && apiKey.isNotEmpty(),
                                shape = RoundedCornerShape(10.dp),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = NeonBlue)
                            ) {
                                if (formState.isAutoFilling) {
                                    CircularProgressIndicator(modifier = Modifier.size(16.dp), color = NeonBlue, strokeWidth = 2.dp)
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Заповнюю...", fontSize = 12.sp)
                                } else {
                                    Icon(Icons.Filled.AutoAwesome, null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Авто-заповнити", fontSize = 12.sp)
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // Část obj. (Object Part)
                        Text("Část obj. (MGLC001-005)", color = TextGray, fontSize = 11.sp)
                        DropdownSelector(
                            selectedCode = formState.sapObjectPart,
                            entries = SapCatalogs.objectParts,
                            onSelected = { viewModel.updateSapObjectPart(it) },
                            modifier = Modifier.fillMaxWidth()
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        // Popis škody (Damage Description)
                        Text("Popis škody (MCZ001)", color = TextGray, fontSize = 11.sp)
                        DropdownSelector(
                            selectedCode = formState.sapDamageDesc,
                            entries = SapCatalogs.damageDescriptions,
                            onSelected = { viewModel.updateSapDamageDesc(it) },
                            modifier = Modifier.fillMaxWidth()
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        // Text poškození
                        OutlinedTextField(
                            value = formState.sapDamageText,
                            onValueChange = { viewModel.updateSapDamageText(it) },
                            label = { Text("Text poškození", fontSize = 12.sp) },
                            modifier = Modifier.fillMaxWidth(),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = NeonOrange,
                                unfocusedBorderColor = TextGray.copy(alpha = 0.2f),
                                focusedTextColor = TextWhite,
                                unfocusedTextColor = TextWhite
                            )
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        // Příčina (Cause)
                        Text("Příčina (MGLO001-007)", color = TextGray, fontSize = 11.sp)
                        DropdownSelector(
                            selectedCode = formState.sapCause,
                            entries = SapCatalogs.causes,
                            onSelected = { viewModel.updateSapCause(it) },
                            modifier = Modifier.fillMaxWidth()
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        // Text příčiny
                        OutlinedTextField(
                            value = formState.sapCauseText,
                            onValueChange = { viewModel.updateSapCauseText(it) },
                            label = { Text("Text příčiny", fontSize = 12.sp) },
                            modifier = Modifier.fillMaxWidth(),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = NeonOrange,
                                unfocusedBorderColor = TextGray.copy(alpha = 0.2f),
                                focusedTextColor = TextWhite,
                                unfocusedTextColor = TextWhite
                            )
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        // Dopad (Impact)
                        Text("Dopad", color = TextGray, fontSize = 11.sp)
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            for (impact in SapCatalogs.impacts) {
                                FilterChip(
                                    selected = formState.sapImpact == impact.code,
                                    onClick = { viewModel.updateSapImpact(impact.code) },
                                    label = { Text("${impact.code}. ${impact.description}", fontSize = 11.sp) },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = NeonBlue,
                                        selectedLabelColor = Color.White,
                                        containerColor = DarkSurface
                                    )
                                )
                            }
                        }
                    }
                }
            }
        }

        // ==================== ADVISOR RESULT ====================
        AnimatedVisibility(
            visible = advisorResult != null,
            enter = fadeIn() + slideInVertically(initialOffsetY = { it / 4 }),
            exit = fadeOut()
        ) {
            advisorResult?.let { text ->
                Column {
                    Spacer(modifier = Modifier.height(16.dp))
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = DarkCard)
                    ) {
                        Column(modifier = Modifier.padding(20.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("🔧 Порада", color = NeonBlue, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                    IconButton(
                                        onClick = {
                                            val clipboard = context.getSystemService(android.content.Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
                                            clipboard.setPrimaryClip(android.content.ClipData.newPlainText("advice", text))
                                        },
                                        modifier = Modifier.size(32.dp)
                                    ) { Icon(Icons.Outlined.ContentCopy, "Копіювати", tint = TextGray, modifier = Modifier.size(18.dp)) }
                                    IconButton(
                                        onClick = {
                                            val share = Intent(Intent.ACTION_SEND).apply { type = "text/plain"; putExtra(Intent.EXTRA_TEXT, text) }
                                            context.startActivity(Intent.createChooser(share, "Поділитися"))
                                        },
                                        modifier = Modifier.size(32.dp)
                                    ) { Icon(Icons.Outlined.Share, "Поділитися", tint = NeonOrange, modifier = Modifier.size(18.dp)) }
                                }
                            }
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(text = text, color = TextWhite, fontSize = 15.sp, lineHeight = 22.sp)
                        }
                    }
                }
            }
        }

        // ==================== TECHNICAL REPORT ====================
        AnimatedVisibility(
            visible = technicalReport != null,
            enter = fadeIn() + slideInVertically(initialOffsetY = { it / 4 }),
            exit = fadeOut()
        ) {
            technicalReport?.let { text ->
                Column {
                    Spacer(modifier = Modifier.height(12.dp))
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = DarkSurface)
                    ) {
                        Column(modifier = Modifier.padding(20.dp)) {
                            Text("📋 Технічна зправа (SAP IW41)", color = NeonOrange, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(text = text, color = TextWhite, fontSize = 14.sp, lineHeight = 22.sp)
                        }
                    }
                }
            }
        }

        // ==================== SAVE & EMAIL BUTTONS ====================
        if (translationResult != null) {
            Spacer(modifier = Modifier.height(12.dp))
            Button(
                onClick = { viewModel.saveEntry(apiKey) },
                enabled = !formState.isSaving && apiKey.isNotEmpty(),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = NeonBlue
                )
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.Save, contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("ЗБЕРЕГТИ ЗАПИС", color = Color.White, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Email button - sends full report with technical report
            OutlinedButton(
                onClick = {
                    val s = formState
                    val translation = translationResult ?: ""
                    val report = technicalReport ?: ""
                    val emailBody = buildString {
                        append("⚡ ${if (s.workType == "E") "Elektrická" else "Mechanická"} | #${s.orderId}\n")
                        append("🕐 ${s.startTime}–${s.endTime} (${s.hours}h)\n")
                        append("👷 ${profile?.name ?: ""} (${profile?.email ?: ""})\n\n")
                        append("🇺🇦 UA:\n${s.descriptionUa}\n\n")
                        append("🇨🇿 CZ:\n$translation\n")
                        if (report.isNotEmpty()) {
                            append("\n📋 Technická zpráva:\n$report\n")
                        }
                        // SAP fields
                        if (s.sapObjectPart.isNotEmpty() || s.sapDamageDesc.isNotEmpty()) {
                            append("\n🔧 SAP PM:\n")
                            if (s.sapObjectPart.isNotEmpty()) append("  Část obj.: MGLC ${s.sapObjectPart}\n")
                            if (s.sapDamageDesc.isNotEmpty()) append("  Popis škody: MCZ001 ${s.sapDamageDesc}\n")
                            if (s.sapDamageText.isNotEmpty()) append("  Text: ${s.sapDamageText}\n")
                            if (s.sapCause.isNotEmpty()) append("  Příčina: MGLO ${s.sapCause}\n")
                            if (s.sapCauseText.isNotEmpty()) append("  Text příčiny: ${s.sapCauseText}\n")
                            if (s.sapImpact.isNotEmpty()) append("  Dopad: ${s.sapImpact}\n")
                        }
                    }
                    val subject = "✅ Hlášení práce - Zakázka ${s.orderId} | ${profile?.name ?: ""}"
                    val emailIntent = Intent(Intent.ACTION_SEND).apply {
                        type = "message/rfc822"
                        putExtra(
                            Intent.EXTRA_EMAIL,
                            arrayOf(profile?.reportEmail ?: "Maksym.kovalevskyi@knorr-bremse.com")
                        )
                        putExtra(Intent.EXTRA_BCC, arrayOf(profile?.reportEmailBcc ?: "arrogantdoor697@agentmail.to"))
                        putExtra(Intent.EXTRA_SUBJECT, subject)
                        putExtra(Intent.EXTRA_TEXT, emailBody)
                    }
                    context.startActivity(Intent.createChooser(emailIntent, "Відправити звіт"))
                },
                modifier = Modifier.fillMaxWidth().height(48.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = NeonOrange)
            ) {
                Icon(Icons.Filled.Email, contentDescription = null, modifier = Modifier.size(20.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("ВІДПРАВИТИ НА ПОШТУ", fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
            }
        }

        // ==================== ERROR DISPLAY ====================
        AnimatedVisibility(visible = formState.translationError != null) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(
                    containerColor = Color(0xFFCC0000).copy(alpha = 0.15f)
                )
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("⚠️", fontSize = 18.sp)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = formState.translationError ?: "",
                        color = Color(0xFFFF6666),
                        fontSize = 13.sp,
                        modifier = Modifier.weight(1f)
                    )
                    TextButton(onClick = { viewModel.resetError() }) {
                        Text("✕", color = Color(0xFFFF6666), fontSize = 16.sp)
                    }
                }
            }
        }

        // ==================== API KEY WARNING ====================
        if (apiKey.isEmpty()) {
            Spacer(modifier = Modifier.height(24.dp))
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(
                    containerColor = NeonOrange.copy(alpha = 0.1f)
                )
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("⚡", fontSize = 20.sp)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Введи API ключ в налаштуваннях", color = NeonOrange, fontSize = 13.sp)
                }
            }
        }

        // Success snackbar-like indicator
        AnimatedVisibility(visible = formState.saveSuccess) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(
                    containerColor = NeonBlue.copy(alpha = 0.15f)
                )
            ) {
                Text(
                    text = "✅ Збережено!",
                    modifier = Modifier.padding(12.dp),
                    color = NeonBlue,
                    fontWeight = FontWeight.SemiBold,
                    textAlign = TextAlign.Center
                )
            }
        }

        Spacer(modifier = Modifier.height(40.dp))
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DropdownSelector(
    selectedCode: String,
    entries: List<cz.kovmak.pomocnik.data.model.CatalogEntry>,
    onSelected: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }
    val selectedEntry = entries.find { it.code == selectedCode }

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = !expanded },
        modifier = modifier
    ) {
        OutlinedTextField(
            value = selectedEntry?.let { "${it.code}: ${it.description}" } ?: "",
            onValueChange = {},
            readOnly = true,
            modifier = Modifier.menuAnchor().fillMaxWidth(),
            trailingIcon = { Icon(Icons.Filled.ArrowDropDown, null, tint = TextGray) },
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = NeonOrange,
                unfocusedBorderColor = TextGray.copy(alpha = 0.2f),
                focusedTextColor = TextWhite,
                unfocusedTextColor = if (selectedCode.isNotEmpty()) TextWhite else TextGray.copy(alpha = 0.3f)
            )
        )
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            entries.forEach { entry ->
                DropdownMenuItem(
                    text = { Text("${entry.code}: ${entry.description}") },
                    onClick = {
                        onSelected(entry.code)
                        expanded = false
                    }
                )
            }
        }
    }
}
