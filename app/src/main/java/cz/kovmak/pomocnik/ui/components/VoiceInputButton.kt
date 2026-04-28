package cz.kovmak.pomocnik.ui.components

import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.SpeechRecognizer
import android.speech.RecognizerIntent
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * In-app speech recognizer that stays inside the app.
 * No Google popup dialog — uses SpeechRecognizer directly.
 * Supports long dictation with continuous listening.
 */

private val NeonOrange = Color(0xFFFF6B35)
private val NeonBlue = Color(0xFF00B4D8)
private val DarkBg = Color(0xFF0A0E21)
private val TextGray = Color(0xFF8892B0)

@Composable
fun VoiceInputButton(
    onResult: (String) -> Unit,
    isProcessing: Boolean = false,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var isListening by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    // Pulsing animation for listening state
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.15f,
        animationSpec = infiniteRepeatable(
            animation = tween(600, easing = EaseInOutCubic),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseScale"
    )

    // SpeechRecognizer instance — created/disposed with Compose lifecycle
    val speechRecognizer = remember {
        SpeechRecognizer.createSpeechRecognizer(context)
    }

    DisposableEffect(Unit) {
        onDispose {
            speechRecognizer.destroy()
        }
    }

    val recognitionListener = remember {
        object : RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) {
                isListening = true
                errorMessage = null
            }

            override fun onBeginningOfSpeech() {}
            override fun onRmsChanged(rmsdB: Float) {}
            override fun onBufferReceived(buffer: ByteArray?) {}
            override fun onEndOfSpeech() {
                isListening = false
            }

            override fun onError(error: Int) {
                isListening = false
                errorMessage = when (error) {
                    SpeechRecognizer.ERROR_NO_MATCH -> null // silent — just no speech detected
                    SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "Час очікування мовлення вийшов"
                    SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "Немає дозволу на мікрофон"
                    SpeechRecognizer.ERROR_NETWORK -> "Помилка мережі"
                    SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> "Таймаут мережі"
                    SpeechRecognizer.ERROR_CLIENT -> null // cancelled by user
                    else -> "Помилка розпізнавання: $error"
                }
            }

            override fun onPartialResults(partialResults: Bundle?) {
                // Show partial results in real-time if desired
            }

            override fun onEvent(eventType: Int, params: Bundle?) {}

            override fun onResults(results: Bundle?) {
                isListening = false
                val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                if (!matches.isNullOrEmpty()) {
                    onResult(matches.first())
                }
            }
        }
    }

    // Set listener once
    DisposableEffect(speechRecognizer, recognitionListener) {
        speechRecognizer.setRecognitionListener(recognitionListener)
        onDispose {
            speechRecognizer.destroy()
        }
    }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
    ) {
        Box(
            modifier = Modifier
                .size(120.dp * (if (isListening) pulseScale else 1f))
                .shadow(16.dp, CircleShape)
                .clip(CircleShape)
                .background(
                    brush = Brush.radialGradient(
                        colors = if (isListening) {
                            listOf(NeonBlue, Color(0xFF006994))
                        } else {
                            listOf(NeonOrange, Color(0xFFCC5500))
                        }
                    )
                ),
            contentAlignment = Alignment.Center
        ) {
            IconButton(
                onClick = {
                    if (isListening) {
                        // Stop listening
                        speechRecognizer.stopListening()
                        isListening = false
                    } else {
                        // Start listening — in-app, Ukrainian, long dictation
                        errorMessage = null
                        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                            putExtra(RecognizerIntent.EXTRA_LANGUAGE, "uk-UA")
                            putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, "uk-UA")
                            putExtra(RecognizerIntent.EXTRA_ONLY_RETURN_LANGUAGE_PREFERENCE, false)
                            putExtra(RecognizerIntent.EXTRA_PROMPT, "Надиктуйте опис роботи")
                            // Longer max results for dictation
                            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
                            // Request partial results for real-time feedback
                            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
                        }
                        speechRecognizer.startListening(intent)
                    }
                },
                modifier = Modifier.fillMaxSize()
            ) {
                Icon(
                    if (isListening) Icons.Filled.Mic else Icons.Filled.Mic,
                    contentDescription = if (isListening) "Зупинити" else "Диктувати",
                    tint = Color.White,
                    modifier = Modifier.size(48.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        Text(
            text = if (isListening) "🎧 Слухаю..." else "Натисни і говори",
            fontSize = 14.sp,
            color = if (isListening) NeonBlue else TextGray,
            textAlign = TextAlign.Center,
            fontWeight = if (isListening) FontWeight.Bold else FontWeight.Normal
        )
        Text(
            text = if (isListening) "Натисни ще раз щоб зупинити" else "або пиши нижче",
            fontSize = 12.sp,
            color = TextGray,
            textAlign = TextAlign.Center
        )

        // Show error message
        errorMessage?.let { msg ->
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = msg,
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.error,
                textAlign = TextAlign.Center
            )
        }
    }
}