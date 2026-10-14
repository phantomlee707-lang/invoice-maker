package com.rainweaver.invoicemaker.domain

/**
 * Configurable, restart-safe invoice numbering. The persisted sequence lives in
 * settings; this is the pure logic that derives the next number from it, so it
 * stays reliable across app restarts and never reuses a number.
 */
object InvoiceNumbering {

    fun next(prefix: String, lastSequence: Long, padding: Int = 4): String {
        val seq = (lastSequence + 1).coerceAtLeast(1)
        return prefix + seq.toString().padStart(padding, '0')
    }

    /** Extract the numeric sequence from a formatted number, or null if it doesn't match the prefix. */
    fun parseSequence(number: String, prefix: String): Long? {
        if (!number.startsWith(prefix)) return null
        return number.substring(prefix.length).trim().toLongOrNull()
    }

    /** Invoice number as a timestamp: YYYYMMDDHHmmss (device local time). */
    fun timestamp(millis: Long): String =
        java.text.SimpleDateFormat("yyyyMMddHHmmss", java.util.Locale.US).format(java.util.Date(millis))
}
