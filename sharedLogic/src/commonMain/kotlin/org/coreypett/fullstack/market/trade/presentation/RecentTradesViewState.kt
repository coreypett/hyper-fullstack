package org.coreypett.fullstack.market.trade.presentation

import org.coreypett.fullstack.market.trade.model.RecentTradesEntry
import org.coreypett.fullstack.market.trade.model.RecentTradesSide
import org.coreypett.fullstack.market.trade.model.RecentTradesUiState

data class RecentTradesViewState(
    val status: RecentTradesStatus,
    val statusLabel: String,
    val centerMessage: String?,
    val recentTrades: List<RecentTradesRowDisplay>,
) {
    val hasTrades: Boolean = recentTrades.isNotEmpty()

    companion object {
        val Connecting = RecentTradesViewState(
            status = RecentTradesStatus.Connecting,
            statusLabel = "Connecting",
            centerMessage = "Connecting",
            recentTrades = emptyList(),
        )

        fun from(uiState: RecentTradesUiState): RecentTradesViewState = when (uiState) {
            RecentTradesUiState.Connecting -> Connecting
            is RecentTradesUiState.Failed -> RecentTradesViewState(
                status = RecentTradesStatus.Failed,
                statusLabel = "Disconnected",
                centerMessage = uiState.message,
                recentTrades = emptyList(),
            )
            is RecentTradesUiState.Live -> uiState.recentTrades.toRecentTradesViewState(RecentTradesStatus.Live, "Live", null)
            is RecentTradesUiState.Stale -> uiState.recentTrades.toRecentTradesViewState(
                status = RecentTradesStatus.Stale,
                statusLabel = "Reconnecting",
                centerMessage = null,
            )
        }
    }
}

enum class RecentTradesStatus {
    Connecting,
    Live,
    Stale,
    Failed,
}

data class RecentTradesRowDisplay(
    val side: RecentTradesSide,
    val sideText: String,
    val priceText: String,
    val sizeText: String,
    val timeMillis: Long,
    val tradeId: Long,
)

private fun List<RecentTradesEntry>.toRecentTradesViewState(
    status: RecentTradesStatus,
    statusLabel: String,
    centerMessage: String?,
): RecentTradesViewState = RecentTradesViewState(
    status = status,
    statusLabel = statusLabel,
    centerMessage = centerMessage,
    recentTrades = map(RecentTradesEntry::toRecentTradesRowDisplay),
)

private fun RecentTradesEntry.toRecentTradesRowDisplay(): RecentTradesRowDisplay = RecentTradesRowDisplay(
    side = side,
    sideText = when (side) {
        RecentTradesSide.Buy -> "Buy"
        RecentTradesSide.Sell -> "Sell"
        RecentTradesSide.Unknown -> "--"
    },
    priceText = priceText,
    sizeText = sizeText,
    timeMillis = timeMillis,
    tradeId = tradeId,
)
