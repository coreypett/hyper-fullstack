package org.coreypett.fullstack.market.trade.repository

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.coreypett.fullstack.market.trade.model.RecentTradesEntry
import org.coreypett.fullstack.market.trade.model.RecentTradesSelection
import org.coreypett.fullstack.market.trade.model.RecentTradesUiState
import org.coreypett.fullstack.market.trade.service.RecentTradesService
import org.coreypett.fullstack.network.RealtimeFeedEvent

interface RecentTradesRepository {
    fun states(selection: StateFlow<RecentTradesSelection>): Flow<RecentTradesUiState>

    class Impl internal constructor(
        private val service: RecentTradesService,
    ) : RecentTradesRepository {
        private val latestTrades = mutableMapOf<RecentTradesSelection, List<RecentTradesEntry>>()
        private val latestTradesMutex = Mutex()

        constructor() : this(RecentTradesService.Impl())

        @OptIn(ExperimentalCoroutinesApi::class)
        override fun states(selection: StateFlow<RecentTradesSelection>): Flow<RecentTradesUiState> =
            selection.flatMapLatest { currentSelection ->
                flow {
                    var recentTrades = latestTrades(currentSelection)
                    if (recentTrades.isEmpty()) {
                        emit(RecentTradesUiState.Connecting)
                    } else {
                        emit(RecentTradesUiState.Live(recentTrades))
                    }

                    try {
                        service.recentTradesEvents(currentSelection).collect { event ->
                            when (event) {
                                is RealtimeFeedEvent.Live -> {
                                    recentTrades = (event.value + recentTrades)
                                        .distinctBy { trade -> trade.timeMillis to trade.tradeId }
                                        .sortedByDescending(RecentTradesEntry::timeMillis)
                                        .take(MaxRecentTrades)
                                    cacheTrades(currentSelection, recentTrades)
                                    emit(RecentTradesUiState.Live(recentTrades))
                                }
                                is RealtimeFeedEvent.Reconnecting -> {
                                    if (recentTrades.isEmpty()) {
                                        emit(RecentTradesUiState.Connecting)
                                    } else {
                                        emit(RecentTradesUiState.Stale(recentTrades, event.reconnectMessage()))
                                    }
                                }
                            }
                        }
                    } catch (error: Throwable) {
                        if (error is CancellationException) throw error
                        val message = error.message ?: "Unable to load trades"
                        if (recentTrades.isEmpty()) {
                            emit(RecentTradesUiState.Failed(message))
                        } else {
                            emit(RecentTradesUiState.Stale(recentTrades, message))
                        }
                    }
                }
            }

        private suspend fun latestTrades(selection: RecentTradesSelection): List<RecentTradesEntry> =
            latestTradesMutex.withLock {
                latestTrades[selection].orEmpty()
            }

        private suspend fun cacheTrades(
            selection: RecentTradesSelection,
            trades: List<RecentTradesEntry>,
        ) {
            latestTradesMutex.withLock {
                latestTrades[selection] = trades.take(MaxRecentTrades)
            }
        }
    }
}

private fun RealtimeFeedEvent.Reconnecting.reconnectMessage(): String =
    reason ?: "Reconnecting in ${delayMillis}ms"

private const val MaxRecentTrades = 40
