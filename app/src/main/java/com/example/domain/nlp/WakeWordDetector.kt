package com.example.domain.nlp

object WakeWordDetector {

    // English and Bengali wake phrases / variants / common phonetic misrecognitions
    private val WAKE_WORDS = listOf(
        "hey sweaty",
        "hi sweaty",
        "ok sweaty",
        "okay sweaty",
        "sweaty ai",
        "sweaty",
        // common phonetic misrecognitions
        "hey sweety",
        "hi sweety",
        "ok sweety",
        "okay sweety",
        "sweety ai",
        "sweety",
        "sweetie",
        "hey sweetie",
        "hi sweetie",
        "swaty",
        "hey swaty",
        // Bengali variations
        "হেই সোয়েটি",
        "হাই সোয়েটি",
        "ওহে সোয়েটি",
        "শোনো সোয়েটি",
        "এই সোয়েটি",
        "সোয়েটি এআই",
        "সোয়েটি",
        "সোয়েটি",
        "হেই সুইটি",
        "হাই সুইটি",
        "সুইটি এআই",
        "সুইটি"
    )

    data class WakeWordResult(
        val detected: Boolean,
        val matchedWakeWord: String? = null,
        val remainingCommand: String = ""
    )

    fun check(text: String): WakeWordResult {
        val trimmed = text.trim()
        if (trimmed.isEmpty()) return WakeWordResult(detected = false)

        val lower = trimmed.lowercase()

        for (wake in WAKE_WORDS) {
            val wakeLower = wake.lowercase()
            if (lower.startsWith(wakeLower)) {
                val remainder = trimmed.substring(wakeLower.length)
                    .trimStart(',', ' ', '.', '!', '?', ':', ';', '-')
                    .trim()
                return WakeWordResult(
                    detected = true,
                    matchedWakeWord = wake,
                    remainingCommand = remainder
                )
            } else if (lower.contains(wakeLower)) {
                // Wake word appears elsewhere in the sentence
                val idx = lower.indexOf(wakeLower)
                val before = trimmed.substring(0, idx).trim()
                val after = trimmed.substring(idx + wakeLower.length)
                    .trimStart(',', ' ', '.', '!', '?', ':', ';', '-')
                    .trim()
                val remainder = if (after.isNotEmpty()) after else before
                return WakeWordResult(
                    detected = true,
                    matchedWakeWord = wake,
                    remainingCommand = remainder
                )
            }
        }

        return WakeWordResult(detected = false, remainingCommand = trimmed)
    }

    fun stripWakeWordIfPresent(text: String): String {
        val result = check(text)
        return if (result.detected) {
            result.remainingCommand
        } else {
            text.trim()
        }
    }
}
