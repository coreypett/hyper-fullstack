package org.coreypett.fullstack.market.price.service

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.flow.flow
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.decodeFromJsonElement
import org.coreypett.fullstack.market.model.MarketSymbol
import org.coreypett.fullstack.market.price.dto.AllMidsDataDto
import org.coreypett.fullstack.market.price.dto.AllMidsSubscriptionDto
import org.coreypett.fullstack.market.price.model.LivePrice
import org.coreypett.fullstack.network.HyperliquidJson
import org.coreypett.fullstack.network.HyperliquidWebSocketClient
import org.coreypett.fullstack.network.HyperliquidWebSocketEnvelopeDto
import org.coreypett.fullstack.network.HyperliquidWebSocketEvent
import org.coreypett.fullstack.network.RealtimeFeedEvent

/**
 * Shared live price service backed by Hyperliquid's `allMids` WebSocket feed.
 */
internal interface LivePriceService {
    fun prices(market: MarketSymbol): Flow<LivePrice> =
        priceEvents(market).mapNotNull { event ->
            (event as? RealtimeFeedEvent.Live)?.value
        }

    fun priceEvents(market: MarketSymbol): Flow<RealtimeFeedEvent<LivePrice>>

    class Impl(
        private val webSocketClient: HyperliquidWebSocketClient = HyperliquidWebSocketClient.Shared,
        private val json: Json = HyperliquidJson,
    ) : LivePriceService {
        override fun priceEvents(market: MarketSymbol): Flow<RealtimeFeedEvent<LivePrice>> = flow {
            webSocketClient.subscribeEvents(
                subscription = AllMidsSubscriptionDto(type = AllMidsSubscriptionType),
                serializer = AllMidsSubscriptionDto.serializer(),
            ).collect { event ->
                when (event) {
                    is HyperliquidWebSocketEvent.Reconnecting -> emit(event.toRealtimeFeedEvent())
                    is HyperliquidWebSocketEvent.Text -> {
                        parsePrice(event.text, market)?.let { price ->
                            emit(RealtimeFeedEvent.Live(price))
                        }
                    }
                }
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

private fun HyperliquidWebSocketEvent.Reconnecting.toRealtimeFeedEvent(): RealtimeFeedEvent.Reconnecting =
    RealtimeFeedEvent.Reconnecting(
        attempt = attempt,
        delayMillis = delayMillis,
        reason = reason,
    )

private const val AllMidsSubscriptionType = "allMids"
