package com.example.domain.nlp

object LanguageDetector {
    /**
     * Detects if the string is predominantly Bengali or English.
     * Bengali Unicode block is U+0980 to U+09FF.
     */
    fun detectLanguage(text: String): String {
        var bengaliCharCount = 0
        var englishCharCount = 0

        for (ch in text) {
            when {
                ch in '\u0980'..'\u09FF' -> bengaliCharCount++
                ch in 'a'..'z' || ch in 'A'..'Z' -> englishCharCount++
            }
        }

        return if (bengaliCharCount > 0 && bengaliCharCount >= englishCharCount / 2) {
            "bn"
        } else {
            "en"
        }
    }

    /**
     * Converts western digits 0-9 to Bengali digits ০-৯.
     */
    fun toBengaliDigits(input: String): String {
        val bengaliDigits = charArrayOf('০', '১', '২', '৩', '৪', '৫', '৬', '৭', '৮', '৯')
        val sb = StringBuilder()
        for (c in input) {
            if (c in '0'..'9') {
                sb.append(bengaliDigits[c - '0'])
            } else {
                sb.append(c)
            }
        }
        return sb.toString()
    }

    /**
     * Converts Bengali digits ০-৯ to standard western digits 0-9.
     */
    fun fromBengaliDigits(input: String): String {
        val sb = StringBuilder()
        for (c in input) {
            when (c) {
                '০' -> sb.append('0')
                '১' -> sb.append('1')
                '২' -> sb.append('2')
                '৩' -> sb.append('3')
                '৪' -> sb.append('4')
                '৫' -> sb.append('5')
                '৬' -> sb.append('6')
                '৭' -> sb.append('7')
                '৮' -> sb.append('8')
                '৯' -> sb.append('9')
                else -> sb.append(c)
            }
        }
        return sb.toString()
    }
}
