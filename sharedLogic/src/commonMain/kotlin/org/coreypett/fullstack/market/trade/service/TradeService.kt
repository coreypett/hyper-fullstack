package org.coreypett.fullstack.market.trade.service

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.flow.flow
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.decodeFromJsonElement
import org.coreypett.fullstack.market.trade.dto.TradesSubscriptionDto
import org.coreypett.fullstack.market.trade.dto.WsTradeDto
import org.coreypett.fullstack.market.trade.model.Trade
import org.coreypett.fullstack.market.trade.model.TradeSelection
import org.coreypett.fullstack.market.trade.model.TradeSide
import org.coreypett.fullstack.network.HyperliquidJson
import org.coreypett.fullstack.network.HyperliquidWebSocketClient
import org.coreypett.fullstack.network.HyperliquidWebSocketEnvelopeDto
import org.coreypett.fullstack.network.HyperliquidWebSocketEvent
import org.coreypett.fullstack.network.RealtimeFeedEvent

/**
 * Shared recent trades stream backed by Hyperliquid's `trades` WebSocket feed.
 *
 * Docs: https://hyperliquid.gitbook.io/hyperliquid-docs/for-developers/api/websocket/subscriptions
 */
internal interface TradeService {
    fun trades(selection: TradeSelection): Flow<List<Trade>> =
        tradeEvents(selection).mapNotNull { event ->
            (event as? RealtimeFeedEvent.Live)?.value
        }

    fun tradeEvents(selection: TradeSelection): Flow<RealtimeFeedEvent<List<Trade>>>

    class Impl(
        private val webSocketClient: HyperliquidWebSocketClient = HyperliquidWebSocketClient.Shared,
        private val json: Json = HyperliquidJson,
    ) : TradeService {
        override fun tradeEvents(selection: TradeSelection): Flow<RealtimeFeedEvent<List<Trade>>> = flow {
            webSocketClient.subscribeEvents(
                subscription = tradesSubscription(selection),
                serializer = TradesSubscriptionDto.serializer(),
            ).collect { event ->
                when (event) {
                    is HyperliquidWebSocketEvent.Reconnecting -> emit(event.toRealtimeFeedEvent())
                    is HyperliquidWebSocketEvent.Text -> {
                        val trades = parseTrades(
                            text = event.text,
                            selection = selection,
                        )
                        if (trades.isNotEmpty()) emit(RealtimeFeedEvent.Live(trades))
                    }
                }
            }
        }

        private fun parseTrades(
            text: String,
            selection: TradeSelection,
        ): List<Trade> {
            val envelope = json.decodeFromString(HyperliquidWebSocketEnvelopeDto.serializer(), text)
            if (envelope.channel != TradesSubscriptionType) return emptyList()

            val data = envelope.data ?: return emptyList()
            return json.decodeFromJsonElement(ListSerializer(WsTradeDto.serializer()), data)
                .asSequence()
                .filter { trade -> trade.coin == selection.market.wireName }
                .map { trade -> trade.toModel(selection) }
                .toList()
        }

        private fun tradesSubscription(selection: TradeSelection): TradesSubscriptionDto =
            TradesSubscriptionDto(
                type = TradesSubscriptionType,
                coin = selection.market.wireName,
            )
    }
}

private fun WsTradeDto.toModel(selection: TradeSelection): Trade =
    Trade(
        market = selection.market,
        side = side.toTradeSide(),
        price = price,
        size = size,
        transactionHash = hash,
        timeMillis = time,
        tradeId = tid,
        buyer = users.getOrNull(0),
        seller = users.getOrNull(1),
    )

private fun String.toTradeSide(): TradeSide = when (this) {
    "B" -> TradeSide.Buy
    "A" -> TradeSide.Sell
    else -> TradeSide.Unknown
}

private fun HyperliquidWebSocketEvent.Reconnecting.toRealtimeFeedEvent(): RealtimeFeedEvent.Reconnecting =
    RealtimeFeedEvent.Reconnecting(
        attempt = attempt,
        delayMillis = delayMillis,
        reason = reason,
    )

private const val TradesSubscriptionType = "trades"
