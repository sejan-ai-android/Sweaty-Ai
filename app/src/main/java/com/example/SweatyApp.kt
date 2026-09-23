package com.example

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import com.example.data.db.AppDatabase
import com.example.domain.SecurePreferences
import com.example.service.DeviceActionExecutor
import com.example.service.ai.GoogleAiGeminiService
import com.example.service.ai.LlmClient

class SweatyApp : Application() {

    lateinit var database: AppDatabase
        private set

    lateinit var securePreferences: SecurePreferences
        private set

    lateinit var googleAiGeminiService: GoogleAiGeminiService
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
        googleAiGeminiService = GoogleAiGeminiService()
        llmClient = LlmClient(googleAiGeminiService)
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
            manager?.createNotificationChannel(channel)
        }
    }

    companion object {
        lateinit var instance: SweatyApp
            private set
    }
}
