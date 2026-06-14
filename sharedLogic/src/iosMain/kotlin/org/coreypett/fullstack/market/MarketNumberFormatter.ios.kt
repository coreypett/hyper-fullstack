package org.coreypett.fullstack.market

internal actual fun formatMarketPrice(value: Double): String = value.formatUsd()

internal actual fun formatMarketSize(value: Double): String = when {
    value >= 1_000.0 -> "${(value / 1_000.0).formatWithDecimals(2)}K"
    value >= 1.0 -> value.formatWithDecimals(4)
    else -> value.formatWithDecimals(6)
}

private fun Double.formatUsd(): String {
    val scale = 100L
    val roundedUnits = kotlin.math.round(kotlin.math.abs(this) * scale).toLong()
    val whole = roundedUnits / scale
    val fraction = roundedUnits % scale
    val prefix = if (this < 0.0) "-\$" else "\$"
    val wholeText = whole.groupedDigits()
    return if (fraction == 0L) {
        "$prefix$wholeText"
    } else {
        "$prefix$wholeText.${fraction.toString().padStart(2, '0')}"
    }
}

private fun Double.formatWithDecimals(decimals: Int, grouping: Boolean = false): String {
    val scale = pow10(decimals)
    val roundedUnits = kotlin.math.round(kotlin.math.abs(this) * scale).toLong()
    val whole = roundedUnits / scale
    val fraction = roundedUnits % scale
    val sign = if (this < 0.0) "-" else ""
    val wholeText = if (grouping) whole.groupedDigits() else whole.toString()
    val fractionText = fraction.toString().padStart(decimals, '0')
    return "$sign$wholeText.$fractionText"
}

private fun Long.groupedDigits(): String {
    val digits = toString()
    val firstGroupSize = digits.length % 3
    val result = StringBuilder(digits.length + digits.length / 3)
    var index = 0
    if (firstGroupSize > 0) {
        result.append(digits.substring(0, firstGroupSize))
        index = firstGroupSize
        if (index < digits.length) result.append(',')
    }
    while (index < digits.length) {
        result.append(digits.substring(index, index + 3))
        index += 3
        if (index < digits.length) result.append(',')
    }
    return result.toString()
}

private fun pow10(decimals: Int): Long {
    var result = 1L
    repeat(decimals) {
        result *= 10L
    }
    return result
}
