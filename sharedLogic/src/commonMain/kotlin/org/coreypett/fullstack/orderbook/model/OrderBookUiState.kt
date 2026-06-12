package org.coreypett.fullstack.orderbook.model

sealed interface OrderBookUiState {
    data object Connecting : OrderBookUiState
    data class Live(val snapshot: OrderBookSnapshot) : OrderBookUiState
    data class Failed(val message: String) : OrderBookUiState
}
