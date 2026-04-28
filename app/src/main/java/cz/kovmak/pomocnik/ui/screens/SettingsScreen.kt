package cz.kovmak.pomocnik.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.material3.ExperimentalMaterial3Api
import cz.kovmak.pomocnik.viewmodel.SettingsViewModel
import cz.kovmak.pomocnik.data.network.ModelConfig

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(viewModel: SettingsViewModel = viewModel()) {
    val profile by viewModel.userProfile.collectAsState()

    var name by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var apiKey by remember { mutableStateOf("") }
    var showApiKey by remember { mutableStateOf(false) }
    var startTime by remember { mutableStateOf("07:00") }
    var endTime by remember { mutableStateOf("15:30") }
    var defaultWorkType by remember { mutableStateOf("E") }
    var selectedModel by remember { mutableStateOf(ModelConfig.DEFAULT_MODEL) }

    // Load profile values
    LaunchedEffect(profile) {
        profile?.let {
            name = it.name
            email = it.email
            apiKey = it.openRouterApiKey
            selectedModel = it.selectedModel
            startTime = it.defaultStartTime
            endTime = it.defaultEndTime
            defaultWorkType = it.defaultWorkType
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "⚙️ Nastavení",
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.primary
        )

        // Profile section
        Card(
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "👤 Profil",
                    style = MaterialTheme.typography.titleMedium
                )
                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = name,
                    onValueChange = {
                        name = it
                        viewModel.updateName(it)
                    },
                    label = { Text("Jméno") },
                    modifier = Modifier.fillMaxWidth(),
                    leadingIcon = { Icon(Icons.Default.Person, contentDescription = null) }
                )

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = email,
                    onValueChange = {
                        email = it
                        viewModel.updateEmail(it)
                    },
                    label = { Text("Email") },
                    modifier = Modifier.fillMaxWidth(),
                    leadingIcon = { Icon(Icons.Default.Email, contentDescription = null) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email)
                )
            }
        }

        // API section
        Card(
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "🤖 OpenRouter API",
                    style = MaterialTheme.typography.titleMedium
                )
                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = apiKey,
                    onValueChange = {
                        apiKey = it
                        viewModel.updateApiKey(it)
                    },
                    label = { Text("API klíč") },
                    modifier = Modifier.fillMaxWidth(),
                    leadingIcon = { Icon(Icons.Default.Key, contentDescription = null) },
                    visualTransformation = if (showApiKey) VisualTransformation.None else PasswordVisualTransformation(),
                    trailingIcon = {
                        IconButton(onClick = { showApiKey = !showApiKey }) {
                            Icon(
                                if (showApiKey) Icons.Default.VisibilityOff
                                else Icons.Default.Visibility,
                                contentDescription = null
                            )
                        }
                    },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password)
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "Získejte klíč na openrouter.ai. Používá se pro překlad UA→CZ a analýzu fotek.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // Model selection section
        Card(
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "🧠 Модель AI",
                    style = MaterialTheme.typography.titleMedium
                )
                Spacer(modifier = Modifier.height(8.dp))

                val currentModel = ModelConfig.getModelById(selectedModel)

                Text(
                    text = "Обрано: ${currentModel.displayName}",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(8.dp))

                // Group by category
                ModelConfig.models.groupBy { it.category }.forEach { (category, models) ->
                    val categoryLabel = when (category) {
                        "budget" -> "💰 Бюджетні"
                        "balanced" -> "🎯 Збалансовані"
                        "powerful" -> "💎 Потужні"
                        else -> category
                    }
                    Text(
                        text = categoryLabel,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                    models.forEach { model ->
                        FilterChip(
                            selected = selectedModel == model.id,
                            onClick = {
                                selectedModel = model.id
                                viewModel.updateSelectedModel(model.id)
                            },
                            label = { Text(model.description, style = MaterialTheme.typography.bodySmall) },
                            modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp)
                        )
                    }
                }
            }
        }

        // Defaults section
        Card(
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "⏰ Výchozí hodnoty",
                    style = MaterialTheme.typography.titleMedium
                )
                Spacer(modifier = Modifier.height(12.dp))

                // Default work type
                Text(
                    text = "Typ práce:",
                    style = MaterialTheme.typography.labelLarge
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = defaultWorkType == "E",
                        onClick = {
                            defaultWorkType = "E"
                            viewModel.updateDefaultWorkType("E")
                        },
                        label = { Text("⚡ Elektrická") }
                    )
                    FilterChip(
                        selected = defaultWorkType == "M",
                        onClick = {
                            defaultWorkType = "M"
                            viewModel.updateDefaultWorkType("M")
                        },
                        label = { Text("🔧 Mechanická") }
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Default times
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = startTime,
                        onValueChange = {
                            startTime = it
                            viewModel.updateDefaultTimes(it, endTime)
                        },
                        label = { Text("Výchozí začátek") },
                        modifier = Modifier.weight(1f),
                        placeholder = { Text("07:00") }
                    )
                    OutlinedTextField(
                        value = endTime,
                        onValueChange = {
                            endTime = it
                            viewModel.updateDefaultTimes(startTime, it)
                        },
                        label = { Text("Výchozí konec") },
                        modifier = Modifier.weight(1f),
                        placeholder = { Text("15:30") }
                    )
                }
            }
        }

        // About section
        Card(
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "ℹ️ O aplikaci",
                    style = MaterialTheme.typography.titleMedium
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Pomocnik v1.0\n\nMobilní asistent pro evidenci údržby.\nPřeklad UA→CZ pomocí Gemini 2.0 Flash.\n\nAutor: Maxim Kovmak",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Spacer(modifier = Modifier.height(32.dp))
    }
}
