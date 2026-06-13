package org.coreypett.fullstack.marketdata.candle

import kotlin.test.Test
import kotlin.test.assertEquals
import org.coreypett.fullstack.market.model.MarketSymbol
import org.coreypett.fullstack.marketdata.candle.model.CandleInterval
import org.coreypett.fullstack.marketdata.candle.model.CandleSelection
import org.coreypett.fullstack.marketdata.candle.service.CandleService

class CandleServiceParserTest {
    @Test
    fun parsesSingleCandleEnvelope() {
        val service = CandleService.Impl()
        val bars = service.parseCandles(
            text = """
                {
                  "channel": "candle",
                  "data": {
                    "t": 1710000000000,
                    "T": 1710003599999,
                    "s": "BTC",
                    "i": "1h",
                    "o": 69000.0,
                    "c": 69125.5,
                    "h": 69200.0,
                    "l": 68850.0,
                    "v": 12.75,
                    "n": 42
                  }
                }
            """.trimIndent(),
            selection = CandleSelection(
                market = MarketSymbol.BTC,
                interval = CandleInterval.OneHour,
            ),
        )

        assertEquals(1, bars.size)
        val bar = bars.single()
        assertEquals(MarketSymbol.BTC, bar.market)
        assertEquals(CandleInterval.OneHour, bar.interval)
        assertEquals(1710000000000, bar.openTimeMillis)
        assertEquals(1710003599999, bar.closeTimeMillis)
        assertEquals(69000.0, bar.open)
        assertEquals(69125.5, bar.close)
        assertEquals(69200.0, bar.high)
        assertEquals(68850.0, bar.low)
        assertEquals(12.75, bar.volume)
        assertEquals(42, bar.tradeCount)
    }

    @Test
    fun parsesCandleArrayEnvelopeAndFiltersSelection() {
        val service = CandleService.Impl()
        val bars = service.parseCandles(
            text = """
                {
                  "channel": "candle",
                  "data": [
                    {
                      "t": 1710000000000,
                      "T": 1710003599999,
                      "s": "ETH",
                      "i": "1h",
                      "o": 3500.0,
                      "c": 3510.0,
                      "h": 3525.0,
                      "l": 3490.0,
                      "v": 9.0,
                      "n": 7
                    },
                    {
                      "t": 1710000000000,
                      "T": 1710003599999,
                      "s": "BTC",
                      "i": "1h",
                      "o": 69000.0,
                      "c": 69100.0,
                      "h": 69200.0,
                      "l": 68850.0,
                      "v": 12.0,
                      "n": 40
                    }
                  ]
                }
            """.trimIndent(),
            selection = CandleSelection(
                market = MarketSymbol.BTC,
                interval = CandleInterval.OneHour,
            ),
        )

        assertEquals(1, bars.size)
        assertEquals(MarketSymbol.BTC, bars.single().market)
        assertEquals(69100.0, bars.single().close)
    }
}
