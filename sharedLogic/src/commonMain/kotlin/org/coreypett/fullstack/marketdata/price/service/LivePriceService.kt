package org.coreypett.fullstack.marketdata.price.service

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.flow
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.decodeFromJsonElement
import org.coreypett.fullstack.market.model.MarketSymbol
import org.coreypett.fullstack.marketdata.price.dto.AllMidsDataDto
import org.coreypett.fullstack.marketdata.price.dto.AllMidsSubscriptionDto
import org.coreypett.fullstack.marketdata.price.model.LivePrice
import org.coreypett.fullstack.network.HyperliquidWebSocketClient
import org.coreypett.fullstack.network.HyperliquidWebSocketEnvelopeDto

/**
 * Shared live price service backed by Hyperliquid's `allMids` WebSocket feed.
 */
internal interface LivePriceService {
    fun prices(market: MarketSymbol): Flow<LivePrice>

    class Impl(
        private val webSocketClient: HyperliquidWebSocketClient = HyperliquidWebSocketClient.Impl(),
        private val json: Json = Json {
            ignoreUnknownKeys = true
            explicitNulls = false
        },
    ) : LivePriceService {
        override fun prices(market: MarketSymbol): Flow<LivePrice> = flow {
            webSocketClient.subscribe(
                subscription = AllMidsSubscriptionDto(type = AllMidsSubscriptionType),
                serializer = AllMidsSubscriptionDto.serializer(),
            ).collect { text ->
                parsePrice(text, market)?.let { emit(it) }
            }
        }

        internal fun parsePrice(text: String, market: MarketSymbol): LivePrice? {
            val envelope = json.decodeFromString(HyperliquidWebSocketEnvelopeDto.serializer(), text)
            if (envelope.channel != AllMidsSubscriptionType) return null

            val data = envelope.data?.let { json.decodeFromJsonElement(AllMidsDataDto.serializer(), it) } ?: return null
            val price = data.mids[market.wireName]?.toDoubleOrNull() ?: return null
            return LivePrice(market = market, price = price)
        }
    }
}

private const val AllMidsSubscriptionType = "allMids"
