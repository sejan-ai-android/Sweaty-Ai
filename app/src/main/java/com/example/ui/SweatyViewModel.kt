package com.example.ui

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.SweatyApp
import com.example.data.model.ActionLog
import com.example.data.model.ChatMessage
import com.example.data.model.Memory
import com.example.data.model.Reminder
import com.example.domain.nlp.BengaliDateTimeParser
import com.example.domain.nlp.LanguageDetector
import com.example.domain.SecurePreferences
import com.example.service.AssistantVoiceState
import com.example.service.DeviceActionExecutor
import com.example.service.ExecutionResult
import com.example.service.ReminderBroadcastReceiver
import com.example.service.SpeechManager
import com.example.service.SweatyAccessibilityService
import com.example.service.TtsManager
import com.example.service.ai.LlmClient
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

enum class NavScreen {
    HOME,
    CHAT,
    MEMORY,
    REMINDERS,
    CONTROL,
    SETTINGS
}

class SweatyViewModel(private val app: SweatyApp) : AndroidViewModel(app) {

    private val db = app.database
    val securePrefs: SecurePreferences = app.securePreferences
    private val llmClient: LlmClient = app.llmClient
    private val actionExecutor: DeviceActionExecutor = app.actionExecutor

    private val _currentScreen = MutableStateFlow(NavScreen.HOME)
    val currentScreen: StateFlow<NavScreen> = _currentScreen.asStateFlow()

    // State flows
    private val _voiceState = MutableStateFlow(AssistantVoiceState.IDLE)
    val voiceState: StateFlow<AssistantVoiceState> = _voiceState.asStateFlow()

    private val _liveTranscript = MutableStateFlow("")
    val liveTranscript: StateFlow<String> = _liveTranscript.asStateFlow()

    private val _audioRms = MutableStateFlow(0f)
    val audioRms: StateFlow<Float> = _audioRms.asStateFlow()

    private val _lastAssistantReply = MutableStateFlow<String?>(null)
    val lastAssistantReply: StateFlow<String?> = _lastAssistantReply.asStateFlow()

    private val _followUpRemainingSeconds = MutableStateFlow(0)
    val followUpRemainingSeconds: StateFlow<Int> = _followUpRemainingSeconds.asStateFlow()

    private val _pendingConfirmationResult = MutableStateFlow<ExecutionResult?>(null)
    val pendingConfirmationResult: StateFlow<ExecutionResult?> = _pendingConfirmationResult.asStateFlow()

    private val _memorySearchQuery = MutableStateFlow("")
    val memorySearchQuery: StateFlow<String> = _memorySearchQuery.asStateFlow()

    private var followUpJob: Job? = null

    // Flows from Room DB
    val chatMessages: StateFlow<List<ChatMessage>> = db.chatMessageDao()
        .getAllMessages()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val memories: StateFlow<List<Memory>> = db.memoryDao()
        .getAllMemories()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val reminders: StateFlow<List<Reminder>> = db.reminderDao()
        .getAllReminders()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val actionLogs: StateFlow<List<ActionLog>> = db.actionLogDao()
        .getAllLogs()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val isAccessibilityActive: StateFlow<Boolean> = SweatyAccessibilityService.isServiceActive

    // Managers
    private val speechManager = SpeechManager(
        context = app,
        onResultRecognized = { text ->
            onUserSpoke(text)
        },
        onWakeWordDetectedOnly = {
            onWakeWordOnlyDetected()
        },
        onPanicStopDetected = {
            panicStop()
        }
    )

    val isAlwaysListening: StateFlow<Boolean> = speechManager.isAlwaysListening
    private val _wakeWordOnly = MutableStateFlow(securePrefs.wakeWordOnly)
    val wakeWordOnly: StateFlow<Boolean> = _wakeWordOnly.asStateFlow()

    private val ttsManager = TtsManager(
        context = app,
        onSpeechStarted = {
            speechManager.pauseListeningForTts()
            _voiceState.value = AssistantVoiceState.SPEAKING
        },
        onSpeechFinished = {
            _voiceState.value = AssistantVoiceState.IDLE
            speechManager.resumeListeningAfterTts()
            startFollowUpWindowIfNeeded()
        }
    )

