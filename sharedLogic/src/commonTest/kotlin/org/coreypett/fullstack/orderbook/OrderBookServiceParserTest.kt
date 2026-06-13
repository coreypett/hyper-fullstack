package org.coreypett.fullstack.orderbook

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.KSerializer
import org.coreypett.fullstack.market.model.MarketSymbol
import org.coreypett.fullstack.network.HyperliquidWebSocketClient
import org.coreypett.fullstack.network.HyperliquidWebSocketEvent
import org.coreypett.fullstack.orderbook.model.LevelChange
import org.coreypett.fullstack.orderbook.model.OrderBookSelection
import org.coreypett.fullstack.orderbook.model.OrderBookSide
import org.coreypett.fullstack.orderbook.model.PricePrecision
import org.coreypett.fullstack.orderbook.service.OrderBookService

class OrderBookServiceParserTest {
    @Test
    fun parsesCapturedL2BookFrame() = runBlocking {
        val service = OrderBookService.Impl(
            webSocketClient = FakeWebSocketClient(
                l2BookFrame(
                    coin = "BTC",
                    levels = """
                        [
                          [
                            {"px": "69125.5", "sz": "1.25", "n": 3},
                            {"px": "69124.0", "sz": "0.50", "n": 1},
                            {"px": "invalid", "sz": "9.0", "n": 1}
                          ],
                          [
                            {"px": "69126.0", "sz": "2.0", "n": 4},
                            {"px": "69127.5", "sz": "1.0", "n": 2}
                          ]
                        ]
                    """.trimIndent(),
                ),
            ),
        )

        val snapshot = service.snapshots(
            OrderBookSelection(
                market = MarketSymbol.BTC,
                precision = PricePrecision.Five,
            ),
        ).first()

        assertEquals(MarketSymbol.BTC, snapshot.market)
        assertEquals(PricePrecision.Five, snapshot.precision)
        assertEquals(1710000000000, snapshot.timeMillis)

        assertEquals(2, snapshot.bids.size)
        assertEquals(OrderBookSide.Bid, snapshot.bids[0].side)
        assertEquals(69125.5, snapshot.bids[0].price)
        assertEquals(1.25, snapshot.bids[0].size)
        assertEquals(3, snapshot.bids[0].orderCount)
        assertEquals(1.0f, snapshot.bids[0].depthFraction)
        assertEquals(LevelChange.None, snapshot.bids[0].change)
        assertEquals(69124.0, snapshot.bids[1].price)
        assertEquals(0.4f, snapshot.bids[1].depthFraction)

        assertEquals(2, snapshot.asks.size)
        assertEquals(OrderBookSide.Ask, snapshot.asks[0].side)
        assertEquals(69126.0, snapshot.asks[0].price)
        assertEquals(2.0, snapshot.asks[0].size)
        assertEquals(4, snapshot.asks[0].orderCount)
        assertEquals(1.0f, snapshot.asks[0].depthFraction)
        assertEquals(69127.5, snapshot.asks[1].price)
        assertEquals(0.5f, snapshot.asks[1].depthFraction)

        assertNotNull(snapshot.spread)
        assertEquals(0.5, snapshot.spread)
    }

    @Test
    fun filtersWrongChannelAndCoin() = runBlocking {
        val service = OrderBookService.Impl(
            webSocketClient = FakeWebSocketClient(
                """
                    {
                      "channel": "allMids",
                      "data": {"mids": {"BTC": "69125.5"}}
                    }
                """.trimIndent(),
                l2BookFrame(coin = "ETH"),
                l2BookFrame(coin = "BTC"),
            ),
        )

        val snapshot = service.snapshots(OrderBookSelection(market = MarketSymbol.BTC)).first()

        assertEquals(MarketSymbol.BTC, snapshot.market)
        assertEquals(69125.5, snapshot.bestBid?.price)
    }

    @Test
    fun tracksLevelSizeChangesAcrossFrames() = runBlocking {
        val service = OrderBookService.Impl(
            webSocketClient = FakeWebSocketClient(
                l2BookFrame(
                    coin = "BTC",
                    levels = """
                        [
                          [
                            {"px": "69125.5", "sz": "1.25", "n": 3}
                          ],
                          [
                            {"px": "69126.0", "sz": "2.0", "n": 4}
                          ]
                        ]
                    """.trimIndent(),
                ),
                l2BookFrame(
                    coin = "BTC",
                    levels = """
                        [
                          [
                            {"px": "69125.5", "sz": "1.50", "n": 3}
                          ],
                          [
                            {"px": "69126.0", "sz": "1.75", "n": 4}
                          ]
                        ]
                    """.trimIndent(),
                ),
            ),
        )

        val snapshots = service.snapshots(OrderBookSelection(market = MarketSymbol.BTC))
            .take(2)
            .toList()

        assertEquals(LevelChange.None, snapshots[0].bids.single().change)
        assertEquals(LevelChange.None, snapshots[0].asks.single().change)
        assertEquals(LevelChange.Up, snapshots[1].bids.single().change)
        assertEquals(LevelChange.Down, snapshots[1].asks.single().change)
    }

    private fun l2BookFrame(
        coin: String,
        levels: String = """
            [
              [
                {"px": "69125.5", "sz": "1.25", "n": 3}
              ],
              [
                {"px": "69126.0", "sz": "2.0", "n": 4}
              ]
            ]
        """.trimIndent(),
    ): String =
        """
            {
              "channel": "l2Book",
              "data": {
                "coin": "$coin",
                "time": 1710000000000,
                "levels": $levels
              }
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
