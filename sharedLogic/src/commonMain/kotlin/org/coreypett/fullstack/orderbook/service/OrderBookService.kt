package org.coreypett.fullstack.orderbook.service

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.flow.flow
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.decodeFromJsonElement
import org.coreypett.fullstack.network.HyperliquidJson
import org.coreypett.fullstack.network.HyperliquidWebSocketEnvelopeDto
import org.coreypett.fullstack.network.HyperliquidWebSocketClient
import org.coreypett.fullstack.network.HyperliquidWebSocketEvent
import org.coreypett.fullstack.network.RealtimeFeedEvent
import org.coreypett.fullstack.orderbook.dto.L2BookDataDto
import org.coreypett.fullstack.orderbook.dto.L2BookLevelDto
import org.coreypett.fullstack.orderbook.dto.L2BookSubscriptionDto
import org.coreypett.fullstack.orderbook.model.OrderBookLevel
import org.coreypett.fullstack.orderbook.model.OrderBookSelection
import org.coreypett.fullstack.orderbook.model.OrderBookSide
import org.coreypett.fullstack.orderbook.model.OrderBookSnapshot
import org.coreypett.fullstack.orderbook.util.OrderBookLevelCalculator

/**
 * Shared order book stream service backed by Hyperliquid's `l2Book` WebSocket feed.
 *
 * Docs: https://hyperliquid.gitbook.io/hyperliquid-docs/for-developers/api/websocket/subscriptions
 */
internal interface OrderBookService {
    fun snapshots(selection: OrderBookSelection): Flow<OrderBookSnapshot> =
        snapshotEvents(selection).mapNotNull { event ->
            (event as? RealtimeFeedEvent.Live)?.value
        }

    fun snapshotEvents(selection: OrderBookSelection): Flow<RealtimeFeedEvent<OrderBookSnapshot>>

    class Impl(
        private val webSocketClient: HyperliquidWebSocketClient = HyperliquidWebSocketClient.Shared,
        private val json: Json = HyperliquidJson,
    ) : OrderBookService {
        override fun snapshotEvents(selection: OrderBookSelection): Flow<RealtimeFeedEvent<OrderBookSnapshot>> = flow {
            val previousSizes = PreviousBookSizes()

            webSocketClient.subscribeEvents(
                subscription = l2BookSubscription(selection),
                serializer = L2BookSubscriptionDto.serializer(),
            ).collect { event ->
                when (event) {
                    is HyperliquidWebSocketEvent.Reconnecting -> emit(event.toRealtimeFeedEvent())
                    is HyperliquidWebSocketEvent.Text -> {
                        val snapshot = parseSnapshot(
                            text = event.text,
                            selection = selection,
                            previousSizes = previousSizes,
                        ) ?: return@collect
                        emit(RealtimeFeedEvent.Live(snapshot))
                    }
                }
            }
        }

        private fun parseSnapshot(
            text: String,
            selection: OrderBookSelection,
            previousSizes: PreviousBookSizes,
        ): OrderBookSnapshot? {
            val envelope = json.decodeFromString(HyperliquidWebSocketEnvelopeDto.serializer(), text)
            if (envelope.channel != "l2Book") return null

            val data = envelope.data?.let { json.decodeFromJsonElement(L2BookDataDto.serializer(), it) } ?: return null
            if (data.coin != selection.market.wireName) return null

            val bids = parseSide(
                levels = data.levels.getOrNull(0) ?: return null,
                side = OrderBookSide.Bid,
                previousBookSizes = previousSizes,
            ).sortedByDescending(OrderBookLevel::price)

            val asks = parseSide(
                levels = data.levels.getOrNull(1) ?: return null,
                side = OrderBookSide.Ask,
                previousBookSizes = previousSizes,
            ).sortedBy(OrderBookLevel::price)

            return OrderBookSnapshot(
                market = selection.market,
                precision = selection.precision,
                timeMillis = data.time,
                bids = bids,
                asks = asks,
            )
        }

        private fun parseSide(
            levels: List<L2BookLevelDto>,
            side: OrderBookSide,
            previousBookSizes: PreviousBookSizes,
        ): List<OrderBookLevel> {
            val parsed = levels
                .asSequence()
                .mapNotNull { level ->
                    val price = level.priceValue ?: return@mapNotNull null
                    val size = level.sizeValue ?: return@mapNotNull null
                    ParsedLevel(price = price, size = size, orderCount = level.orderCount)
                }
                .take(MaxVisibleLevels)
                .toList()

            val maxSize = parsed.maxOfOrNull(ParsedLevel::size)?.takeIf { it > 0.0 } ?: 1.0
            val previousSizes = previousBookSizes.forSide(side)
            previousSizes.keys.retainAll(parsed.mapTo(mutableSetOf(), ParsedLevel::price))

            return parsed.map { level ->
                val previousSize = previousSizes.put(level.price, level.size)

                OrderBookLevel(
                    side = side,
                    price = level.price,
                    size = level.size,
                    orderCount = level.orderCount,
                    depthFraction = OrderBookLevelCalculator.depthFraction(level.size, maxSize),
                    sizeChangeFraction = OrderBookLevelCalculator.changeFraction(previousSize, level.size),
                    change = OrderBookLevelCalculator.change(previousSize, level.size),
                )
            }
        }

        private data class ParsedLevel(
            val price: Double,
            val size: Double,
            val orderCount: Int,
        )

        private class PreviousBookSizes {
            private val bids = mutableMapOf<Double, Double>()
            private val asks = mutableMapOf<Double, Double>()

            fun forSide(side: OrderBookSide): MutableMap<Double, Double> =
                if (side == OrderBookSide.Bid) bids else asks
        }

        private fun l2BookSubscription(selection: OrderBookSelection): L2BookSubscriptionDto =
            L2BookSubscriptionDto(
                type = L2BookSubscriptionType,
                coin = selection.market.wireName,
                nSigFigs = selection.precision.nSigFigs,
            )
    }
}

private fun HyperliquidWebSocketEvent.Reconnecting.toRealtimeFeedEvent(): RealtimeFeedEvent.Reconnecting =
    RealtimeFeedEvent.Reconnecting(
        attempt = attempt,
        delayMillis = delayMillis,
        reason = reason,
    )

private const val MaxVisibleLevels = 18
private const val L2BookSubscriptionType = "l2Book"
