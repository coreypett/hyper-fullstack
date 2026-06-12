package org.coreypett.fullstack.market

import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.Locale

private val symbols = DecimalFormatSymbols(Locale.US)

internal actual fun formatMarketPrice(value: Double): String = value.formatWithDecimals(
    decimals = when {
        value >= 10_000.0 -> 0
        value >= 1_000.0 -> 1
        else -> 2
    },
)

internal actual fun formatMarketSize(value: Double): String = when {
    value >= 1_000.0 -> "${(value / 1_000.0).formatWithDecimals(2)}K"
    value >= 1.0 -> value.formatWithDecimals(4)
    else -> value.formatWithDecimals(6)
}

private fun Double.formatWithDecimals(decimals: Int): String {
    val formatter = DecimalFormat().apply {
        decimalFormatSymbols = symbols
        isGroupingUsed = false
        minimumFractionDigits = decimals
        maximumFractionDigits = decimals
    }
    return formatter.format(this)
}
