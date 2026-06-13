package org.coreypett.fullstack.marketdata.candle.model

sealed interface CandleChartUiState {
    data object Connecting : CandleChartUiState
    data class Live(val bars: List<CandleBar>) : CandleChartUiState
    data class Failed(val message: String) : CandleChartUiState
}
