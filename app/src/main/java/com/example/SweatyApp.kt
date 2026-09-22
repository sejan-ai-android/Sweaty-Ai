package com.example

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import android.util.Log
import com.example.data.db.AppDatabase
import com.example.domain.SecurePreferences
import com.example.service.DeviceActionExecutor
import com.example.service.ai.AppCheckTokenProvider
import com.example.service.ai.FirebaseGeminiService
import com.example.service.ai.LlmClient
import com.google.firebase.Firebase
import com.google.firebase.appcheck.appCheck
import com.google.firebase.appcheck.debug.DebugAppCheckProviderFactory
import com.google.firebase.initialize

class SweatyApp : Application() {

    lateinit var database: AppDatabase
        private set

    lateinit var securePreferences: SecurePreferences
        private set

    lateinit var firebaseGeminiService: FirebaseGeminiService
        private set

    lateinit var llmClient: LlmClient
        private set

    lateinit var actionExecutor: DeviceActionExecutor
        private set

    override fun onCreate() {
        super.onCreate()
        instance = this

        database = AppDatabase.getInstance(this)
        securePreferences = SecurePreferences(this)

        // Initialize persistent App Check Debug Token so it is available in-app and in Firebase AppCheck
        val debugToken = AppCheckTokenProvider.initializeToken(this, securePreferences)

        try {
            Firebase.initialize(context = this)
            Firebase.appCheck.installAppCheckProviderFactory(
                DebugAppCheckProviderFactory.getInstance()
            )
            Log.i("SweatyApp", "Firebase & AppCheck Debug Provider initialized successfully with token: $debugToken")
        } catch (e: Exception) {
            Log.w("SweatyApp", "Firebase initialization note: ${e.message}")
        }

        firebaseGeminiService = FirebaseGeminiService()
        llmClient = LlmClient(firebaseGeminiService)
        actionExecutor = DeviceActionExecutor(this, database.actionLogDao())

        createNotificationChannels()
    }

    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                "sweaty_reminders_channel",
                "Sweaty AI Reminders",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Reminders and alerts from Sweaty AI"
                enableVibration(true)
            }
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }

    companion object {
        lateinit var instance: SweatyApp
            private set
    }
}