    init {
        speechManager.initialize()
        if (securePrefs.alwaysListeningEnabled) {
            speechManager.setAlwaysListening(true, securePrefs.wakeWordOnly)
        }

        // Sync speechManager flows
        viewModelScope.launch {
            speechManager.voiceState.collect {
                // If TTS is speaking, don't overwrite with IDLE
                if (_voiceState.value != AssistantVoiceState.SPEAKING || it != AssistantVoiceState.IDLE) {
                    _voiceState.value = it
                }
            }
        }
        viewModelScope.launch {
            speechManager.audioRms.collect { _audioRms.value = it }
        }
        viewModelScope.launch {
            speechManager.partialText.collect { _liveTranscript.value = it }
        }
    }

    fun navigateTo(screen: NavScreen) {
        _currentScreen.value = screen
    }

    fun onPermissionsChecked(granted: Boolean) {
        if (granted && securePrefs.alwaysListeningEnabled) {
            speechManager.setAlwaysListening(true, securePrefs.wakeWordOnly)
        }
    }

    fun toggleAlwaysListening() {
        val newState = !isAlwaysListening.value
        setAlwaysListening(newState)
    }

    fun setLanguage(language: String) {
        securePrefs.language = language
    }

    fun setSpeechRate(rate: Float) {
        securePrefs.speechRate = rate
    }

    fun setSpeechPitch(pitch: Float) {
        securePrefs.speechPitch = pitch
    }

    fun setAiProvider(provider: String) {
        securePrefs.selectedProvider = provider
    }

    fun setGeminiModel(model: String) {
        securePrefs.geminiModel = model
    }

    fun setGeminiApiKey(key: String) {
        securePrefs.geminiApiKey = key
    }

    fun setOpenAiApiKey(key: String) {
        securePrefs.openAiApiKey = key
    }

    fun setGrokApiKey(key: String) {
        securePrefs.grokApiKey = key
    }

    fun setActiveFollowUpEnabled(enabled: Boolean) {
        securePrefs.activeFollowUpEnabled = enabled
        if (!enabled) {
            followUpJob?.cancel()
            _followUpRemainingSeconds.value = 0
        }
    }

    fun setAlwaysListening(enabled: Boolean) {
        securePrefs.alwaysListeningEnabled = enabled
        speechManager.setAlwaysListening(enabled, securePrefs.wakeWordOnly)
    }

    fun setWakeWordOnly(enabled: Boolean) {
        securePrefs.wakeWordOnly = enabled
        _wakeWordOnly.value = enabled
        if (securePrefs.alwaysListeningEnabled) {
            speechManager.setAlwaysListening(true, enabled)
        }
    }

    fun onWakeWordOnlyDetected() {
        ttsManager.stop()
        followUpJob?.cancel()
        _followUpRemainingSeconds.value = 0
        val prompt = if (securePrefs.language == "bn") "বলুন, আমি শুনছি।" else "I'm listening."
        _lastAssistantReply.value = prompt
        ttsManager.speak(prompt, securePrefs.speechRate, securePrefs.speechPitch)
    }

    fun toggleVoiceListening() {
        if (_voiceState.value == AssistantVoiceState.SPEAKING) {
            ttsManager.stop()
            speechManager.startListening(securePrefs.language)
        } else if (_voiceState.value == AssistantVoiceState.LISTENING) {
            if (speechManager.isAlwaysListening.value) {
                setAlwaysListening(false)
            } else {
                speechManager.stopListening()
                _voiceState.value = AssistantVoiceState.IDLE
            }
        } else {
            ttsManager.stop()
            followUpJob?.cancel()
            _followUpRemainingSeconds.value = 0
            speechManager.startListening(securePrefs.language)
        }
    }

    fun panicStop() {
        ttsManager.stop()
        speechManager.cancelListening()
        followUpJob?.cancel()
        _followUpRemainingSeconds.value = 0
        _voiceState.value = AssistantVoiceState.IDLE
        _pendingConfirmationResult.value = null
        SweatyAccessibilityService.instance?.triggerPanicStop()
    }

