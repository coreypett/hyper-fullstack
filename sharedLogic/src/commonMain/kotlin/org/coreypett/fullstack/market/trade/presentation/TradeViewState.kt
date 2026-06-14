package org.coreypett.fullstack.market.trade.presentation

import org.coreypett.fullstack.market.trade.model.Trade
import org.coreypett.fullstack.market.trade.model.TradeSide
import org.coreypett.fullstack.market.trade.model.TradeUiState

data class TradeViewState(
    val status: TradeStatus,
    val statusLabel: String,
    val centerMessage: String?,
    val trades: List<TradeRowDisplay>,
) {
    val hasTrades: Boolean = trades.isNotEmpty()

    companion object {
        val Connecting = TradeViewState(
            status = TradeStatus.Connecting,
            statusLabel = "Connecting",
            centerMessage = "Connecting",
            trades = emptyList(),
        )

        fun from(uiState: TradeUiState): TradeViewState = when (uiState) {
            TradeUiState.Connecting -> Connecting
            is TradeUiState.Failed -> TradeViewState(
                status = TradeStatus.Failed,
                statusLabel = "Disconnected",
                centerMessage = uiState.message,
                trades = emptyList(),
            )
            is TradeUiState.Live -> uiState.trades.toViewState(TradeStatus.Live, "Live", null)
            is TradeUiState.Stale -> uiState.trades.toViewState(
                status = TradeStatus.Stale,
                statusLabel = "Reconnecting",
                centerMessage = null,
            )
        }
    }
}

enum class TradeStatus {
    Connecting,
    Live,
    Stale,
    Failed,
}

data class TradeRowDisplay(
    val side: TradeSide,
    val sideText: String,
    val priceText: String,
    val sizeText: String,
    val timeMillis: Long,
    val tradeId: Long,
)

private fun List<Trade>.toViewState(
    status: TradeStatus,
    statusLabel: String,
    centerMessage: String?,
): TradeViewState = TradeViewState(
    status = status,
    statusLabel = statusLabel,
    centerMessage = centerMessage,
    trades = map(Trade::toRowDisplay),
)

private fun Trade.toRowDisplay(): TradeRowDisplay = TradeRowDisplay(
    side = side,
    sideText = when (side) {
        TradeSide.Buy -> "Buy"
        TradeSide.Sell -> "Sell"
        TradeSide.Unknown -> "--"
    },
    priceText = priceText,
    sizeText = sizeText,
    timeMillis = timeMillis,
    tradeId = tradeId,
)
