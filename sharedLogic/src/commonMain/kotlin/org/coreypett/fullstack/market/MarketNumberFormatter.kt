package org.coreypett.fullstack.market

internal object MarketNumberFormatter {
    fun price(value: Double): String = platformFormatMarketPrice(value)

    fun size(value: Double): String = platformFormatMarketSize(value)
}

internal expect fun platformFormatMarketPrice(value: Double): String

internal expect fun platformFormatMarketSize(value: Double): String
