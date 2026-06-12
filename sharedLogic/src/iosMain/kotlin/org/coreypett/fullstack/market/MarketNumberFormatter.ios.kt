package org.coreypett.fullstack.market

import platform.Foundation.NSLocale
import platform.Foundation.NSNumber
import platform.Foundation.NSNumberFormatter
import platform.Foundation.NSNumberFormatterDecimalStyle
import platform.Foundation.localeWithLocaleIdentifier
import platform.Foundation.numberWithDouble

internal actual fun platformFormatMarketPrice(value: Double): String = value.formatWithDecimals(
    decimals = when {
        value >= 10_000.0 -> 0
        value >= 1_000.0 -> 1
        else -> 2
    },
)

internal actual fun platformFormatMarketSize(value: Double): String = when {
    value >= 1_000.0 -> "${(value / 1_000.0).formatWithDecimals(2)}K"
    value >= 1.0 -> value.formatWithDecimals(4)
    else -> value.formatWithDecimals(6)
}

private fun Double.formatWithDecimals(decimals: Int): String {
    val formatter = NSNumberFormatter().apply {
        numberStyle = NSNumberFormatterDecimalStyle
        locale = NSLocale.localeWithLocaleIdentifier("en_US_POSIX")
        usesGroupingSeparator = false
        minimumFractionDigits = decimals.toULong()
        maximumFractionDigits = decimals.toULong()
    }
    return formatter.stringFromNumber(NSNumber.numberWithDouble(this)) ?: toString()
}
