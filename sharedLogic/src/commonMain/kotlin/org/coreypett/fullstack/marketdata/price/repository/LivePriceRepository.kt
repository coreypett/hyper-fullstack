package org.coreypett.fullstack.marketdata.price.repository

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import org.coreypett.fullstack.market.model.MarketSymbol
import org.coreypett.fullstack.marketdata.price.model.LivePrice
import org.coreypett.fullstack.marketdata.price.model.LivePriceUiState
import org.coreypett.fullstack.marketdata.price.service.LivePriceService

class LivePriceRepository internal constructor(
    private val service: LivePriceService,
) {
    constructor() : this(LivePriceService.Impl())

    @OptIn(ExperimentalCoroutinesApi::class)
    fun states(market: StateFlow<MarketSymbol>): Flow<LivePriceUiState> =
        market.flatMapLatest { currentMarket ->
            service.prices(currentMarket)
                .map<LivePrice, LivePriceUiState>(LivePriceUiState::Live)
                .onStart { emit(LivePriceUiState.Connecting) }
                .catch { error ->
                    emit(LivePriceUiState.Failed(error.message ?: "Unable to load live price"))
                }
        }
}
