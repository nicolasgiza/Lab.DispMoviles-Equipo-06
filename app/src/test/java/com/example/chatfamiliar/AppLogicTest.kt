package com.example.chatfamiliar

import java.util.Date
import java.util.Locale
import java.util.Random
import java.util.TimeZone
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AppLogicTest {
    @Test
    fun normalizeEmailTrimsWhitespaceAndLowercases() {
        assertEquals("user@example.com", normalizeEmail("  UsEr@Example.COM \n"))
    }

    @Test
    fun generatedInviteCodeHasSixAlphanumericCharacters() {
        val code = generateInviteCode(Random(1234L))

        assertEquals(INVITE_CODE_LENGTH, code.length)
        assertTrue(isValidInviteCode(code))
        assertTrue(code.all { it in 'A'..'Z' || it in '0'..'9' })
    }

    @Test
    fun inviteCodeValidationRejectsIncorrectLengthAndCharacters() {
        assertFalse(isValidInviteCode("ABCDE"))
        assertFalse(isValidInviteCode("ABC12!"))
        assertFalse(isValidInviteCode("abc123"))
    }

    @Test
    fun roleValuesMapToKnownRoles() {
        assertEquals(UserRole.ADMIN, UserRole.fromValue("ADMIN"))
        assertEquals(UserRole.USUARIO, UserRole.fromValue("USUARIO"))
        assertEquals(null, UserRole.fromValue(null))
        assertEquals(null, UserRole.fromValue("UNKNOWN"))
    }

    @Test
    fun messageTimestampFormatsAsTwentyFourHourHourAndMinute() {
        val timeZone = TimeZone.getTimeZone("UTC")
        val timestamp = Date(1_729_120_047_000L)

        assertEquals("23:07", formatMessageTime(timestamp, Locale.US, timeZone))
        assertEquals("--:--", formatMessageTime(null, Locale.US, timeZone))
    }
}
