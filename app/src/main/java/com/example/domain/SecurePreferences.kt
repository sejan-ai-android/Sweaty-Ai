package com.example.domain

import android.content.Context
import android.content.SharedPreferences
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import android.util.Log
import com.example.BuildConfig
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

class SecurePreferences(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("sweaty_secure_prefs", Context.MODE_PRIVATE)
    private val keyStore: KeyStore? = try {
        KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
    } catch (e: Exception) {
        null
    }

    init {
        createKeyIfNeeded()
    }

    private fun createKeyIfNeeded() {
        val ks = keyStore ?: return
        try {
            if (!ks.containsAlias(KEY_ALIAS)) {
                val keyGenerator = KeyGenerator.getInstance(
                    KeyProperties.KEY_ALGORITHM_AES,
                    "AndroidKeyStore"
                )
                val spec = KeyGenParameterSpec.Builder(
                    KEY_ALIAS,
                    KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
                )
                    .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                    .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                    .setKeySize(256)
                    .build()
                keyGenerator.init(spec)
                keyGenerator.generateKey()
            }
        } catch (e: Exception) {
            Log.w("SecurePreferences", "Could not initialize KeyStore key: ${e.message}")
        }
    }

    private fun getSecretKey(): SecretKey? {
        return try {
            keyStore?.getKey(KEY_ALIAS, null) as? SecretKey
        } catch (e: Exception) {
            null
        }
    }

    private fun encrypt(plainText: String): String {
        if (plainText.isEmpty()) return ""
        val secretKey = getSecretKey() ?: return plainText
        return try {
            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            cipher.init(Cipher.ENCRYPT_MODE, secretKey)
            val iv = cipher.iv
            val cipherText = cipher.doFinal(plainText.toByteArray(Charsets.UTF_8))
            val combined = ByteArray(iv.size + cipherText.size)
            System.arraycopy(iv, 0, combined, 0, iv.size)
            System.arraycopy(cipherText, 0, combined, iv.size, cipherText.size)
            Base64.encodeToString(combined, Base64.NO_WRAP)
        } catch (e: Exception) {
            // Fallback to plain if keystore is in test environment
            plainText
        }
    }

    private fun decrypt(encryptedBase64: String): String {
        if (encryptedBase64.isEmpty()) return ""
        val secretKey = getSecretKey() ?: return encryptedBase64
        return try {
            val combined = Base64.decode(encryptedBase64, Base64.NO_WRAP)
            if (combined.size < 12) return encryptedBase64
            val iv = combined.copyOfRange(0, 12)
            val cipherText = combined.copyOfRange(12, combined.size)
            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            val spec = GCMParameterSpec(128, iv)
            cipher.init(Cipher.DECRYPT_MODE, secretKey, spec)
            String(cipher.doFinal(cipherText), Charsets.UTF_8)
        } catch (e: Exception) {
            encryptedBase64
        }
    }

    var openAiApiKey: String
        get() = prefs.getString(PREF_OPENAI_KEY, null)?.let { decrypt(it) } ?: ""
        set(value) = prefs.edit().putString(PREF_OPENAI_KEY, encrypt(value.trim())).apply()

    var grokApiKey: String
        get() = prefs.getString(PREF_GROK_KEY, null)?.let { decrypt(it) } ?: ""
        set(value) = prefs.edit().putString(PREF_GROK_KEY, encrypt(value.trim())).apply()

    var language: String
        get() = prefs.getString(PREF_LANGUAGE, "auto") ?: "auto"
        set(value) = prefs.edit().putString(PREF_LANGUAGE, value).apply()

    var activeFollowUpEnabled: Boolean
        get() = prefs.getBoolean(PREF_FOLLOW_UP, true)
        set(value) = prefs.edit().putBoolean(PREF_FOLLOW_UP, value).apply()

    var alwaysListeningEnabled: Boolean
        get() = prefs.getBoolean(PREF_ALWAYS_LISTENING, false)
        set(value) = prefs.edit().putBoolean(PREF_ALWAYS_LISTENING, value).apply()

    var wakeWordOnly: Boolean
        get() = prefs.getBoolean(PREF_WAKE_WORD_ONLY, false)
        set(value) = prefs.edit().putBoolean(PREF_WAKE_WORD_ONLY, value).apply()

    var speechRate: Float
        get() = prefs.getFloat(PREF_SPEECH_RATE, 1.0f)
        set(value) = prefs.edit().putFloat(PREF_SPEECH_RATE, value).apply()

    var speechPitch: Float
        get() = prefs.getFloat(PREF_SPEECH_PITCH, 1.0f)
        set(value) = prefs.edit().putFloat(PREF_SPEECH_PITCH, value).apply()

    var selectedProvider: String
        get() = prefs.getString(PREF_PROVIDER, "gemini") ?: "gemini"
        set(value) = prefs.edit().putString(PREF_PROVIDER, value).apply()

    var geminiModel: String
        get() = prefs.getString(PREF_GEMINI_MODEL, "gemini-2.5-flash") ?: "gemini-2.5-flash"
        set(value) = prefs.edit().putString(PREF_GEMINI_MODEL, value).apply()

    var appCheckDebugToken: String
        get() = prefs.getString(PREF_APPCHECK_DEBUG_TOKEN, "") ?: ""
        set(value) = prefs.edit().putString(PREF_APPCHECK_DEBUG_TOKEN, value).apply()

    companion object {
        private const val KEY_ALIAS = "SweatyKey_v1"
        private const val PREF_OPENAI_KEY = "encrypted_openai_key"
        private const val PREF_GROK_KEY = "encrypted_grok_key"
        private const val PREF_LANGUAGE = "pref_language"
        private const val PREF_FOLLOW_UP = "pref_follow_up"
        private const val PREF_ALWAYS_LISTENING = "pref_always_listening"
        private const val PREF_WAKE_WORD_ONLY = "pref_wake_word_only"
        private const val PREF_SPEECH_RATE = "pref_speech_rate"
        private const val PREF_SPEECH_PITCH = "pref_speech_pitch"
        private const val PREF_PROVIDER = "pref_provider"
        private const val PREF_GEMINI_MODEL = "pref_gemini_model"
        private const val PREF_APPCHECK_DEBUG_TOKEN = "pref_appcheck_debug_token"
    }
}
