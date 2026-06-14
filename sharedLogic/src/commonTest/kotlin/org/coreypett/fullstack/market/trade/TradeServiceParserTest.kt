package org.coreypett.fullstack.market.trade

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.KSerializer
import org.coreypett.fullstack.market.model.MarketSymbol
import org.coreypett.fullstack.market.trade.model.TradeSelection
import org.coreypett.fullstack.market.trade.model.TradeSide
import org.coreypett.fullstack.market.trade.service.TradeService
import org.coreypett.fullstack.network.HyperliquidWebSocketClient
import org.coreypett.fullstack.network.HyperliquidWebSocketEvent

class TradeServiceParserTest {
    @Test
    fun parsesCapturedTradesFrame() = runBlocking {
        val service = TradeService.Impl(
            webSocketClient = FakeWebSocketClient(
                tradesFrame(
                    coin = "BTC",
                    trades = """
                        [
                          {
                            "coin": "BTC",
                            "side": "B",
                            "px": "69125.5",
                            "sz": "0.42",
                            "hash": "0xabc",
                            "time": 1710000000000,
                            "tid": 42,
                            "users": ["0xbuyer", "0xseller"]
                          },
                          {
                            "coin": "BTC",
                            "side": "A",
                            "px": "69124.0",
                            "sz": "0.25",
                            "hash": "0xdef",
                            "time": 1710000000100,
                            "tid": 43,
                            "users": ["0xbuyer2", "0xseller2"]
                          }
                        ]
                    """.trimIndent(),
                ),
            ),
        )

        val trades = service.trades(TradeSelection(market = MarketSymbol.BTC)).first()

        assertEquals(2, trades.size)
        assertEquals(MarketSymbol.BTC, trades[0].market)
        assertEquals(TradeSide.Buy, trades[0].side)
        assertEquals(69125.5, trades[0].price)
        assertEquals(0.42, trades[0].size)
        assertEquals("0xabc", trades[0].transactionHash)
        assertEquals(1710000000000, trades[0].timeMillis)
        assertEquals(42, trades[0].tradeId)
        assertEquals("0xbuyer", trades[0].buyer)
        assertEquals("0xseller", trades[0].seller)
        assertEquals(TradeSide.Sell, trades[1].side)
    }

    @Test
    fun filtersWrongChannelAndCoin() = runBlocking {
        val service = TradeService.Impl(
            webSocketClient = FakeWebSocketClient(
                """
                    {
                      "channel": "l2Book",
                      "data": {"coin": "BTC", "levels": [[], []], "time": 1710000000000}
                    }
                """.trimIndent(),
                tradesFrame(coin = "ETH"),
                tradesFrame(coin = "BTC"),
            ),
        )

        val trades = service.trades(TradeSelection(market = MarketSymbol.BTC)).first()

        assertEquals(1, trades.size)
        assertEquals(MarketSymbol.BTC, trades.single().market)
        assertEquals(69125.5, trades.single().price)
    }

    private fun tradesFrame(
        coin: String,
        trades: String = """
            [
              {
                "coin": "$coin",
                "side": "B",
                "px": "69125.5",
                "sz": "0.42",
                "hash": "0xabc",
                "time": 1710000000000,
                "tid": 42,
                "users": ["0xbuyer", "0xseller"]
              }
            ]
        """.trimIndent(),
    ): String =
        """
            {
              "channel": "trades",
              "data": $trades
            }
        """.trimIndent()

    private class FakeWebSocketClient(
        private vararg val texts: String,
    ) : HyperliquidWebSocketClient {
        override fun <Subscription : Any> subscribeEvents(
            subscription: Subscription,
            serializer: KSerializer<Subscription>,
        ): Flow<HyperliquidWebSocketEvent> = flow {
            texts.forEach { text ->
                emit(HyperliquidWebSocketEvent.Text(text))
            }
        }
    }
}
