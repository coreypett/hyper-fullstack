package org.coreypett.fullstack.di

import kotlinx.serialization.json.Json
import org.coreypett.fullstack.market.candle.presentation.CandleChartFeature
import org.coreypett.fullstack.market.candle.repository.CandleRepository
import org.coreypett.fullstack.market.candle.service.CandleService
import org.coreypett.fullstack.market.price.repository.LivePriceRepository
import org.coreypett.fullstack.market.price.service.LivePriceService
import org.coreypett.fullstack.network.HyperliquidInfoClient
import org.coreypett.fullstack.network.HyperliquidJson
import org.coreypett.fullstack.network.HyperliquidWebSocketClient
import org.coreypett.fullstack.orderbook.presentation.OrderBookFeature
import org.coreypett.fullstack.orderbook.repository.OrderBookRepository
import org.coreypett.fullstack.orderbook.service.OrderBookService
import org.koin.core.Koin
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.dsl.module
import org.koin.mp.KoinPlatformTools

object SharedDependencyGraph {
    fun start() {
        ensureKoin()
    }

    fun stop() {
        stopKoin()
    }

    fun orderBookRepository(): OrderBookRepository = ensureKoin().get()

    fun candleRepository(): CandleRepository = ensureKoin().get()

    fun livePriceRepository(): LivePriceRepository = ensureKoin().get()

    fun candleChartFeature(): CandleChartFeature = ensureKoin().get()

    fun orderBookFeature(): OrderBookFeature = ensureKoin().get()

    private fun ensureKoin(): Koin {
        val context = KoinPlatformTools.defaultContext()
        return context.getOrNull() ?: startKoin {
            modules(sharedLogicModules)
        }.koin
    }
}

private val networkModule = module {
    single<Json> { HyperliquidJson }
    single<HyperliquidWebSocketClient> {
        HyperliquidWebSocketClient.Impl(json = get())
    }
    single<HyperliquidInfoClient> {
        HyperliquidInfoClient.Impl(json = get())
    }
}

private val serviceModule = module {
    single<OrderBookService> {
        OrderBookService.Impl(
            webSocketClient = get(),
            json = get(),
        )
    }
    single<CandleService> {
        CandleService.Impl(
            webSocketClient = get(),
            infoClient = get(),
            json = get(),
        )
    }
    single<LivePriceService> {
        LivePriceService.Impl(
            webSocketClient = get(),
            json = get(),
        )
    }
}

private val repositoryModule = module {
    single<OrderBookRepository> { OrderBookRepository.Impl(service = get()) }
    single<CandleRepository> { CandleRepository.Impl(service = get()) }
    single<LivePriceRepository> { LivePriceRepository.Impl(service = get()) }
}

private val presentationModule = module {
    factory { CandleChartFeature(repository = get()) }
    factory { OrderBookFeature(repository = get()) }
}

internal val sharedLogicModules = listOf(
    networkModule,
    serviceModule,
    repositoryModule,
    presentationModule,
)
