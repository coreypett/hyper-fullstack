package org.coreypett.fullstack.market.trade.model

sealed interface TradeUiState {
    data object Connecting : TradeUiState
    data class Live(val trades: List<Trade>) : TradeUiState
    data class Stale(val trades: List<Trade>, val message: String) : TradeUiState
    data class Failed(val message: String) : TradeUiState
}
