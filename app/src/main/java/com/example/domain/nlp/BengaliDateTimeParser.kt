package com.example.domain.nlp

import java.util.Calendar

data class ParsedReminder(
    val title: String,
    val timestampMillis: Long,
    val language: String
)

object BengaliDateTimeParser {

    /**
     * Parses a reminder voice command in Bengali or English.
     * E.g.,
     * "আমাকে কাল সকাল ১০টায় সারা-কে ফোন করতে মনে করিয়ে দিও"
     * "Remind me to call Sarah tomorrow at 10 AM"
     * "Remind me in 15 minutes to take medicine"
     * "আমাকে ১০ মিনিট পর ওষুধ খেতে মনে করিয়ে দাও"
     */
    fun parse(input: String): ParsedReminder {
        val lang = LanguageDetector.detectLanguage(input)
        val normalized = LanguageDetector.fromBengaliDigits(input).lowercase()
        val calendar = Calendar.getInstance()

        var title = input
        var offsetMillis: Long? = null

        // Check relative minutes: "in 15 minutes", "১০ মিনিট পর"
        val minRegexEn = Regex("""in\s+(\d+)\s+min(?:ute)?s?""")
        val minMatchEn = minRegexEn.find(normalized)
        if (minMatchEn != null) {
            val mins = minMatchEn.groupValues[1].toLongOrNull() ?: 10
            offsetMillis = mins * 60 * 1000
        }

        val minRegexBn = Regex("""(\d+)\s*(?:মিনিট|মি\.)\s*(?:পর|পরে)""")
        val minMatchBn = minRegexBn.find(normalized)
        if (minMatchBn != null) {
            val mins = minMatchBn.groupValues[1].toLongOrNull() ?: 10
            offsetMillis = mins * 60 * 1000
        }

        // Relative hours: "in 2 hours", "২ ঘণ্টা পর"
        val hourRegexEn = Regex("""in\s+(\d+)\s+hours?""")
        val hourMatchEn = hourRegexEn.find(normalized)
        if (hourMatchEn != null) {
            val hours = hourMatchEn.groupValues[1].toLongOrNull() ?: 1
            offsetMillis = hours * 3600 * 1000
        }

        val hourRegexBn = Regex("""(\d+)\s*(?:ঘণ্টা|ঘন্টা)\s*(?:পর|পরে)""")
        val hourMatchBn = hourRegexBn.find(normalized)
        if (hourMatchBn != null) {
            val hours = hourMatchBn.groupValues[1].toLongOrNull() ?: 1
            offsetMillis = hours * 3600 * 1000
        }

        if (offsetMillis != null) {
            val targetTime = System.currentTimeMillis() + offsetMillis
            title = cleanTitle(input, lang)
            return ParsedReminder(title, targetTime, lang)
        }

        // Check date offsets: tomorrow / কাল / আগামী কাল
        var dayOffset = 0
        if (normalized.contains("tomorrow") || normalized.contains("কাল") || normalized.contains("আগামীকাল")) {
            dayOffset = 1
        } else if (normalized.contains("পরশু") || normalized.contains("পরশুদিন") || normalized.contains("day after tomorrow")) {
            dayOffset = 2
        }

        calendar.add(Calendar.DAY_OF_YEAR, dayOffset)

        // Parse time of day
        var targetHour = 9 // Default morning 9 AM
        var targetMinute = 0

        // Parse explicit hour e.g. "at 5 pm", "৫টায়", "রাত ৮টা", "সকাল ১০টা"
        val timeRegexEn = Regex("""(?:at\s+)?(\d{1,2})(?::(\d{2}))?\s*(am|pm)?""")
        val timeMatchEn = timeRegexEn.find(normalized)

        val timeRegexBn = Regex("""(\d{1,2})(?::(\d{2}))?\s*টা(?:য়|য়)?""")
        val timeMatchBn = timeRegexBn.find(normalized)

        if (timeMatchBn != null) {
            val hr = timeMatchBn.groupValues[1].toIntOrNull() ?: 9
            val min = timeMatchBn.groupValues[2].toIntOrNull() ?: 0
            val isNight = normalized.contains("রাত") || normalized.contains("সন্ধ্যা") || normalized.contains("বিকেল") || normalized.contains("বিকাল") || normalized.contains("দুপুর")
            targetHour = if (isNight && hr < 12) hr + 12 else hr
            targetMinute = min
        } else if (timeMatchEn != null) {
            var hr = timeMatchEn.groupValues[1].toIntOrNull() ?: 9
            val min = timeMatchEn.groupValues[2].toIntOrNull() ?: 0
            val ampm = timeMatchEn.groupValues[3]
            if (ampm == "pm" && hr < 12) hr += 12
            if (ampm == "am" && hr == 12) hr = 0
            targetHour = hr
            targetMinute = min
        } else {
            // General time of day terms
            if (normalized.contains("morning") || normalized.contains("সকাল")) {
                targetHour = 9
            } else if (normalized.contains("noon") || normalized.contains("দুপুর")) {
                targetHour = 13
            } else if (normalized.contains("afternoon") || normalized.contains("বিকাল") || normalized.contains("বিকেল")) {
                targetHour = 16
            } else if (normalized.contains("evening") || normalized.contains("সন্ধ্যা")) {
                targetHour = 19
            } else if (normalized.contains("night") || normalized.contains("রাত")) {
                targetHour = 21
            } else {
                // If today and time already passed, default to in 1 hour
                if (dayOffset == 0) {
                    calendar.add(Calendar.HOUR_OF_DAY, 1)
                    targetHour = calendar.get(Calendar.HOUR_OF_DAY)
                    targetMinute = 0
                }
            }
        }

        calendar.set(Calendar.HOUR_OF_DAY, targetHour)
        calendar.set(Calendar.MINUTE, targetMinute)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)

        // If target time is in the past for today, push to tomorrow
        if (dayOffset == 0 && calendar.timeInMillis <= System.currentTimeMillis()) {
            calendar.add(Calendar.DAY_OF_YEAR, 1)
        }

        title = cleanTitle(input, lang)
        return ParsedReminder(title, calendar.timeInMillis, lang)
    }

    private fun cleanTitle(raw: String, lang: String): String {
        var clean = raw
            .replace(Regex("""(?i)^remind me (?:to|that)?\s*"""), "")
            .replace(Regex("""(?i)\s*(tomorrow|today|tonight|in \d+ minutes|in \d+ hours|at \d+.*)"""), "")
            .replace(Regex("""আমাকে\s*"""), "")
            .replace(Regex("""মনে করিয়ে (?:দিও|দাও|দিন)"""), "")
            .replace(Regex("""(?:কাল|আজ|আগামীকাল|পরশু|সকাল|রাত|সন্ধ্যা|দুপুর|বিকেল)?\s*\d+\s*টায়?"""), "")
            .replace(Regex("""\d+\s*(?:মিনিট|ঘণ্টা|ঘন্টা)\s*(?:পর|পরে)"""), "")
            .trim()

        if (clean.isBlank()) {
            clean = if (lang == "bn") "রিমাইন্ডার" else "Reminder"
        }
        return clean
    }
}
