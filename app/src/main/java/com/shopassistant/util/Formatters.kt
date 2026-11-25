package com.shopassistant.util

import java.math.BigDecimal
import java.math.RoundingMode
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.Locale

object Formatters {
    private val symbols = DecimalFormatSymbols(Locale.US)

    /** ₦1,250 / ₦4.17 / ₦0.0042. Whole amounts drop the decimals. */
    fun money(value: BigDecimal): String {
        val negative = value.signum() < 0
        val abs = value.abs()
        val small = abs.signum() != 0 && abs < BigDecimal.ONE
        val rounded = abs.setScale(if (small) 4 else 2, RoundingMode.HALF_UP)
        val whole = rounded.stripTrailingZeros().scale() <= 0
        val pattern = when {
            small -> "0.00##"
            whole -> "#,##0"
            else -> "#,##0.00"
        }
        val text = DecimalFormat(pattern, symbols).format(rounded)
        return (if (negative) "-" else "") + "₦" + text
    }

    /** Quantity with up to 4 decimals and no trailing zeros: 12.5, 4, 0.25. */
    fun quantity(value: BigDecimal): String =
        value.setScale(4, RoundingMode.HALF_UP).stripTrailingZeros().toPlainString()

    /** Plain editable text without exponent or trailing zeros. */
    fun plain(value: BigDecimal): String = value.stripTrailingZeros().toPlainString()

    fun percent(value: BigDecimal): String = value.stripTrailingZeros().toPlainString() + "%"
}
