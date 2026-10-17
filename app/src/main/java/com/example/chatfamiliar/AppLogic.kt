package com.example.chatfamiliar

import java.security.SecureRandom
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.Random
import java.util.TimeZone

// Keep shared business logic independent of Android and Firebase for JVM unit tests.
fun normalizeEmail(email: String): String = email.trim().lowercase()

const val INVITE_CODE_LENGTH = 6
private const val INVITE_CODE_ALPHABET = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789"
private val inviteCodePattern = Regex("^[A-Z0-9]{$INVITE_CODE_LENGTH}$")

fun generateInviteCode(random: Random = SecureRandom()): String =
    buildString(INVITE_CODE_LENGTH) {
        repeat(INVITE_CODE_LENGTH) {
            append(INVITE_CODE_ALPHABET[random.nextInt(INVITE_CODE_ALPHABET.length)])
        }
    }

fun isValidInviteCode(code: String): Boolean = inviteCodePattern.matches(code)

enum class UserRole {
    ADMIN,
    USUARIO;

    companion object {
        fun fromValue(value: String?): UserRole? = entries.firstOrNull { it.name == value }
    }
}

fun formatMessageTime(
    timestamp: Date?,
    locale: Locale = Locale.getDefault(),
    timeZone: TimeZone = TimeZone.getDefault()
): String {
    if (timestamp == null) return "--:--"
    return SimpleDateFormat("HH:mm", locale).apply {
        this.timeZone = timeZone
    }.format(timestamp)
}
