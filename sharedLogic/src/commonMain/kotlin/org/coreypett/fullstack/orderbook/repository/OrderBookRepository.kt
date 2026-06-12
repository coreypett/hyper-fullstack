package org.coreypett.fullstack.orderbook.repository

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import org.coreypett.fullstack.orderbook.model.OrderBookSelection
import org.coreypett.fullstack.orderbook.model.OrderBookSnapshot
import org.coreypett.fullstack.orderbook.model.OrderBookUiState
import org.coreypett.fullstack.orderbook.service.OrderBookService

class OrderBookRepository internal constructor(
    private val service: OrderBookService,
) {
    constructor() : this(OrderBookService.Impl())

    @OptIn(ExperimentalCoroutinesApi::class)
    fun states(selection: StateFlow<OrderBookSelection>): Flow<OrderBookUiState> =
        selection.flatMapLatest { currentSelection ->
            service.snapshots(currentSelection)
                .map<OrderBookSnapshot, OrderBookUiState>(OrderBookUiState::Live)
                .onStart { emit(OrderBookUiState.Connecting) }
                .catch { error ->
                    emit(OrderBookUiState.Failed(error.message ?: "Unable to load order book"))
                }
        }
}
