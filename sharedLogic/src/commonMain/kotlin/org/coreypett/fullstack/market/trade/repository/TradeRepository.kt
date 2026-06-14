package org.coreypett.fullstack.market.trade.repository

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import org.coreypett.fullstack.market.trade.model.Trade
import org.coreypett.fullstack.market.trade.model.TradeSelection
import org.coreypett.fullstack.market.trade.model.TradeUiState
import org.coreypett.fullstack.market.trade.service.TradeService
import org.coreypett.fullstack.network.RealtimeFeedEvent

interface TradeRepository {
    fun states(selection: StateFlow<TradeSelection>): Flow<TradeUiState>

    class Impl internal constructor(
        private val service: TradeService,
    ) : TradeRepository {
        constructor() : this(TradeService.Impl())

        @OptIn(ExperimentalCoroutinesApi::class)
        override fun states(selection: StateFlow<TradeSelection>): Flow<TradeUiState> =
            selection.flatMapLatest { currentSelection ->
                flow {
                    var recentTrades = emptyList<Trade>()
                    emit(TradeUiState.Connecting)

                    try {
                        service.tradeEvents(currentSelection).collect { event ->
                            when (event) {
                                is RealtimeFeedEvent.Live -> {
                                    recentTrades = (event.value + recentTrades)
                                        .distinctBy { trade -> trade.timeMillis to trade.tradeId }
                                        .sortedByDescending(Trade::timeMillis)
                                        .take(MaxRecentTrades)
                                    emit(TradeUiState.Live(recentTrades))
                                }
                                is RealtimeFeedEvent.Reconnecting -> {
                                    if (recentTrades.isEmpty()) {
                                        emit(TradeUiState.Connecting)
                                    } else {
                                        emit(TradeUiState.Stale(recentTrades, event.reconnectMessage()))
                                    }
                                }
                            }
                        }
                    } catch (error: Throwable) {
                        if (error is CancellationException) throw error
                        val message = error.message ?: "Unable to load trades"
                        if (recentTrades.isEmpty()) {
                            emit(TradeUiState.Failed(message))
                        } else {
                            emit(TradeUiState.Stale(recentTrades, message))
                        }
                    }
                }
            }
    }
}

private fun RealtimeFeedEvent.Reconnecting.reconnectMessage(): String =
    reason ?: "Reconnecting in ${delayMillis}ms"

private const val MaxRecentTrades = 40
