package com.example.service.ai

import com.example.data.model.Memory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

data class LlmResponse(
    val spokenText: String,
    val actionJson: String? = null,
    val memoryToSave: Pair<String, String>? = null,
    val isError: Boolean = false
)

class LlmClient(
    private val googleAiGeminiService: GoogleAiGeminiService = GoogleAiGeminiService()
) {

    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(15, TimeUnit.SECONDS)
        .retryOnConnectionFailure(true)
        .build()

    private val jsonMediaType = "application/json; charset=utf-8".toMediaType()

    suspend fun query(
        userInput: String,
        history: List<Pair<String, Boolean>>, // (text, isUser)
        memories: List<Memory>,
        provider: String,
        apiKey: String = "",
        geminiModel: String = GoogleAiGeminiService.DEFAULT_MODEL
    ): LlmResponse = withContext(Dispatchers.IO) {
        val systemPrompt = buildSystemPrompt(memories)

        when (provider.lowercase()) {
            "openai" -> {
                val cleanKey = apiKey.trim().removeSurrounding("\"").removeSurrounding("'")
                if (cleanKey.isBlank()) {
                    return@withContext handleOfflineOrMissingKey(userInput, "OpenAI API Key")
                }
                callOpenAiCompatible(
                    url = "https://api.openai.com/v1/chat/completions",
                    model = "gpt-4o",
                    apiKey = cleanKey,
                    systemPrompt = systemPrompt,
                    history = history,
                    userInput = userInput
                )
            }
            "grok" -> {
                val cleanKey = apiKey.trim().removeSurrounding("\"").removeSurrounding("'")
                if (cleanKey.isBlank()) {
                    return@withContext handleOfflineOrMissingKey(userInput, "xAI Grok API Key")
                }
                callOpenAiCompatible(
                    url = "https://api.x.ai/v1/chat/completions",
                    model = "grok-2",
                    apiKey = cleanKey,
                    systemPrompt = systemPrompt,
                    history = history,
                    userInput = userInput
                )
            }
            else -> {
                val cleanKey = apiKey.trim().removeSurrounding("\"").removeSurrounding("'")
                if (cleanKey.isBlank()) {
                    return@withContext handleOfflineOrMissingKey(userInput, "Google AI Studio API Key")
                }
                callGoogleAiGemini(
                    apiKey = cleanKey,
                    systemPrompt = systemPrompt,
                    history = history,
                    userInput = userInput,
                    modelName = geminiModel.ifBlank { GoogleAiGeminiService.DEFAULT_MODEL }
                )
            }
        }
    }

    private suspend fun callGoogleAiGemini(
        apiKey: String,
        systemPrompt: String,
        history: List<Pair<String, Boolean>>,
        userInput: String,
        modelName: String
    ): LlmResponse {
        val recentHistory = history.takeLast(8).map { (text, isUser) ->
            (if (isUser) "user" else "model") to text
        }

        val rawResponse = googleAiGeminiService.generateChatResponse(
            apiKey = apiKey,
            history = recentHistory,
            userMessage = userInput,
            systemInstructionText = systemPrompt,
            modelName = modelName
        )

        if (rawResponse.startsWith("Error:") || rawResponse.startsWith("Gemini API Error:")) {
            return LlmResponse(
                spokenText = rawResponse,
                isError = true
            )
        }

        return parseRawLlmReply(rawResponse)
    }

    private fun handleOfflineOrMissingKey(userInput: String, keyName: String): LlmResponse {
        val lower = userInput.lowercase()
        // Check local offline device control intents
        val offlineAction = when {
            lower.contains("go home") || lower.contains("হোমে যাও") -> "{\"function\": \"go_home\"}"
            lower.contains("go back") || lower.contains("ব্যাকে যাও") -> "{\"function\": \"go_back\"}"
            lower.contains("notification") || lower.contains("নোটিফিকেশন") -> "{\"function\": \"open_notifications\"}"
            lower.contains("recents") || lower.contains("রিসেন্ট") -> "{\"function\": \"open_recents\"}"
            lower.contains("screenshot") || lower.contains("স্ক্রিনশট") -> "{\"function\": \"take_screenshot\"}"
            lower.contains("whatsapp") || lower.contains("হোয়াটসঅ্যাপ") -> "{\"function\": \"open_app\", \"packageName\": \"com.whatsapp\"}"
            lower.contains("youtube") || lower.contains("ইউটিউব") -> "{\"function\": \"open_app\", \"packageName\": \"com.google.android.youtube\"}"
            lower.contains("settings") || lower.contains("সেটিংস") -> "{\"function\": \"open_app\", \"packageName\": \"com.android.settings\"}"
            else -> null
        }

        if (offlineAction != null) {
            val reply = if (userInput.any { it in '\u0980'..'\u09FF' }) {
                "অ্যাকশন কার্যকর করা হচ্ছে।"
            } else {
                "Executing device action."
            }
            return LlmResponse(spokenText = reply, actionJson = offlineAction)
        }

        val msg = if (userInput.any { it in '\u0980'..'\u09FF' }) {
            "অনলাইন উত্তরের জন্য সেটিংসে আপনার $keyName যোগ করুন।"
        } else {
            "Please configure your $keyName in Settings."
        }
        return LlmResponse(spokenText = msg, isError = true)
    }

    private fun callOpenAiCompatible(
        url: String,
        model: String,
        apiKey: String,
        systemPrompt: String,
        history: List<Pair<String, Boolean>>,
        userInput: String
    ): LlmResponse {
        return try {
            val messages = JSONArray()
            messages.put(JSONObject().apply {
                put("role", "system")
                put("content", systemPrompt)
            })

            val recent = history.takeLast(6)
            for (turn in recent) {
                messages.put(JSONObject().apply {
                    put("role", if (turn.second) "user" else "assistant")
                    put("content", turn.first)
                })
            }

            messages.put(JSONObject().apply {
                put("role", "user")
                put("content", userInput)
            })

            val payload = JSONObject().apply {
                put("model", model)
                put("messages", messages)
                put("temperature", 0.7)
                put("max_tokens", 600)
            }

            val request = Request.Builder()
                .url(url)
                .addHeader("Authorization", "Bearer $apiKey")
                .post(payload.toString().toRequestBody(jsonMediaType))
                .build()

            val response = httpClient.newCall(request).execute()
            val body = response.body?.string() ?: ""

            if (!response.isSuccessful) {
                return LlmResponse(spokenText = "Error from $model (${response.code}): $body", isError = true)
            }

            val json = JSONObject(body)
            val choices = json.optJSONArray("choices")
            val rawReply = choices?.optJSONObject(0)?.optJSONObject("message")?.optString("content") ?: ""
            parseRawLlmReply(rawReply)
        } catch (e: Exception) {
            LlmResponse(spokenText = "Error connecting to $model: ${e.message}", isError = true)
        }
    }

    private fun parseRawLlmReply(rawText: String): LlmResponse {
        var cleanText = rawText.trim()
        var actionJson: String? = null
        var memoryToSave: Pair<String, String>? = null

        // Extract [SAVE_MEMORY: key=... value=...]
        val memRegex = Regex("""\[SAVE_MEMORY:\s*key=([^,\]]+),\s*value=([^\]]+)\]""")
        val memMatch = memRegex.find(cleanText)
        if (memMatch != null) {
            val key = memMatch.groupValues[1].trim()
            val value = memMatch.groupValues[2].trim()
            memoryToSave = Pair(key, value)
            cleanText = cleanText.replace(memMatch.value, "").trim()
        }

        // Extract JSON codeblock or inline JSON object with "function"
        val codeBlockRegex = Regex("""```(?:json)?\s*(\{[\s\S]*?"function"[\s\S]*?\})\s*```""")
        val codeMatch = codeBlockRegex.find(cleanText)
        if (codeMatch != null) {
            actionJson = codeMatch.groupValues[1].trim()
            cleanText = cleanText.replace(codeMatch.value, "").trim()
        } else {
            val inlineJsonRegex = Regex("""(\{[\s\S]*?"function"\s*:\s*"[^"]+"[\s\S]*?\})""")
            val inlineMatch = inlineJsonRegex.find(cleanText)
            if (inlineMatch != null) {
                actionJson = inlineMatch.groupValues[1].trim()
                cleanText = cleanText.replace(inlineMatch.value, "").trim()
            }
        }

        // Remove markdown formatting from voice speech
        val spoken = cleanText
            .replace(Regex("""[*#_~`]"""), "")
            .trim()

        return LlmResponse(
            spokenText = spoken.ifBlank { "Done." },
            actionJson = actionJson,
            memoryToSave = memoryToSave
        )
    }

    private fun buildSystemPrompt(memories: List<Memory>): String {
        val memoryContext = if (memories.isNotEmpty()) {
            val list = memories.take(15).joinToString("\n") { "- ${it.key}: ${it.value}" }
            "\nUSER PERSONAL MEMORIES (Reference these naturally):\n$list\n"
        } else {
            ""
        }

        return """
You are Sweaty, a friendly, calm, bilingual voice-first personal AI assistant with FULL DEVICE CONTROL capability.

CORE RULES:
1. Match the user's language: If the user speaks Bengali, respond in Bengali. If English, respond in English.
2. In Bengali, use respectful, polite form ("আপনি") by default unless user is informal ("তুমি").
3. Keep spoken responses short, natural, and under 3 sentences unless explicitly asked for detail.
4. Use friendly backchannels ("বুঝেছি", "ঠিক আছে", "Got it", "Certainly").
5. Do NOT include markdown styling (no asterisks, hash marks, or bullet points) in your conversational output because it is spoken via Text-To-Speech.

MEMORY STORAGE:
- If the user shares a personal fact, preference, or asks you to remember something, include a special tag at the very end:
  [SAVE_MEMORY: key=<Topic/Key>, value=<Fact to remember>]
  Example: [SAVE_MEMORY: key=Mom's Birthday, value=October 15]

DEVICE CONTROL & FUNCTION CALLING:
- You have unrestricted AccessibilityService device control for personal use.
- When the user asks to perform an action on the phone, append a structured JSON function call at the end of your response.
Supported functions:
  {"function": "go_back"}
  {"function": "go_home"}
  {"function": "open_recents"}
  {"function": "open_notifications"}
  {"function": "open_quick_settings"}
  {"function": "lock_screen"}
  {"function": "take_screenshot"}
  {"function": "tap", "x": 500, "y": 1200}
  {"function": "long_press", "x": 500, "y": 1200}
  {"function": "swipe", "x1": 500, "y1": 1500, "x2": 500, "y2": 500, "duration": 300}
  {"function": "click_by_text", "text": "Settings"}
  {"function": "click_by_id", "viewId": "package:id/view"}
  {"function": "scroll_forward"}
  {"function": "scroll_backward"}
  {"function": "type_text", "text": "message text"}
  {"function": "open_app", "packageName": "com.whatsapp"}
  {"function": "read_screen"}
  {"function": "multi_step", "steps": [{"function": "open_app", "packageName": "com.whatsapp"}, {"function": "click_by_text", "text": "Search"}]}

$memoryContext
""".trimIndent()
    }
}
