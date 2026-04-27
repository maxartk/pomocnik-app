package cz.kovmak.pomocnik.ui.screens

import android.Manifest
import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import cz.kovmak.pomocnik.viewmodel.WorkViewModel
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.rememberMultiplePermissionsState

@OptIn(ExperimentalMaterial3Api::class, ExperimentalPermissionsApi::class)
@Composable
fun HomeScreen(viewModel: WorkViewModel = viewModel()) {
    val context = LocalContext.current
    val formState by viewModel.formState.collectAsState()
    val translationResult by viewModel.translationResult.collectAsState()
    val profile by viewModel.userProfile.collectAsState()

    val apiKey = profile?.openRouterApiKey ?: ""

    val permissionsState = rememberMultiplePermissionsState(
        listOf(
            Manifest.permission.RECORD_AUDIO,
            Manifest.permission.CAMERA
        )
    )

    // Voice input launcher
    val voiceLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val data = result.data
        val results = data?.getStringArrayListExtra(android.speech.RecognizerIntent.EXTRA_RESULTS)
        if (!results.isNullOrEmpty()) {
            viewModel.updateDescriptionUa(results[0])
        }
    }

    // Camera launcher
    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicturePreview()
    ) { bitmap ->
        // Photo taken - for now we just note it
        // In a full implementation, save to file and get URI
    }

    // Gallery launcher
    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let { viewModel.setPhotoUri(it.toString()) }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Header
        Text(
            text = "⚡ Nový záznam",
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.primary
        )

        if (apiKey.isEmpty()) {
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer
                )
            ) {
                Text(
                    text = "⚠️ Nejprve nastavte API klíč v Nastavení",
                    modifier = Modifier.padding(16.dp),
                    color = MaterialTheme.colorScheme.onErrorContainer
                )
            }
        }

        // Order ID
        OutlinedTextField(
            value = formState.orderId,
            onValueChange = viewModel::updateOrderId,
            label = { Text("Číslo zakázky") },
            modifier = Modifier.fillMaxWidth(),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next)
        )

        // Work Type
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            FilterChip(
                selected = formState.workType == "E",
                onClick = { viewModel.updateWorkType("E") },
                label = { Text("⚡ Elektrická") }
            )
            FilterChip(
                selected = formState.workType == "M",
                onClick = { viewModel.updateWorkType("M") },
                label = { Text("🔧 Mechanická") }
            )
        }

        // Time fields
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedTextField(
                value = formState.startTime,
                onValueChange = viewModel::updateStartTime,
                label = { Text("Začátek") },
                modifier = Modifier.weight(1f),
                placeholder = { Text("07:00") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
            )
            OutlinedTextField(
                value = formState.endTime,
                onValueChange = viewModel::updateEndTime,
                label = { Text("Konec") },
                modifier = Modifier.weight(1f),
                placeholder = { Text("15:30") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
            )
        }

        if (formState.hours > 0) {
            Text(
                text = "⏱️ ${formState.hours} hodin",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.primary
            )
        }

        // Description with voice button
        Text(
            text = "Popis práce (ukrajinsky)",
            style = MaterialTheme.typography.labelLarge
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedTextField(
                value = formState.descriptionUa,
                onValueChange = viewModel::updateDescriptionUa,
                label = { Text("Popište co jste udělal...") },
                modifier = Modifier.weight(1f),
                minLines = 4,
                maxLines = 10
            )
            Column(
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Button(
                    onClick = {
                        val intent = android.content.Intent(android.speech.RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                            putExtra(android.speech.RecognizerIntent.EXTRA_LANGUAGE_MODEL, android.speech.RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                            putExtra(android.speech.RecognizerIntent.EXTRA_LANGUAGE, "uk")
                            putExtra(android.speech.RecognizerIntent.EXTRA_PROMPT, "Надиктуйте опис роботи")
                        }
                        voiceLauncher.launch(intent)
                    },
                    enabled = permissionsState.allPermissionsGranted,
                    content = { Icon(Icons.Default.Mic, contentDescription = "Voice") }
                )
            }
        }

        // Translate button
        Button(
            onClick = { viewModel.translate(apiKey) },
            enabled = formState.descriptionUa.isNotBlank() && !formState.isTranslating && apiKey.isNotEmpty(),
            modifier = Modifier.fillMaxWidth()
        ) {
            if (formState.isTranslating) {
                CircularProgressIndicator(
                    modifier = Modifier.size(24.dp),
                    strokeWidth = 2.dp,
                    color = MaterialTheme.colorScheme.onPrimary
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Překládám...")
            } else {
                Icon(Icons.Default.Translate, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Přeložit do češtiny")
            }
        }

        // Translation result
        if (translationResult != null) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.secondaryContainer
                )
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "🇨🇿 Překlad:",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = translationResult!!,
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                }
            }
        }

        // Materials
        OutlinedTextField(
            value = formState.materials,
            onValueChange = viewModel::updateMaterials,
            label = { Text("Materiály (volitelné)") },
            modifier = Modifier.fillMaxWidth(),
            minLines = 2
        )

        // Photo attachment
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedButton(
                onClick = { galleryLauncher.launch("image/*") },
                modifier = Modifier.weight(1f)
            ) {
                Icon(Icons.Default.PhotoLibrary, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Foto z galerie")
            }
            OutlinedButton(
                onClick = { cameraLauncher.launch(null) },
                modifier = Modifier.weight(1f)
            ) {
                Icon(Icons.Default.CameraAlt, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Vyfotit")
            }
        }

        if (formState.photoUri != null) {
            Text(
                text = "📸 Foto připojeno ✓",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.primary
            )
        }

        // Save button
        Button(
            onClick = { viewModel.saveEntry(apiKey) },
            enabled = !formState.isSaving && formState.descriptionUa.isNotBlank() && apiKey.isNotEmpty(),
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary
            )
        ) {
            if (formState.isSaving) {
                CircularProgressIndicator(
                    modifier = Modifier.size(24.dp),
                    strokeWidth = 2.dp,
                    color = MaterialTheme.colorScheme.onPrimary
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Ukládám...")
            } else {
                Icon(Icons.Default.Save, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Uložit záznam")
            }
        }

        // Error message
        if (formState.translationError != null) {
            Text(
                text = formState.translationError!!,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodyMedium
            )
        }

        // Success message
        if (formState.saveSuccess) {
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            ) {
                Text(
                    text = "✅ Záznam úspěšně uložen!",
                    modifier = Modifier.padding(16.dp),
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    style = MaterialTheme.typography.bodyLarge
                )
            }
        }

        Spacer(modifier = Modifier.height(32.dp))
    }
}
