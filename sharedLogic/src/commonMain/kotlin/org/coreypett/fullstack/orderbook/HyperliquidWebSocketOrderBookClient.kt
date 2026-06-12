package org.coreypett.fullstack.orderbook

import io.ktor.client.HttpClient
import io.ktor.client.plugins.websocket.WebSockets
import io.ktor.client.plugins.websocket.webSocket
import io.ktor.websocket.Frame
import io.ktor.websocket.readText
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put

class HyperliquidWebSocketOrderBookClient(
    private val httpClient: HttpClient = platformHttpClient(),
    private val json: Json = Json {
        ignoreUnknownKeys = true
        explicitNulls = false
    },
) : HyperliquidOrderBookClient {
    override fun snapshots(selection: OrderBookSelection): Flow<OrderBookSnapshot> = flow {
        val previousSizes = mutableMapOf<String, Double>()

        httpClient.webSocket(urlString = HyperliquidWebSocketUrl) {
            send(Frame.Text(subscribeMessage(selection)))

            for (frame in incoming) {
                val textFrame = frame as? Frame.Text ?: continue
                val snapshot = parseSnapshot(
                    text = textFrame.readText(),
                    selection = selection,
                    previousSizes = previousSizes,
                ) ?: continue
                emit(snapshot)
            }
        }
    }

    private fun parseSnapshot(
        text: String,
        selection: OrderBookSelection,
        previousSizes: MutableMap<String, Double>,
    ): OrderBookSnapshot? {
        val root = json.parseToJsonElement(text).jsonObject
        if (root["channel"]?.jsonPrimitive?.contentOrNull != "l2Book") return null

        val data = root["data"]?.jsonObject ?: return null
        val coin = data["coin"]?.jsonPrimitive?.contentOrNull
        if (coin != selection.market.wireName) return null

        val levels = data["levels"]?.jsonArray ?: return null
        val bids = parseSide(
            rawLevels = levels.getOrNull(0) as? JsonArray ?: return null,
            side = OrderBookSide.Bid,
            previousSizes = previousSizes,
        ).sortedByDescending(OrderBookLevel::price)

        val asks = parseSide(
            rawLevels = levels.getOrNull(1) as? JsonArray ?: return null,
            side = OrderBookSide.Ask,
            previousSizes = previousSizes,
        ).sortedBy(OrderBookLevel::price)

        return OrderBookSnapshot(
            market = selection.market,
            precision = selection.precision,
            timeMillis = data["time"]?.jsonPrimitive?.contentOrNull?.toLongOrNull() ?: 0L,
            bids = bids,
            asks = asks,
        )
    }

    private fun parseSide(
        rawLevels: JsonArray,
        side: OrderBookSide,
        previousSizes: MutableMap<String, Double>,
    ): List<OrderBookLevel> {
        val parsed = rawLevels
            .mapNotNull { rawLevel ->
                val level = rawLevel as? JsonObject ?: return@mapNotNull null
                val price = level.stringDouble("px") ?: return@mapNotNull null
                val size = level.stringDouble("sz") ?: return@mapNotNull null
                val orderCount = level["n"]?.jsonPrimitive?.intOrNull ?: 0
                RawLevel(price = price, size = size, orderCount = orderCount)
            }
            .take(MaxVisibleLevels)

        val maxSize = parsed.maxOfOrNull(RawLevel::size)?.takeIf { it > 0.0 } ?: 1.0
        return parsed.map { level ->
            val key = "${side.name}:${level.price}"
            val previousSize = previousSizes[key]
            previousSizes[key] = level.size

            OrderBookLevel(
                side = side,
                price = level.price,
                size = level.size,
                orderCount = level.orderCount,
                depthFraction = (level.size / maxSize).toFloat().coerceIn(0f, 1f),
                change = when {
                    previousSize == null -> LevelChange.None
                    level.size > previousSize -> LevelChange.Up
                    level.size < previousSize -> LevelChange.Down
                    else -> LevelChange.None
                },
            )
        }
    }

    private fun JsonObject.stringDouble(name: String): Double? =
        this[name]?.jsonPrimitive?.contentOrNull?.toDoubleOrNull()
            ?: this[name]?.jsonPrimitive?.doubleOrNull

    private data class RawLevel(
        val price: Double,
        val size: Double,
        val orderCount: Int,
    )

    private fun subscribeMessage(selection: OrderBookSelection): String =
        buildJsonObject {
            put("method", "subscribe")
            put(
                "subscription",
                buildJsonObject {
                    put("type", "l2Book")
                    put("coin", selection.market.wireName)
                    put("nSigFigs", selection.precision.nSigFigs)
                },
            )
        }.toString()
}

private const val HyperliquidWebSocketUrl = "wss://api.hyperliquid.xyz/ws"
private const val MaxVisibleLevels = 18
