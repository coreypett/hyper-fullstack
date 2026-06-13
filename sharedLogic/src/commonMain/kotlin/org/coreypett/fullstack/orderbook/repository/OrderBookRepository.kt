package org.coreypett.fullstack.orderbook.repository

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import org.coreypett.fullstack.network.RealtimeFeedEvent
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
            flow {
                var lastSnapshot: OrderBookSnapshot? = null
                emit(OrderBookUiState.Connecting)

                try {
                    service.snapshotEvents(currentSelection).collect { event ->
                        when (event) {
                            is RealtimeFeedEvent.Live -> {
                                lastSnapshot = event.value
                                emit(OrderBookUiState.Live(event.value))
                            }
                            is RealtimeFeedEvent.Reconnecting -> {
                                val snapshot = lastSnapshot
                                if (snapshot == null) {
                                    emit(OrderBookUiState.Connecting)
                                } else {
                                    emit(OrderBookUiState.Stale(snapshot, event.reconnectMessage()))
                                }
                            }
                        }
                    }
                } catch (error: Throwable) {
                    if (error is CancellationException) throw error
                    val message = error.message ?: "Unable to load order book"
                    val snapshot = lastSnapshot
                    if (snapshot == null) {
                        emit(OrderBookUiState.Failed(message))
                    } else {
                        emit(OrderBookUiState.Stale(snapshot, message))
                    }
                }
            }
        }
}

private fun RealtimeFeedEvent.Reconnecting.reconnectMessage(): String =
    reason ?: "Reconnecting in ${delayMillis}ms"
