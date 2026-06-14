package org.coreypett.fullstack.market.summary.repository

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import org.coreypett.fullstack.market.model.MarketSymbol
import org.coreypett.fullstack.market.summary.model.MarketSummary
import org.coreypett.fullstack.market.summary.model.MarketSummaryUiState
import org.coreypett.fullstack.market.summary.service.MarketSummaryService

interface MarketSummaryRepository {
    fun states(market: StateFlow<MarketSymbol>): Flow<MarketSummaryUiState>

    class Impl internal constructor(
        private val service: MarketSummaryService,
    ) : MarketSummaryRepository {
        constructor() : this(MarketSummaryService.Impl())

        @OptIn(ExperimentalCoroutinesApi::class)
        override fun states(market: StateFlow<MarketSymbol>): Flow<MarketSummaryUiState> =
            market.flatMapLatest { selectedMarket ->
                flow {
                    var lastSummary: MarketSummary? = null
                    emit(MarketSummaryUiState.Connecting)
                    while (true) {
                        try {
                            val summary = service.summary(selectedMarket)
                            lastSummary = summary
                            emit(MarketSummaryUiState.Live(summary))
                        } catch (error: Throwable) {
                            if (error is CancellationException) throw error
                            val message = error.message ?: "Unable to load market"
                            if (lastSummary == null) {
                                emit(MarketSummaryUiState.Failed(message))
                            } else {
                                emit(MarketSummaryUiState.Stale(lastSummary, message))
                            }
                        }
                        delay(SummaryRefreshMillis)
                    }
                }
            }
    }
}

private const val SummaryRefreshMillis = 30_000L
