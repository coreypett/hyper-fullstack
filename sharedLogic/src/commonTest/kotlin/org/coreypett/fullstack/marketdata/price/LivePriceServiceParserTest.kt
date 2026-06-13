package org.coreypett.fullstack.marketdata.price

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import org.coreypett.fullstack.market.model.MarketSymbol
import org.coreypett.fullstack.marketdata.price.service.LivePriceService

class LivePriceServiceParserTest {
    @Test
    fun parsesLivePriceFromAllMidsEnvelope() {
        val service = LivePriceService.Impl()
        val price = service.parsePrice(
            text = """
                {
                  "channel": "allMids",
                  "data": {
                    "mids": {
                      "BTC": "69125.5",
                      "ETH": "3510.25"
                    }
                  }
                }
            """.trimIndent(),
            market = MarketSymbol.BTC,
        )

        assertNotNull(price)
        assertEquals(MarketSymbol.BTC, price.market)
        assertEquals(69125.5, price.price)
    }

    @Test
    fun ignoresMissingLivePriceMarket() {
        val service = LivePriceService.Impl()
        val price = service.parsePrice(
            text = """
                {
                  "channel": "allMids",
                  "data": {
                    "mids": {
                      "ETH": "3510.25"
                    }
                  }
                }
            """.trimIndent(),
            market = MarketSymbol.BTC,
        )

        assertNull(price)
    }
}
