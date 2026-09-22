package com.example.service.ai

import android.content.Context
import android.util.Log
import com.example.domain.SecurePreferences
import com.google.android.gms.tasks.Task
import com.google.android.gms.tasks.Tasks
import com.google.firebase.FirebaseApp
import com.google.firebase.appcheck.AppCheckProvider
import com.google.firebase.appcheck.AppCheckProviderFactory
import com.google.firebase.appcheck.AppCheckToken
import com.google.firebase.appcheck.debug.DebugAppCheckProviderFactory
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.UUID

object AppCheckTokenProvider {
    private const val TAG = "AppCheckTokenProvider"
    private const val FIREBASE_DEBUG_PREF_KEY = "com.google.firebase.appcheck.debug.DEBUG_SECRET"
    private const val FIREBASE_PREFS_NAME = "com.google.firebase.appcheck.debug.store"

    private val _debugToken = MutableStateFlow<String?>(null)
    val debugToken: StateFlow<String?> = _debugToken.asStateFlow()

    /**
     * Ensures a stable UUID debug secret is generated and synced with Firebase's internal
     * DebugAppCheckProvider SharedPreferences, so DebugAppCheckProviderFactory uses this exact secret.
     */
    fun initializeToken(context: Context, securePrefs: SecurePreferences): String {
        var token = securePrefs.appCheckDebugToken

        // Check if Firebase's own debug store already has a secret
        if (token.isBlank()) {
            try {
                val fbPrefs = context.getSharedPreferences(FIREBASE_PREFS_NAME, Context.MODE_PRIVATE)
                val existingFbToken = fbPrefs.getString(FIREBASE_DEBUG_PREF_KEY, null)
                if (!existingFbToken.isNullOrBlank()) {
                    token = existingFbToken
                    securePrefs.appCheckDebugToken = token
                }
            } catch (e: Exception) {
                Log.w(TAG, "Could not check existing Firebase debug prefs: ${e.message}")
            }
        }

        // If still blank, generate a new UUID
        if (token.isBlank()) {
            token = UUID.randomUUID().toString()
            securePrefs.appCheckDebugToken = token
        }

        // Sync token to Firebase's debug storage SharedPreferences
        try {
            val fbPrefs = context.getSharedPreferences(FIREBASE_PREFS_NAME, Context.MODE_PRIVATE)
            fbPrefs.edit().putString(FIREBASE_DEBUG_PREF_KEY, token).apply()
        } catch (e: Exception) {
            Log.w(TAG, "Could not persist debug token into Firebase debug store: ${e.message}")
        }

        _debugToken.value = token
        Log.i(TAG, "Firebase App Check Debug Secret configured: $token")
        return token
    }

    fun getStoredToken(): String? = _debugToken.value
}
