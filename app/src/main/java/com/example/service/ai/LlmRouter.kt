package com.example.service.ai

class LlmRouter(
    private val gemini: FirebaseGeminiService,
    private val llmClient: LlmClient
) {
    suspend fun chat(provider: String, message: String, modelName: String = "gemini-2.5-flash"): String {
        return when (provider.lowercase()) {
            "gemini" -> gemini.generateResponse(message, modelName = modelName)
            else -> llmClient.query(
                userInput = message,
                history = emptyList(),
                memories = emptyList(),
                provider = provider
            ).spokenText
        }
    }
}