    fun onUserSpoke(input: String) {
        followUpJob?.cancel()
        _followUpRemainingSeconds.value = 0
        _liveTranscript.value = input
        _voiceState.value = AssistantVoiceState.THINKING

        viewModelScope.launch {
            val lang = LanguageDetector.detectLanguage(input)
            // Record user message
            db.chatMessageDao().insertMessage(
                ChatMessage(content = input, isUser = true, language = lang)
            )

            // Check if this is a reminder command
            val isReminder = input.contains("remind", ignoreCase = true) ||
                    input.contains("মনে করিয়ে", ignoreCase = true) ||
                    input.contains("মনে করায়", ignoreCase = true)

            if (isReminder) {
                handleReminderCommand(input, lang)
                return@launch
            }

            // Normal or Device Action query via LLM
            val memoryList = db.memoryDao().getRecentMemories(20)
            val history = db.chatMessageDao().getRecentMessages(6).reversed().map {
                Pair(it.content, it.isUser)
            }

            val llmResponse = llmClient.query(
                userInput = input,
                history = history,
                memories = memoryList,
                provider = securePrefs.selectedProvider,
                apiKey = when (securePrefs.selectedProvider) {
                    "openai" -> securePrefs.openAiApiKey
                    "grok" -> securePrefs.grokApiKey
                    else -> securePrefs.geminiApiKey
                },
                geminiModel = securePrefs.geminiModel
            )

            _lastAssistantReply.value = llmResponse.spokenText

            // Auto-save memory if extracted by LLM
            if (llmResponse.memoryToSave != null) {
                val (key, value) = llmResponse.memoryToSave
                db.memoryDao().insertMemory(
                    Memory(key = key, value = value, language = lang)
                )
            }

            // Record Assistant Message
            db.chatMessageDao().insertMessage(
                ChatMessage(
                    content = llmResponse.spokenText,
                    isUser = false,
                    language = lang,
                    actionJson = llmResponse.actionJson
                )
            )

            // Speak response
            ttsManager.speak(
                text = llmResponse.spokenText,
                speechRate = securePrefs.speechRate,
                pitch = securePrefs.speechPitch
            )

            // Execute Device Action if present
            if (llmResponse.actionJson != null) {
                val execResult = actionExecutor.executeActionJson(llmResponse.actionJson)
                if (execResult.requiresConfirmation) {
                    _pendingConfirmationResult.value = execResult
                }
            }
        }
    }

    private suspend fun handleReminderCommand(input: String, lang: String) {
        val parsed = BengaliDateTimeParser.parse(input)
        val reminder = Reminder(
            title = parsed.title,
            targetTimeMillis = parsed.timestampMillis,
            language = lang
        )
        val id = db.reminderDao().insertReminder(reminder)
        scheduleAlarm(id, parsed.title, parsed.timestampMillis, lang)

        val dateStr = SimpleDateFormat("h:mm a, d MMM", Locale.getDefault()).format(Date(parsed.timestampMillis))
        val reply = if (lang == "bn") {
            "ঠিক আছে, $dateStr-এ আপনাকে মনে করিয়ে দেব: ${parsed.title}"
        } else {
            "Got it! I will remind you on $dateStr to: ${parsed.title}"
        }

        _lastAssistantReply.value = reply
        db.chatMessageDao().insertMessage(
            ChatMessage(content = reply, isUser = false, language = lang)
        )
        ttsManager.speak(reply, securePrefs.speechRate, securePrefs.speechPitch)
    }

