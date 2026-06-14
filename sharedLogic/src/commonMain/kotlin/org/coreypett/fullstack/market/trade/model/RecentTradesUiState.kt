package org.coreypett.fullstack.market.trade.model

sealed interface RecentTradesUiState {
    data object Connecting : RecentTradesUiState
    data class Live(val recentTrades: List<RecentTradesEntry>) : RecentTradesUiState
    data class Stale(val recentTrades: List<RecentTradesEntry>, val message: String) : RecentTradesUiState
    data class Failed(val message: String) : RecentTradesUiState
}
