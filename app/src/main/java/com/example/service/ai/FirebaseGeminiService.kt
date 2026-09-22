package com.example.service.ai

import android.util.Log
import com.google.firebase.Firebase
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.GenerativeBackend
import com.google.firebase.ai.type.content
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class FirebaseGeminiService {

    private fun getModel(
        modelName: String = "gemini-2.5-flash",
        systemInstructionText: String? = null
    ) = Firebase.ai(backend = GenerativeBackend.googleAI())
        .generativeModel(
            modelName = modelName,
            systemInstruction = systemInstructionText?.let { content { text(it) } }
        )

    suspend fun generateResponse(
        prompt: String,
        modelName: String = "gemini-2.5-flash",
        systemInstructionText: String? = null
    ): String = withContext(Dispatchers.IO) {
        try {
            val model = getModel(modelName, systemInstructionText)
            val response = model.generateContent(prompt)
            response.text ?: "No response from Gemini."
        } catch (e: Exception) {
            Log.e("FirebaseGeminiService", "Error generating response", e)
            handleFirebaseException(e)
        }
    }

    suspend fun generateChatResponse(
        history: List<Pair<String, String>>, // (role, text) - role is "user" or "model"
        userMessage: String,
        systemInstructionText: String? = null,
        modelName: String = "gemini-2.5-flash"
    ): String = withContext(Dispatchers.IO) {
        try {
            val model = getModel(modelName, systemInstructionText)
            val chat = model.startChat(
                history = history.map { (role, text) ->
                    val normalizedRole = if (role.equals("model", ignoreCase = true) || role.equals("assistant", ignoreCase = true)) {
                        "model"
                    } else {
                        "user"
                    }
                    content(role = normalizedRole) { text(text) }
                }
            )
            val response = chat.sendMessage(userMessage)
            response.text ?: "No response from Gemini."
        } catch (e: Exception) {
            Log.e("FirebaseGeminiService", "Error in chat response", e)
            handleFirebaseException(e)
        }
    }

    fun handleFirebaseException(e: Exception): String {
        val msg = e.localizedMessage ?: e.message ?: "Unknown error"
        return when {
            msg.contains("PERMISSION_DENIED", ignoreCase = true) || msg.contains("403", ignoreCase = true) ->
                "Error: App Check token missing. Register the debug token in Firebase Console → App Check."
            msg.contains("RESOURCE_EXHAUSTED", ignoreCase = true) || msg.contains("429", ignoreCase = true) ->
                "Error: Rate limit reached. Wait a minute and try again."
            msg.contains("UNAVAILABLE", ignoreCase = true) || msg.contains("UnknownHostException", ignoreCase = true) || msg.contains("NETWORK_ERROR", ignoreCase = true) ->
                "Error: No internet connection."
            else -> "Firebase Gemini Error: $msg"
        }
    }
}

