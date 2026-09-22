package com.example.service

import android.content.Context
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.util.Log
import com.example.domain.nlp.LanguageDetector
import java.util.Locale

class TtsManager(
    private val context: Context,
    private val onSpeechStarted: () -> Unit,
    private val onSpeechFinished: () -> Unit
) : TextToSpeech.OnInitListener {

    private var tts: TextToSpeech? = null
    private var isInitialized = false

    private val bnBdLocale = Locale.Builder().setLanguage("bn").setScript("Beng").setRegion("BD").build()
    private val bnInLocale = Locale.Builder().setLanguage("bn").setScript("Beng").setRegion("IN").build()
    private val enUsLocale = Locale.US

    init {
        tts = TextToSpeech(context, this)
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            isInitialized = true
            setupProgressListener()
            // Test if Bengali is available
            val res = tts?.isLanguageAvailable(bnBdLocale)
            if (res == TextToSpeech.LANG_NOT_SUPPORTED || res == TextToSpeech.LANG_MISSING_DATA) {
                Log.w("TtsManager", "bn-BD voice missing or not supported on device TTS engine.")
            }
        } else {
            Log.e("TtsManager", "TTS initialization failed: $status")
        }
    }

    private fun setupProgressListener() {
        tts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
            override fun onStart(utteranceId: String?) {
                onSpeechStarted()
            }

            override fun onDone(utteranceId: String?) {
                onSpeechFinished()
            }

            override fun onError(utteranceId: String?) {
                onSpeechFinished()
            }
        })
    }

    fun speak(text: String, speechRate: Float = 1.0f, pitch: Float = 1.0f) {
        if (!isInitialized || tts == null) return

        // Clean any markdown formatting for natural speech
        val cleanedText = text
            .replace(Regex("""[*_`#~]"""), "")
            .replace(Regex("""\{.*?\}"""), "") // Remove raw JSON if leaked
            .trim()

        if (cleanedText.isBlank()) return

        val detectedLang = LanguageDetector.detectLanguage(cleanedText)
        val targetLocale = if (detectedLang == "bn") {
            val bdSupport = tts?.isLanguageAvailable(bnBdLocale)
            if (bdSupport != TextToSpeech.LANG_NOT_SUPPORTED && bdSupport != TextToSpeech.LANG_MISSING_DATA) {
                bnBdLocale
            } else {
                val inSupport = tts?.isLanguageAvailable(bnInLocale)
                if (inSupport != TextToSpeech.LANG_NOT_SUPPORTED && inSupport != TextToSpeech.LANG_MISSING_DATA) {
                    bnInLocale
                } else {
                    enUsLocale
                }
            }
        } else {
            enUsLocale
        }

        tts?.language = targetLocale
        tts?.setSpeechRate(speechRate)
        tts?.setPitch(pitch)

        val utteranceId = "sweaty_${System.currentTimeMillis()}"
        tts?.speak(cleanedText, TextToSpeech.QUEUE_FLUSH, null, utteranceId)
    }

    fun stop() {
        tts?.stop()
        onSpeechFinished()
    }

    fun shutdown() {
        tts?.stop()
        tts?.shutdown()
        tts = null
        isInitialized = false
    }
}
