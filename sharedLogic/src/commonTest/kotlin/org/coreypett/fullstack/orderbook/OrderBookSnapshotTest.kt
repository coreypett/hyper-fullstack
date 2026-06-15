package org.coreypett.fullstack.orderbook

import org.coreypett.fullstack.market.model.MarketSymbol
import org.coreypett.fullstack.orderbook.model.LevelChange
import org.coreypett.fullstack.orderbook.model.OrderBookLevel
import org.coreypett.fullstack.orderbook.model.OrderBookSide
import org.coreypett.fullstack.orderbook.model.OrderBookSnapshot
import org.coreypett.fullstack.orderbook.model.PricePrecision
import kotlin.test.Test
import kotlin.test.assertEquals

class OrderBookSnapshotTest {
    @Test
    fun formatsSpreadPercentFromMidPrice() {
        val snapshot = OrderBookSnapshot(
            market = MarketSymbol.BTC,
            precision = PricePrecision.Two,
            timeMillis = 1L,
            bids = listOf(level(OrderBookSide.Bid, 99.0)),
            asks = listOf(level(OrderBookSide.Ask, 101.0)),
        )

        assertEquals(2.0, snapshot.spread)
        assertEquals(2.0, snapshot.spreadPercent)
        assertEquals("2.000%", snapshot.spreadPercentText)
    }

    private fun level(
        side: OrderBookSide,
        price: Double,
    ): OrderBookLevel = OrderBookLevel(
        side = side,
        price = price,
        size = 1.0,
        orderCount = 1,
        depthFraction = 1.0f,
        change = LevelChange.None,
    )
}
