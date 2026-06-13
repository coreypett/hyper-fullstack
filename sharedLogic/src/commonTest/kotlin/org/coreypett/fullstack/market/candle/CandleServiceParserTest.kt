package org.coreypett.fullstack.market.candle

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlinx.serialization.json.encodeToJsonElement
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.coreypett.fullstack.market.candle.dto.CandleSnapshotReqDto
import org.coreypett.fullstack.market.candle.dto.CandleSnapshotRequestDto
import org.coreypett.fullstack.market.candle.model.CandleInterval
import org.coreypett.fullstack.market.candle.model.CandleSelection
import org.coreypett.fullstack.market.candle.service.CandleService
import org.coreypett.fullstack.market.model.MarketSymbol
import org.coreypett.fullstack.network.HyperliquidJson

class CandleServiceParserTest {
    @Test
    fun serializesCandleSnapshotRequest() {
        val request = CandleSnapshotRequestDto(
            type = "candleSnapshot",
            req = CandleSnapshotReqDto(
                coin = "SOL",
                interval = "1h",
                startTime = 1710000000000,
                endTime = 1710003600000,
            ),
        )

        val json = HyperliquidJson
            .encodeToJsonElement(CandleSnapshotRequestDto.serializer(), request)
            .jsonObject
        val req = json.getValue("req").jsonObject

        assertEquals("candleSnapshot", json.getValue("type").jsonPrimitive.content)
        assertEquals("SOL", req.getValue("coin").jsonPrimitive.content)
        assertEquals("1h", req.getValue("interval").jsonPrimitive.content)
        assertEquals("1710000000000", req.getValue("startTime").jsonPrimitive.content)
        assertEquals("1710003600000", req.getValue("endTime").jsonPrimitive.content)
    }

    @Test
    fun parsesCandleSnapshotResponseAndFiltersSelection() {
        val service = CandleService.Impl()
        val bars = service.parseHistory(
            data = HyperliquidJson.parseToJsonElement(
                """
                    [
                      {
                        "t": 1710003600000,
                        "T": 1710007199999,
                        "s": "BTC",
                        "i": "1h",
                        "o": "69100.0",
                        "c": "69250.0",
                        "h": "69300.0",
                        "l": "69000.0",
                        "v": "10.0",
                        "n": 50
                      },
                      {
                        "t": 1710000000000,
                        "T": 1710003599999,
                        "s": "BTC",
                        "i": "1h",
                        "o": "69000.0",
                        "c": "69100.0",
                        "h": "69200.0",
                        "l": "68850.0",
                        "v": "12.0",
                        "n": 40
                      },
                      {
                        "t": 1710000000000,
                        "T": 1710003599999,
                        "s": "ETH",
                        "i": "1h",
                        "o": "3500.0",
                        "c": "3510.0",
                        "h": "3525.0",
                        "l": "3490.0",
                        "v": "9.0",
                        "n": 7
                      }
                    ]
                """.trimIndent(),
            ),
            selection = CandleSelection(
                market = MarketSymbol.BTC,
                interval = CandleInterval.OneHour,
            ),
        )

        assertEquals(2, bars.size)
        assertEquals(1710000000000, bars[0].openTimeMillis)
        assertEquals(1710003600000, bars[1].openTimeMillis)
        assertEquals(69100.0, bars[0].close)
        assertEquals(69250.0, bars[1].close)
    }

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
                    "o": "69000.0",
                    "c": "69125.5",
                    "h": "69200.0",
                    "l": "68850.0",
                    "v": "12.75",
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
                      "o": "3500.0",
                      "c": "3510.0",
                      "h": "3525.0",
                      "l": "3490.0",
                      "v": "9.0",
                      "n": 7
                    },
                    {
                      "t": 1710000000000,
                      "T": 1710003599999,
                      "s": "BTC",
                      "i": "1h",
                      "o": "69000.0",
                      "c": "69100.0",
                      "h": "69200.0",
                      "l": "68850.0",
                      "v": "12.0",
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
