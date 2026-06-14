package org.coreypett.fullstack.orderbook.presentation

import org.coreypett.fullstack.orderbook.model.LevelChange
import org.coreypett.fullstack.orderbook.model.OrderBookLevel
import org.coreypett.fullstack.orderbook.model.OrderBookSide
import org.coreypett.fullstack.orderbook.model.OrderBookSnapshot
import org.coreypett.fullstack.orderbook.model.OrderBookUiState

data class OrderBookViewState(
    val status: OrderBookStatus,
    val statusLabel: String,
    val centerMessage: String?,
    val spreadText: String?,
    val asks: List<OrderBookRowDisplay>,
    val bids: List<OrderBookRowDisplay>,
) {
    val hasRows: Boolean = asks.isNotEmpty() || bids.isNotEmpty()

    companion object {
        val Connecting = OrderBookViewState(
            status = OrderBookStatus.Connecting,
            statusLabel = "Connecting",
            centerMessage = "Connecting",
            spreadText = null,
            asks = emptyList(),
            bids = emptyList(),
        )

        fun from(uiState: OrderBookUiState): OrderBookViewState = when (uiState) {
            OrderBookUiState.Connecting -> Connecting
            is OrderBookUiState.Failed -> OrderBookViewState(
                status = OrderBookStatus.Failed,
                statusLabel = "Disconnected",
                centerMessage = uiState.message,
                spreadText = null,
                asks = emptyList(),
                bids = emptyList(),
            )
            is OrderBookUiState.Live -> uiState.snapshot.toViewState(OrderBookStatus.Live, "Live", null)
            is OrderBookUiState.Stale -> uiState.snapshot.toViewState(
                status = OrderBookStatus.Stale,
                statusLabel = "Reconnecting",
                centerMessage = null,
            )
        }
    }
}

enum class OrderBookStatus {
    Connecting,
    Live,
    Stale,
    Failed,
}

data class OrderBookRowDisplay(
    val side: OrderBookSide,
    val priceText: String,
    val sizeText: String,
    val orderCountText: String,
    val depthFraction: Float,
    val change: LevelChange,
)

private fun OrderBookSnapshot.toViewState(
    status: OrderBookStatus,
    statusLabel: String,
    centerMessage: String?,
): OrderBookViewState = OrderBookViewState(
    status = status,
    statusLabel = statusLabel,
    centerMessage = centerMessage,
    spreadText = spreadText,
    asks = asks.asReversed().map(OrderBookLevel::toRowDisplay),
    bids = bids.map(OrderBookLevel::toRowDisplay),
)

private fun OrderBookLevel.toRowDisplay(): OrderBookRowDisplay = OrderBookRowDisplay(
    side = side,
    priceText = priceText,
    sizeText = sizeText,
    orderCountText = orderCount.toString(),
    depthFraction = depthFraction,
    change = change,
)
