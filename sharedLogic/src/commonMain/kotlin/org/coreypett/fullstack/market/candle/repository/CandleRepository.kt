package org.coreypett.fullstack.market.candle.repository

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import org.coreypett.fullstack.market.candle.model.CandleBar
import org.coreypett.fullstack.market.candle.model.CandleChartUiState
import org.coreypett.fullstack.market.candle.model.CandleHistoryRange
import org.coreypett.fullstack.market.candle.model.CandleSelection
import org.coreypett.fullstack.market.candle.service.CandleService
import org.coreypett.fullstack.network.RealtimeFeedEvent

class CandleRepository internal constructor(
    private val service: CandleService,
) {
    constructor() : this(CandleService.Impl())

    @OptIn(ExperimentalCoroutinesApi::class)
    fun states(selection: StateFlow<CandleSelection>): Flow<CandleChartUiState> =
        selection.flatMapLatest { currentSelection ->
            candleStates(currentSelection, initialBars = emptyList())
        }

    @OptIn(ExperimentalCoroutinesApi::class)
    fun states(
        selection: StateFlow<CandleSelection>,
        historyRange: StateFlow<CandleHistoryRange>,
    ): Flow<CandleChartUiState> =
        combine(selection, historyRange) { currentSelection, currentRange ->
            currentSelection to currentRange
        }.flatMapLatest { (currentSelection, currentRange) ->
            flow<CandleChartUiState> {
                emit(CandleChartUiState.Connecting)
                try {
                    val historicalBars = service.history(currentSelection, currentRange)
                        .takeLast(MaxVisibleCandles)
                    emit(CandleChartUiState.Live(historicalBars))
                    candleStates(currentSelection, historicalBars, emitConnecting = false)
                        .collect { emit(it) }
                } catch (error: Throwable) {
                    if (error is CancellationException) throw error
                    emit(CandleChartUiState.Failed(error.message ?: "Unable to load candles"))
                }
            }
        }

    private fun candleStates(
        selection: CandleSelection,
        initialBars: List<CandleBar>,
        emitConnecting: Boolean = true,
    ): Flow<CandleChartUiState> = flow {
        var bars = initialBars.takeLast(MaxVisibleCandles)
        if (emitConnecting) {
            emit(CandleChartUiState.Connecting)
        }

        try {
            service.candleEvents(selection).collect { event ->
                when (event) {
                    is RealtimeFeedEvent.Live -> {
                        bars = bars.upsertByOpenTime(event.value).takeLast(MaxVisibleCandles)
                        emit(CandleChartUiState.Live(bars))
                    }
                    is RealtimeFeedEvent.Reconnecting -> {
                        if (bars.isEmpty()) {
                            emit(CandleChartUiState.Connecting)
                        } else {
                            emit(CandleChartUiState.Stale(bars, event.reconnectMessage()))
                        }
                    }
                }
            }
        } catch (error: Throwable) {
            if (error is CancellationException) throw error
            val message = error.message ?: "Unable to load candles"
            if (bars.isEmpty()) {
                emit(CandleChartUiState.Failed(message))
            } else {
                emit(CandleChartUiState.Stale(bars, message))
            }
        }
    }

    private fun List<CandleBar>.upsertByOpenTime(next: CandleBar): List<CandleBar> {
        val existingIndex = indexOfFirst { it.openTimeMillis == next.openTimeMillis }
        return if (existingIndex >= 0) {
            toMutableList().also { it[existingIndex] = next }
        } else {
            (this + next).sortedBy(CandleBar::openTimeMillis)
        }
    }
}

private fun RealtimeFeedEvent.Reconnecting.reconnectMessage(): String =
    reason ?: "Reconnecting in ${delayMillis}ms"

private const val MaxVisibleCandles = 500
