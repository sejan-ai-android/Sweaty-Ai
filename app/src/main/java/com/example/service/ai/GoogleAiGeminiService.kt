package com.example.service.ai

import android.util.Log
import com.google.ai.client.generativeai.GenerativeModel
import com.google.ai.client.generativeai.type.content
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class GoogleAiGeminiService {

    private fun getModel(
        apiKey: String,
        modelName: String = "gemini-2.5-flash",
        systemInstructionText: String? = null
    ): GenerativeModel {
        val cleanKey = apiKey.trim().removeSurrounding("\"").removeSurrounding("'")
        return GenerativeModel(
            modelName = modelName,
            apiKey = cleanKey,
            systemInstruction = systemInstructionText?.let { content { text(it) } }
        )
    }

    suspend fun generateResponse(
        apiKey: String,
        prompt: String,
        modelName: String = "gemini-2.5-flash",
        systemInstructionText: String? = null
    ): String = withContext(Dispatchers.IO) {
        try {
            val model = getModel(apiKey, modelName, systemInstructionText)
            val response = model.generateContent(prompt)
            response.text ?: "No response from Gemini."
        } catch (e: Exception) {
            Log.e("GoogleAiGeminiService", "Error generating response", e)
            handleGeminiException(e)
        }
    }

    suspend fun generateChatResponse(
        apiKey: String,
        history: List<Pair<String, String>>, // (role, text) - role is "user" or "model"
        userMessage: String,
        systemInstructionText: String? = null,
        modelName: String = "gemini-2.5-flash"
    ): String = withContext(Dispatchers.IO) {
        try {
            val model = getModel(apiKey, modelName, systemInstructionText)
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
            Log.e("GoogleAiGeminiService", "Error in chat response", e)
            handleGeminiException(e)
        }
    }

    private fun handleGeminiException(e: Exception): String {
        val msg = e.localizedMessage ?: e.message ?: "Unknown error"
        return when {
            msg.contains("API_KEY_INVALID", ignoreCase = true) || (msg.contains("400", ignoreCase = true) && msg.contains("key", ignoreCase = true)) ->
                "Error: Invalid Gemini API key. Please check your Google AI Studio API key in Settings."
            msg.contains("PERMISSION_DENIED", ignoreCase = true) || msg.contains("403", ignoreCase = true) ->
                "Error: Permission denied. Ensure your Google AI Studio API key has access to the requested model."
            msg.contains("RESOURCE_EXHAUSTED", ignoreCase = true) || msg.contains("429", ignoreCase = true) ->
                "Error: Rate limit reached. Please wait a moment and try again."
            msg.contains("UNAVAILABLE", ignoreCase = true) || msg.contains("UnknownHostException", ignoreCase = true) || msg.contains("NETWORK_ERROR", ignoreCase = true) ->
                "Error: No internet connection."
            else -> "Gemini API Error: $msg"
        }
    }
}
