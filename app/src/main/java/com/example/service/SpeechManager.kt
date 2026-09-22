package com.example.service

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.util.Log
import androidx.core.content.ContextCompat
import com.example.domain.nlp.WakeWordDetector
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Locale

enum class AssistantVoiceState {
    IDLE,
    LISTENING,
    THINKING,
    SPEAKING,
    ERROR
}

class SpeechManager(
    private val context: Context,
    private val onResultRecognized: (String) -> Unit,
    private val onWakeWordDetectedOnly: () -> Unit,
    private val onPanicStopDetected: () -> Unit
) : RecognitionListener {

    private val mainHandler = Handler(Looper.getMainLooper())
    private var speechRecognizer: SpeechRecognizer? = null

    private var isListeningSessionActive = false
    private var isPausedForTts = false
    private var currentLanguagePref = "auto"

    private val _isAlwaysListening = MutableStateFlow(false)
    val isAlwaysListening: StateFlow<Boolean> = _isAlwaysListening.asStateFlow()

    private var wakeWordOnlyMode = false

    private val _voiceState = MutableStateFlow(AssistantVoiceState.IDLE)
    val voiceState: StateFlow<AssistantVoiceState> = _voiceState.asStateFlow()

    private val _audioRms = MutableStateFlow(0f)
    val audioRms: StateFlow<Float> = _audioRms.asStateFlow()

    private val _partialText = MutableStateFlow("")
    val partialText: StateFlow<String> = _partialText.asStateFlow()

    private var consecutiveErrors = 0
    private val restartRunnable = Runnable {
        if (_isAlwaysListening.value && !isPausedForTts) {
            startListeningInternal(currentLanguagePref)
        }
    }

    fun initialize() {
        mainHandler.post {
            ensureRecognizerCreated()
        }
    }

    private fun ensureRecognizerCreated(): Boolean {
        if (speechRecognizer != null) return true

        if (!SpeechRecognizer.isRecognitionAvailable(context)) {
            Log.w("SpeechManager", "SpeechRecognizer.isRecognitionAvailable returned false. Attempting creation anyway...")
        }

        return try {
            speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context).apply {
                setRecognitionListener(this@SpeechManager)
            }
            true
        } catch (e: Exception) {
            Log.e("SpeechManager", "Failed to create SpeechRecognizer", e)
            speechRecognizer = null
            false
        }
    }

    fun setAlwaysListening(enabled: Boolean, wakeWordOnly: Boolean = false) {
        _isAlwaysListening.value = enabled
        this.wakeWordOnlyMode = wakeWordOnly
        if (enabled) {
            if (!isListeningSessionActive && !isPausedForTts) {
                startListening(currentLanguagePref)
            }
        } else {
            cancelScheduledRestart()
            if (_voiceState.value == AssistantVoiceState.LISTENING) {
                stopListening()
            }
        }
    }

    fun startListening(languagePref: String = "auto") {
        currentLanguagePref = languagePref
        cancelScheduledRestart()
        mainHandler.post {
            startListeningInternal(languagePref)
        }
    }

    private fun startListeningInternal(languagePref: String) {
        if (isPausedForTts) return

        val hasMicPermission = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED

        if (!hasMicPermission) {
            Log.e("SpeechManager", "Cannot start listening: RECORD_AUDIO permission not granted")
            _voiceState.value = AssistantVoiceState.ERROR
            return
        }

        if (speechRecognizer == null) {
            val created = ensureRecognizerCreated()
            if (!created) {
                _voiceState.value = AssistantVoiceState.ERROR
                return
            }
        }

        try {
            // Cancel any previous session to put recognizer in clean state
            try {
                speechRecognizer?.cancel()
            } catch (_: Exception) {}

            val defaultLocale = Locale.getDefault()
            val selectedLocale = when (languagePref) {
                "bn" -> "bn-BD"
                "en" -> "en-US"
                else -> {
                    val lang = defaultLocale.language
                    if (lang.equals("bn", ignoreCase = true)) "bn-BD" else "en-US"
                }
            }

            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
                putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 3)
                putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE, context.packageName)
                putExtra(RecognizerIntent.EXTRA_LANGUAGE, selectedLocale)
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, selectedLocale)
                // Add secondary language so bilingual queries in BN and EN both work
                val additionalLangs = if (selectedLocale.startsWith("bn")) {
                    arrayOf("en-US", "bn-BD", "bn-IN")
                } else {
                    arrayOf("bn-BD", "bn-IN", "en-US")
                }
                putExtra("android.speech.extra.EXTRA_ADDITIONAL_LANGUAGES", additionalLangs)
            }

            speechRecognizer?.startListening(intent)
            isListeningSessionActive = true
            _voiceState.value = AssistantVoiceState.LISTENING
            _partialText.value = ""
        } catch (e: Exception) {
            Log.e("SpeechManager", "Error starting listening", e)
            _voiceState.value = AssistantVoiceState.ERROR
            scheduleRestartIfNeeded(delayMs = 1500)
        }
    }

    fun stopListening() {
        mainHandler.post {
            cancelScheduledRestart()
            if (isListeningSessionActive) {
                try {
                    speechRecognizer?.stopListening()
                } catch (_: Exception) {}
                isListeningSessionActive = false
            }
        }
    }

    fun cancelListening() {
        mainHandler.post {
            cancelScheduledRestart()
            isListeningSessionActive = false
            _audioRms.value = 0f
            try {
                speechRecognizer?.cancel()
            } catch (_: Exception) {}
            if (_voiceState.value == AssistantVoiceState.LISTENING) {
                _voiceState.value = AssistantVoiceState.IDLE
            }
        }
    }

    fun pauseListeningForTts() {
        isPausedForTts = true
        cancelScheduledRestart()
        mainHandler.post {
            if (isListeningSessionActive) {
                try {
                    speechRecognizer?.cancel()
                } catch (_: Exception) {}
                isListeningSessionActive = false
                _audioRms.value = 0f
            }
        }
    }

    fun resumeListeningAfterTts() {
        isPausedForTts = false
        if (_isAlwaysListening.value) {
            scheduleRestartIfNeeded(delayMs = 400)
        }
    }

    fun setVoiceState(state: AssistantVoiceState) {
        _voiceState.value = state
        if (state != AssistantVoiceState.LISTENING) {
            _audioRms.value = 0f
        }
    }

    private fun cancelScheduledRestart() {
        mainHandler.removeCallbacks(restartRunnable)
    }

    private fun scheduleRestartIfNeeded(delayMs: Long = 300) {
        cancelScheduledRestart()
        if (_isAlwaysListening.value && !isPausedForTts) {
            mainHandler.postDelayed(restartRunnable, delayMs)
        }
    }

    private fun recreateRecognizer() {
        try {
            speechRecognizer?.destroy()
        } catch (_: Exception) {}
        speechRecognizer = null
        isListeningSessionActive = false
        ensureRecognizerCreated()
    }

    // RecognitionListener Callbacks
    override fun onReadyForSpeech(params: Bundle?) {
        consecutiveErrors = 0
        _voiceState.value = AssistantVoiceState.LISTENING
    }

    override fun onBeginningOfSpeech() {
        _voiceState.value = AssistantVoiceState.LISTENING
    }

    override fun onRmsChanged(rmsdB: Float) {
        // Normalize rmsdB (typically -2 to 10) to 0.0 to 1.0 range for UI pulsation
        val normalized = ((rmsdB + 2f) / 12f).coerceIn(0f, 1f)
        _audioRms.value = normalized
    }

    override fun onBufferReceived(buffer: ByteArray?) {}

    override fun onEndOfSpeech() {
        _audioRms.value = 0f
        _voiceState.value = AssistantVoiceState.THINKING
    }

    override fun onError(error: Int) {
        isListeningSessionActive = false
        _audioRms.value = 0f

        Log.w("SpeechManager", "Speech recognition onError: $error")

        val isTimeoutOrSilence = (error == SpeechRecognizer.ERROR_NO_MATCH ||
                error == SpeechRecognizer.ERROR_SPEECH_TIMEOUT)

        if (isTimeoutOrSilence) {
            consecutiveErrors = 0
            if (_isAlwaysListening.value && !isPausedForTts) {
                _voiceState.value = AssistantVoiceState.LISTENING
                scheduleRestartIfNeeded(delayMs = 250)
            } else {
                _voiceState.value = AssistantVoiceState.IDLE
            }
            return
        }

        consecutiveErrors++

        // Handle client or busy error by rebuilding recognizer
        if (error == SpeechRecognizer.ERROR_CLIENT || error == SpeechRecognizer.ERROR_RECOGNIZER_BUSY) {
            recreateRecognizer()
        }

        if (_isAlwaysListening.value && !isPausedForTts) {
            // In always listening mode, back off gently on transient errors instead of breaking
            val backoff = when {
                consecutiveErrors > 5 -> 3000L
                consecutiveErrors > 2 -> 1500L
                else -> 500L
            }
            scheduleRestartIfNeeded(delayMs = backoff)
        } else {
            _voiceState.value = AssistantVoiceState.ERROR
        }
    }

    override fun onResults(results: Bundle?) {
        isListeningSessionActive = false
        _audioRms.value = 0f
        consecutiveErrors = 0

        val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
        val text = matches?.firstOrNull()?.trim() ?: ""

        if (text.isNotBlank()) {
            _partialText.value = text

            // Check for immediate Panic Stop
            val lower = text.lowercase()
            if (lower == "stop" || lower == "থামো" || lower == "হাল্ট" ||
                lower == "shut up" || lower == "quiet" ||
                lower.startsWith("stop ") || lower.startsWith("থামো ") || lower.startsWith("হাল্ট ")
            ) {
                onPanicStopDetected()
                _voiceState.value = AssistantVoiceState.IDLE
                scheduleRestartIfNeeded(delayMs = 1000)
                return
            }

            val wakeCheck = WakeWordDetector.check(text)

            if (wakeWordOnlyMode) {
                if (wakeCheck.detected) {
                    if (wakeCheck.remainingCommand.isBlank()) {
                        onWakeWordDetectedOnly()
                    } else {
                        onResultRecognized(wakeCheck.remainingCommand)
                    }
                } else {
                    // Ignored ambient noise in wake-word-only mode
                    if (_isAlwaysListening.value) {
                        _voiceState.value = AssistantVoiceState.LISTENING
                        scheduleRestartIfNeeded(delayMs = 200)
                    } else {
                        _voiceState.value = AssistantVoiceState.IDLE
                    }
                }
            } else {
                // Direct mode: strip wake word if user said it, otherwise process whole command
                val command = if (wakeCheck.detected) {
                    if (wakeCheck.remainingCommand.isBlank()) {
                        onWakeWordDetectedOnly()
                        return
                    } else {
                        wakeCheck.remainingCommand
                    }
                } else {
                    text
                }
                onResultRecognized(command)
            }
        } else {
            if (_isAlwaysListening.value && !isPausedForTts) {
                _voiceState.value = AssistantVoiceState.LISTENING
                scheduleRestartIfNeeded(delayMs = 250)
            } else {
                _voiceState.value = AssistantVoiceState.IDLE
            }
        }
    }

    override fun onPartialResults(partialResults: Bundle?) {
        val matches = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
        val text = matches?.firstOrNull()?.trim() ?: ""
        if (text.isNotBlank()) {
            _partialText.value = text
            val lower = text.lowercase()
            if (lower == "stop" || lower == "থামো" || lower == "quiet" || lower == "হাল্ট") {
                onPanicStopDetected()
                cancelListening()
            }
        }
    }

    override fun onEvent(eventType: Int, params: Bundle?) {}

    fun destroy() {
        cancelScheduledRestart()
        mainHandler.post {
            try {
                speechRecognizer?.destroy()
            } catch (_: Exception) {}
            speechRecognizer = null
            isListeningSessionActive = false
        }
    }
}
