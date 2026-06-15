package org.coreypett.fullstack.market.summary.repository

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.coreypett.fullstack.market.model.MarketSymbol
import org.coreypett.fullstack.market.summary.model.MarketSummary
import org.coreypett.fullstack.market.summary.model.MarketSummaryUiState
import org.coreypett.fullstack.market.summary.service.MarketSummaryService

interface MarketSummaryRepository {
    fun states(market: StateFlow<MarketSymbol>): Flow<MarketSummaryUiState>

    class Impl internal constructor(
        private val service: MarketSummaryService,
    ) : MarketSummaryRepository {
        private val latestSummaries = mutableMapOf<MarketSymbol, MarketSummary>()
        private val latestSummariesMutex = Mutex()

        constructor() : this(MarketSummaryService.Impl())

        @OptIn(ExperimentalCoroutinesApi::class)
        override fun states(market: StateFlow<MarketSymbol>): Flow<MarketSummaryUiState> =
            market.flatMapLatest { selectedMarket ->
                flow {
                    var lastSummary = latestSummary(selectedMarket)
                    if (lastSummary == null) {
                        emit(MarketSummaryUiState.Connecting)
                    } else {
                        emit(MarketSummaryUiState.Live(lastSummary))
                    }

                    while (true) {
                        try {
                            val summary = service.summary(selectedMarket)
                            lastSummary = summary
                            cacheSummary(summary)
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

        private suspend fun latestSummary(market: MarketSymbol): MarketSummary? =
            latestSummariesMutex.withLock {
                latestSummaries[market]
            }

        private suspend fun cacheSummary(summary: MarketSummary) {
            latestSummariesMutex.withLock {
                latestSummaries[summary.market] = summary
            }
        }
    }
}

private const val SummaryRefreshMillis = 30_000L
