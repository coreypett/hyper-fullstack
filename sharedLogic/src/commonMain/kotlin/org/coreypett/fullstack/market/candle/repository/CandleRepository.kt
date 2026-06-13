package org.coreypett.fullstack.market.candle.repository

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.scan
import org.coreypett.fullstack.market.candle.model.CandleBar
import org.coreypett.fullstack.market.candle.model.CandleChartUiState
import org.coreypett.fullstack.market.candle.model.CandleSelection
import org.coreypett.fullstack.market.candle.service.CandleService

class CandleRepository internal constructor(
    private val service: CandleService,
) {
    constructor() : this(CandleService.Impl())

    @OptIn(ExperimentalCoroutinesApi::class)
    fun states(selection: StateFlow<CandleSelection>): Flow<CandleChartUiState> =
        selection.flatMapLatest { currentSelection ->
            service.candles(currentSelection)
                .scan(emptyList<CandleBar>()) { bars, next ->
                    bars.upsertByOpenTime(next).takeLast(MaxVisibleCandles)
                }
                .drop(1)
                .map<List<CandleBar>, CandleChartUiState>(CandleChartUiState::Live)
                .onStart { emit(CandleChartUiState.Connecting) }
                .catch { error ->
                    emit(CandleChartUiState.Failed(error.message ?: "Unable to load candles"))
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

private const val MaxVisibleCandles = 500
