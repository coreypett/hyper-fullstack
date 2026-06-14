package org.coreypett.fullstack.network

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.coreypett.fullstack.market.candle.dto.CandleSubscriptionDto
import org.coreypett.fullstack.market.price.dto.AllMidsSubscriptionDto
import org.coreypett.fullstack.orderbook.dto.L2BookSubscriptionDto

class HyperliquidWebSocketClientTest {
    @Test
    fun encodesL2BookSubscribeRequest() {
        val json = encodeSubscribeRequest(
            subscription = L2BookSubscriptionDto(
                type = "l2Book",
                coin = "BTC",
                nSigFigs = 5,
            ),
            serializer = L2BookSubscriptionDto.serializer(),
        ).asJsonObject()
        val subscription = json.getValue("subscription").jsonObject

        assertEquals("subscribe", json.getValue("method").jsonPrimitive.content)
        assertEquals("l2Book", subscription.getValue("type").jsonPrimitive.content)
        assertEquals("BTC", subscription.getValue("coin").jsonPrimitive.content)
        assertEquals("5", subscription.getValue("nSigFigs").jsonPrimitive.content)
    }

    @Test
    fun encodesL2BookUnsubscribeRequest() {
        val json = encodeUnsubscribeRequest(
            subscription = L2BookSubscriptionDto(
                type = "l2Book",
                coin = "BTC",
                nSigFigs = 5,
            ),
            serializer = L2BookSubscriptionDto.serializer(),
        ).asJsonObject()
        val subscription = json.getValue("subscription").jsonObject

        assertEquals("unsubscribe", json.getValue("method").jsonPrimitive.content)
        assertEquals("l2Book", subscription.getValue("type").jsonPrimitive.content)
        assertEquals("BTC", subscription.getValue("coin").jsonPrimitive.content)
        assertEquals("5", subscription.getValue("nSigFigs").jsonPrimitive.content)
    }

    @Test
    fun encodesCandleSubscribeRequest() {
        val json = encodeSubscribeRequest(
            subscription = CandleSubscriptionDto(
                type = "candle",
                coin = "ETH",
                interval = "1h",
            ),
            serializer = CandleSubscriptionDto.serializer(),
        ).asJsonObject()
        val subscription = json.getValue("subscription").jsonObject

        assertEquals("subscribe", json.getValue("method").jsonPrimitive.content)
        assertEquals("candle", subscription.getValue("type").jsonPrimitive.content)
        assertEquals("ETH", subscription.getValue("coin").jsonPrimitive.content)
        assertEquals("1h", subscription.getValue("interval").jsonPrimitive.content)
    }

    @Test
    fun encodesAllMidsSubscribeRequestWithoutNullDex() {
        val json = encodeSubscribeRequest(
            subscription = AllMidsSubscriptionDto(type = "allMids"),
            serializer = AllMidsSubscriptionDto.serializer(),
        ).asJsonObject()
        val subscription = json.getValue("subscription").jsonObject

        assertEquals("subscribe", json.getValue("method").jsonPrimitive.content)
        assertEquals("allMids", subscription.getValue("type").jsonPrimitive.content)
        assertFalse("dex" in subscription)
    }

    @Test
    fun capsReconnectDelay() {
        val policy = HyperliquidWebSocketRetryPolicy(
            initialDelayMillis = 500,
            maxDelayMillis = 2_000,
        )

        assertEquals(500, policy.delayMillis(attempt = 1, cause = null))
        assertEquals(1_000, policy.delayMillis(attempt = 2, cause = null))
        assertEquals(2_000, policy.delayMillis(attempt = 3, cause = null))
        assertEquals(2_000, policy.delayMillis(attempt = 4, cause = null))
    }

    private fun String.asJsonObject() = HyperliquidJson.parseToJsonElement(this).jsonObject
}
