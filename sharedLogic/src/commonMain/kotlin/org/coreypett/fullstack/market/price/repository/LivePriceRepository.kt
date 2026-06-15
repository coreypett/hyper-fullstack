package org.coreypett.fullstack.market.price.repository

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.coreypett.fullstack.market.model.MarketSymbol
import org.coreypett.fullstack.market.price.model.LivePrice
import org.coreypett.fullstack.market.price.model.LivePriceUiState
import org.coreypett.fullstack.market.price.service.LivePriceService
import org.coreypett.fullstack.network.RealtimeFeedEvent

interface LivePriceRepository {
    fun states(market: StateFlow<MarketSymbol>): Flow<LivePriceUiState>

    class Impl internal constructor(
        private val service: LivePriceService,
    ) : LivePriceRepository {
        private val latestPrices = mutableMapOf<MarketSymbol, LivePrice>()
        private val latestPricesMutex = Mutex()

        constructor() : this(LivePriceService.Impl())

        @OptIn(ExperimentalCoroutinesApi::class)
        override fun states(market: StateFlow<MarketSymbol>): Flow<LivePriceUiState> =
            market.flatMapLatest { currentMarket ->
                flow {
                    var lastPrice = latestPrice(currentMarket)
                    if (lastPrice == null) {
                        emit(LivePriceUiState.Connecting)
                    } else {
                        emit(LivePriceUiState.Live(lastPrice))
                    }

                    try {
                        service.priceEvents(currentMarket).collect { event ->
                            when (event) {
                                is RealtimeFeedEvent.Live -> {
                                    lastPrice = event.value
                                    cachePrice(event.value)
                                    emit(LivePriceUiState.Live(event.value))
                                }
                                is RealtimeFeedEvent.Reconnecting -> {
                                    val price = lastPrice
                                    if (price == null) {
                                        emit(LivePriceUiState.Connecting)
                                    } else {
                                        emit(LivePriceUiState.Stale(price, event.reconnectMessage()))
                                    }
                                }
                            }
                        }
                    } catch (error: Throwable) {
                        if (error is CancellationException) throw error
                        val message = error.message ?: "Unable to load live price"
                        val price = lastPrice
                        if (price == null) {
                            emit(LivePriceUiState.Failed(message))
                        } else {
                            emit(LivePriceUiState.Stale(price, message))
                        }
                    }
                }
            }

        private suspend fun latestPrice(market: MarketSymbol): LivePrice? =
            latestPricesMutex.withLock {
                latestPrices[market]
            }

        private suspend fun cachePrice(price: LivePrice) {
            latestPricesMutex.withLock {
                latestPrices[price.market] = price
            }
        }
    }
}

private fun RealtimeFeedEvent.Reconnecting.reconnectMessage(): String =
    reason ?: "Reconnecting in ${delayMillis}ms"
