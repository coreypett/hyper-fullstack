package org.coreypett.fullstack.market.summary.service

import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.decodeFromJsonElement
import org.coreypett.fullstack.market.candle.model.CandleHistoryRange
import org.coreypett.fullstack.market.candle.model.CandleInterval
import org.coreypett.fullstack.market.candle.model.CandleSelection
import org.coreypett.fullstack.market.candle.service.CandleService
import org.coreypett.fullstack.market.model.MarketSymbol
import org.coreypett.fullstack.market.summary.dto.MarketAssetContextDto
import org.coreypett.fullstack.market.summary.dto.MarketMetaDto
import org.coreypett.fullstack.market.summary.dto.MarketSummaryRequestDto
import org.coreypett.fullstack.market.summary.model.MarketSummary
import org.coreypett.fullstack.network.HyperliquidInfoClient
import org.coreypett.fullstack.network.HyperliquidJson
import org.coreypett.fullstack.time.currentTimeMillis

internal interface MarketSummaryService {
    suspend fun summary(market: MarketSymbol): MarketSummary

    class Impl(
        private val infoClient: HyperliquidInfoClient = HyperliquidInfoClient.Impl(),
        private val candleService: CandleService = CandleService.Impl(infoClient = infoClient),
        private val json: Json = HyperliquidJson,
    ) : MarketSummaryService {
        override suspend fun summary(market: MarketSymbol): MarketSummary {
            val assetContext = assetContext(market)
            val bars = candleService.history(
                selection = CandleSelection(market = market, interval = CandleInterval.OneHour),
                range = last24Hours(),
            )
            return MarketSummary(
                market = market,
                midPrice = assetContext.midPrice ?: assetContext.markPrice ?: error("Missing ${market.displayName} price"),
                previousDayPrice = assetContext.previousDayPrice,
                volumeUsd24h = assetContext.dayNotionalVolume,
                high24h = bars.maxOfOrNull { it.high },
                low24h = bars.minOfOrNull { it.low },
                openInterest = assetContext.openInterest,
            )
        }

        private suspend fun assetContext(market: MarketSymbol): MarketAssetContextDto {
            val response = infoClient.post(
                request = MarketSummaryRequestDto(type = MetaAndAssetContextsRequestType),
                serializer = MarketSummaryRequestDto.serializer(),
            )
            val envelope = response as? JsonArray ?: error("Unexpected market summary response")
            val meta = json.decodeFromJsonElement(MarketMetaDto.serializer(), envelope[0])
            val contexts = json.decodeFromJsonElement(
                ListSerializer(MarketAssetContextDto.serializer()),
                envelope[1],
            )
            val index = meta.universe.indexOfFirst { asset -> asset.name == market.wireName }
            if (index < 0) error("Missing ${market.displayName} asset metadata")
            return contexts.getOrNull(index) ?: error("Missing ${market.displayName} asset context")
        }

        private fun last24Hours(): CandleHistoryRange {
            val endTimeMillis = currentTimeMillis()
            return CandleHistoryRange(
                startTimeMillis = endTimeMillis - TwentyFourHoursMillis,
                endTimeMillis = endTimeMillis,
            )
        }
    }
}

private const val MetaAndAssetContextsRequestType = "metaAndAssetCtxs"
private const val TwentyFourHoursMillis = 24L * 60L * 60L * 1_000L
