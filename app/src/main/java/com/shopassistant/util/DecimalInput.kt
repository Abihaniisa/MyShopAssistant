package com.shopassistant.util

import java.math.BigDecimal

/**
 * Keeps only digits and a single decimal point. A comma is treated as a decimal point.
 * Limits the integer part to 12 digits and the fraction to [maxFraction] digits.
 */
fun sanitizeDecimal(raw: String, maxFraction: Int = 4): String {
    val out = StringBuilder()
    var seenDot = false
    var intDigits = 0
    var fractionDigits = 0
    for (c in raw) {
        when {
            c in '0'..'9' -> {
                if (seenDot) {
                    if (fractionDigits < maxFraction) {
                        out.append(c)
                        fractionDigits++
                    }
                } else if (intDigits < 12) {
                    out.append(c)
                    intDigits++
                }
            }
            c == '.' || c == ',' -> if (!seenDot) {
                seenDot = true
                out.append('.')
            }
        }
    }
    return out.toString()
}

/** Parses sanitized input; returns null for empty or invalid text. */
fun parseDecimal(text: String): BigDecimal? {
    val trimmed = text.trim().trimEnd('.')
    if (trimmed.isEmpty()) return null
    return trimmed.toBigDecimalOrNull()
}
