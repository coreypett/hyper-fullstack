package org.coreypett.fullstack.market.candle.service

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.flow
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.decodeFromJsonElement
import org.coreypett.fullstack.market.candle.dto.CandleDto
import org.coreypett.fullstack.market.candle.dto.CandleSubscriptionDto
import org.coreypett.fullstack.market.candle.model.CandleBar
import org.coreypett.fullstack.market.candle.model.CandleSelection
import org.coreypett.fullstack.network.HyperliquidJson
import org.coreypett.fullstack.network.HyperliquidWebSocketClient
import org.coreypett.fullstack.network.HyperliquidWebSocketEnvelopeDto

/**
 * Shared candle stream service backed by Hyperliquid's `candle` WebSocket feed.
 *
 * Docs: https://hyperliquid.gitbook.io/hyperliquid-docs/for-developers/api/websocket/subscriptions
 */
internal interface CandleService {
    fun candles(selection: CandleSelection): Flow<CandleBar>

    class Impl(
        private val webSocketClient: HyperliquidWebSocketClient = HyperliquidWebSocketClient.Impl(),
        private val json: Json = HyperliquidJson,
    ) : CandleService {
        override fun candles(selection: CandleSelection): Flow<CandleBar> = flow {
            webSocketClient.subscribe(
                subscription = candleSubscription(selection),
                serializer = CandleSubscriptionDto.serializer(),
            ).collect { text ->
                parseCandles(text, selection).forEach { emit(it) }
            }
        }

        internal fun parseCandles(text: String, selection: CandleSelection): List<CandleBar> {
            val envelope = json.decodeFromString(HyperliquidWebSocketEnvelopeDto.serializer(), text)
            if (envelope.channel != CandleSubscriptionType) return emptyList()
            val data = envelope.data ?: return emptyList()

            val candles = if (data is JsonArray) {
                json.decodeFromJsonElement(ListSerializer(CandleDto.serializer()), data)
            } else {
                listOf(json.decodeFromJsonElement(CandleDto.serializer(), data))
            }

            return candles.mapNotNull { candle ->
                candle.toCandleBar(selection)
            }
        }

        private fun CandleDto.toCandleBar(selection: CandleSelection): CandleBar? {
            if (coin != selection.market.wireName) return null
            if (interval != selection.interval.wireName) return null

            return CandleBar(
                market = selection.market,
                interval = selection.interval,
                openTimeMillis = openTimeMillis,
                closeTimeMillis = closeTimeMillis,
                open = open,
                close = close,
                high = high,
                low = low,
                volume = volume,
                tradeCount = tradeCount,
            )
        }

        private fun candleSubscription(selection: CandleSelection): CandleSubscriptionDto =
            CandleSubscriptionDto(
                type = CandleSubscriptionType,
                coin = selection.market.wireName,
                interval = selection.interval.wireName,
            )
    }
}

private const val CandleSubscriptionType = "candle"
