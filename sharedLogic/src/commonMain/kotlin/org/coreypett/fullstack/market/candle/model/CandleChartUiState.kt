package org.coreypett.fullstack.market.candle.model

sealed interface CandleChartUiState {
    data object Connecting : CandleChartUiState
    data class Live(val bars: List<CandleBar>) : CandleChartUiState
    data class Stale(val bars: List<CandleBar>, val message: String) : CandleChartUiState
    data class Failed(val message: String) : CandleChartUiState
}