    private fun scheduleAlarm(id: Long, title: String, timeMillis: Long, lang: String) {
        if (timeMillis <= System.currentTimeMillis()) return
        try {
            val alarmManager = app.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            val intent = Intent(app, ReminderBroadcastReceiver::class.java).apply {
                putExtra(ReminderBroadcastReceiver.EXTRA_REMINDER_ID, id)
                putExtra(ReminderBroadcastReceiver.EXTRA_REMINDER_TITLE, title)
                putExtra(ReminderBroadcastReceiver.EXTRA_REMINDER_LANG, lang)
            }
            val pending = PendingIntent.getBroadcast(
                app,
                id.toInt(),
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, timeMillis, pending)
            } else {
                alarmManager.setExact(AlarmManager.RTC_WAKEUP, timeMillis, pending)
            }
        } catch (_: Exception) {}
    }

    private fun startFollowUpWindowIfNeeded() {
        if (!securePrefs.activeFollowUpEnabled) return

        followUpJob?.cancel()
        followUpJob = viewModelScope.launch {
            for (sec in 30 downTo 1) {
                _followUpRemainingSeconds.value = sec
                delay(1000)
            }
            _followUpRemainingSeconds.value = 0
        }
    }

    fun confirmPendingAction() {
        val pending = _pendingConfirmationResult.value ?: return
        _pendingConfirmationResult.value = null
        viewModelScope.launch {
            if (pending.pendingAction != null) {
                actionExecutor.executeActionJson(pending.pendingAction)
            }
        }
    }

    fun cancelPendingAction() {
        _pendingConfirmationResult.value = null
    }

    fun playMorningBriefing() {
        viewModelScope.launch {
            val lang = if (securePrefs.language == "bn") "bn" else "en"
            val pendingReminders = db.reminderDao().getPendingRemindersList()

            val greeting = if (lang == "bn") "শুভ সকাল!" else "Good morning!"
            val reminderCount = pendingReminders.size

            val reminderText = if (lang == "bn") {
                if (reminderCount > 0) "আজ আপনার $reminderCount টি রিমাইন্ডার রয়েছে।" else "আজ কোনো জরুরি রিমাইন্ডার নেই।"
            } else {
                if (reminderCount > 0) "You have $reminderCount upcoming reminder${if (reminderCount > 1) "s" else ""}." else "You have no pending reminders today."
            }

            val briefing = "$greeting $reminderText Sweaty AI is ready for device control and voice commands."

            _lastAssistantReply.value = briefing
            db.chatMessageDao().insertMessage(
                ChatMessage(content = briefing, isUser = false, language = lang)
            )
            ttsManager.speak(briefing, securePrefs.speechRate, securePrefs.speechPitch)
        }
    }

    // Memory operations
    fun addMemory(key: String, value: String, category: String) {
        viewModelScope.launch {
            val lang = LanguageDetector.detectLanguage("$key $value")
            db.memoryDao().insertMemory(
                Memory(key = key, value = value, category = category, language = lang)
            )
        }
    }

    fun deleteMemory(memory: Memory) {
        viewModelScope.launch {
            db.memoryDao().deleteMemory(memory)
        }
    }

    fun setMemorySearchQuery(query: String) {
        _memorySearchQuery.value = query
    }

    // Reminder operations
    fun addManualReminder(title: String, timeMillis: Long) {
        viewModelScope.launch {
            val lang = LanguageDetector.detectLanguage(title)
            val id = db.reminderDao().insertReminder(
                Reminder(title = title, targetTimeMillis = timeMillis, source = "manual", language = lang)
            )
            scheduleAlarm(id, title, timeMillis, lang)
        }
    }

    fun toggleReminderCompleted(reminder: Reminder) {
        viewModelScope.launch {
            db.reminderDao().setCompleted(reminder.id, !reminder.isCompleted)
        }
    }

    fun deleteReminder(reminder: Reminder) {
        viewModelScope.launch {
            db.reminderDao().deleteReminder(reminder)
        }
    }

    // Device action test bench
    fun testDeviceAction(actionJson: String) {
        viewModelScope.launch {
            val res = actionExecutor.executeActionJson(actionJson)
            val msg = res.message + (res.screenData?.let { "\n\n$it" } ?: "")
            _lastAssistantReply.value = msg
        }
    }

    fun clearChat() {
        viewModelScope.launch {
            db.chatMessageDao().clearMessages()
        }
    }

    override fun onCleared() {
        super.onCleared()
        speechManager.destroy()
        ttsManager.shutdown()
        followUpJob?.cancel()
    }

    companion object {
        fun provideFactory(app: SweatyApp): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return SweatyViewModel(app) as T
            }
        }
    }
}
