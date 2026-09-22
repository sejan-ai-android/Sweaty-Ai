package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.domain.nlp.BengaliDateTimeParser
import com.example.domain.nlp.LanguageDetector
import com.example.domain.nlp.WakeWordDetector
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ExampleRobolectricTest {

    @Test
    fun `read string from context`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("Sweaty AI", appName)
    }

    @Test
    fun `test language detection`() {
        assertEquals("bn", LanguageDetector.detectLanguage("আমাকে কাল মনে করিয়ে দিও"))
        assertEquals("en", LanguageDetector.detectLanguage("Remind me tomorrow at 10 AM"))
    }

    @Test
    fun `test bengali digit conversions`() {
        val bengaliNum = "১২৩৪৫"
        val westernNum = LanguageDetector.fromBengaliDigits(bengaliNum)
        assertEquals("12345", westernNum)
        assertEquals(bengaliNum, LanguageDetector.toBengaliDigits("12345"))
    }

    @Test
    fun `test reminder parsing`() {
        val parsed = BengaliDateTimeParser.parse("Remind me in 15 minutes to drink water")
        assertTrue(parsed.timestampMillis > System.currentTimeMillis())
        assertEquals("en", parsed.language)
    }

    @Test
    fun `test wake word detector english and bengali`() {
        val enRes = WakeWordDetector.check("Hey Sweaty, what is the weather today?")
        assertTrue(enRes.detected)
        assertEquals("what is the weather today?", enRes.remainingCommand)

        val bnRes = WakeWordDetector.check("সোয়েটি, লাইট অন করো")
        assertTrue(bnRes.detected)
        assertEquals("লাইট অন করো", bnRes.remainingCommand)

        val wakeOnlyRes = WakeWordDetector.check("Hey Sweaty")
        assertTrue(wakeOnlyRes.detected)
        assertEquals("", wakeOnlyRes.remainingCommand)

        val normalCommand = WakeWordDetector.check("Open settings right now")
        assertFalse(normalCommand.detected)
        assertEquals("Open settings right now", normalCommand.remainingCommand)
    }
}
