package com.example.service.ai

import android.util.Log
import com.google.ai.client.generativeai.GenerativeModel
import com.google.ai.client.generativeai.type.content
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class GoogleAiGeminiService {

    companion object {
        const val DEFAULT_MODEL = "gemini-2.5-flash"

        fun normalizeModel(model: String?): String {
            if (model.isNullOrBlank()) return DEFAULT_MODEL
            val lower = model.trim().lowercase()
            return when {
                lower == "gemini-3.1-flash-live" || lower.contains("flash-live") -> "gemini-2.5-flash"
                lower.contains("3.5-flash-lite") -> "gemini-3.1-flash-lite-preview"
                lower.contains("3.8-flash") -> "gemini-2.5-flash"
                lower == "gemini-pro" -> "gemini-3.1-pro-preview"
                lower == "gemini-flash" -> "gemini-2.5-flash"
                else -> model.trim()
            }
        }
    }

    private fun getModel(
        apiKey: String,
        modelName: String = DEFAULT_MODEL,
        systemInstructionText: String? = null
    ): GenerativeModel {
        val cleanKey = apiKey.trim().removeSurrounding("\"").removeSurrounding("'")
        val effectiveModel = normalizeModel(modelName)
        return GenerativeModel(
            modelName = effectiveModel,
            apiKey = cleanKey,
            systemInstruction = systemInstructionText?.let { content { text(it) } }
        )
    }

    suspend fun generateResponse(
        apiKey: String,
        prompt: String,
        modelName: String = DEFAULT_MODEL,
        systemInstructionText: String? = null
    ): String = withContext(Dispatchers.IO) {
        val effectiveModel = normalizeModel(modelName)
        try {
            val model = getModel(apiKey, effectiveModel, systemInstructionText)
            val response = model.generateContent(prompt)
            response.text ?: "No response from Gemini."
        } catch (e: Exception) {
            Log.e("GoogleAiGeminiService", "Error generating response with $effectiveModel", e)
            if (effectiveModel != DEFAULT_MODEL) {
                try {
                    val fallbackModel = getModel(apiKey, DEFAULT_MODEL, systemInstructionText)
                    val response = fallbackModel.generateContent(prompt)
                    return@withContext response.text ?: "No response from Gemini."
                } catch (retryEx: Exception) {
                    Log.e("GoogleAiGeminiService", "Fallback retry failed", retryEx)
                }
            }
            handleGeminiException(e)
        }
    }

    suspend fun generateChatResponse(
        apiKey: String,
        history: List<Pair<String, String>>, // (role, text) - role is "user" or "model"
        userMessage: String,
        systemInstructionText: String? = null,
        modelName: String = DEFAULT_MODEL
    ): String = withContext(Dispatchers.IO) {
        val effectiveModel = normalizeModel(modelName)
        try {
            val model = getModel(apiKey, effectiveModel, systemInstructionText)
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
            Log.e("GoogleAiGeminiService", "Error in chat response with $effectiveModel", e)
            if (effectiveModel != DEFAULT_MODEL) {
                try {
                    val fallbackModel = getModel(apiKey, DEFAULT_MODEL, systemInstructionText)
                    val chat = fallbackModel.startChat(
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
                    return@withContext response.text ?: "No response from Gemini."
                } catch (retryEx: Exception) {
                    Log.e("GoogleAiGeminiService", "Fallback retry failed", retryEx)
                }
            }
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
            msg.contains("404", ignoreCase = true) || msg.contains("NOT_FOUND", ignoreCase = true) || msg.contains("MissingFieldException", ignoreCase = true) || msg.contains("GRpcError", ignoreCase = true) ->
                "Error: Model not found or unsupported for generateContent. Falling back to gemini-2.5-flash."
            msg.contains("UNAVAILABLE", ignoreCase = true) || msg.contains("UnknownHostException", ignoreCase = true) || msg.contains("NETWORK_ERROR", ignoreCase = true) ->
                "Error: No internet connection."
            else -> "Gemini API Error: $msg"
        }
    }
}
