package org.coreypett.fullstack.orderbook

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart

class OrderBookRepository(
    private val client: HyperliquidOrderBookClient = HyperliquidWebSocketOrderBookClient(),
) {
    @OptIn(ExperimentalCoroutinesApi::class)
    fun states(selection: StateFlow<OrderBookSelection>): Flow<OrderBookUiState> =
        selection.flatMapLatest { currentSelection ->
            client.snapshots(currentSelection)
                .map<OrderBookSnapshot, OrderBookUiState>(OrderBookUiState::Live)
                .onStart { emit(OrderBookUiState.Connecting) }
                .catch { error ->
                    emit(OrderBookUiState.Failed(error.message ?: "Unable to load order book"))
                }
        }
}
