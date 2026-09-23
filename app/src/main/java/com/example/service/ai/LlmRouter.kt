package com.example.service.ai

class LlmRouter(
    private val gemini: GoogleAiGeminiService,
    private val llmClient: LlmClient
) {
    suspend fun chat(
        apiKey: String,
        provider: String,
        message: String,
        modelName: String = "gemini-2.5-flash"
    ): String {
        return when (provider.lowercase()) {
            "gemini" -> gemini.generateResponse(apiKey = apiKey, prompt = message, modelName = modelName)
            else -> llmClient.query(
                userInput = message,
                history = emptyList(),
                memories = emptyList(),
                provider = provider,
                apiKey = apiKey
            ).spokenText
        }
    }
}
