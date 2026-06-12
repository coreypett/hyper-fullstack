package org.coreypett.fullstack.market

internal object MarketNumberFormatter {
    fun price(value: Double): String = formatMarketPrice(value)
    fun size(value: Double): String = formatMarketSize(value)
}

internal expect fun formatMarketPrice(value: Double): String
internal expect fun formatMarketSize(value: Double): String
