package org.coreypett.fullstack.orderbook.presentation

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.coreypett.fullstack.market.model.MarketSymbol
import org.coreypett.fullstack.orderbook.model.OrderBookSelection
import org.coreypett.fullstack.orderbook.model.OrderBookUiState
import org.coreypett.fullstack.orderbook.model.PricePrecision
import org.coreypett.fullstack.orderbook.repository.OrderBookRepository

class OrderBookFeature(
    repository: OrderBookRepository,
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val selectionMutableState = MutableStateFlow(OrderBookSelection())
    private var observerJob: Job? = null

    val selectionState: StateFlow<OrderBookSelection> = selectionMutableState.asStateFlow()

    val state: StateFlow<OrderBookViewState> = repository.states(selectionMutableState)
        .map(OrderBookViewState::from)
        .stateIn(
            scope = scope,
            started = SharingStarted.WhileSubscribed(stopTimeoutMillis = 5_000),
            initialValue = OrderBookViewState.Connecting,
        )

    val selection: OrderBookSelection
        get() = selectionState.value

    val currentState: OrderBookViewState
        get() = state.value

    fun observe(observer: (OrderBookViewState) -> Unit) {
        observerJob?.cancel()
        observerJob = scope.launch {
            state.collect { nextState ->
                withContext(Dispatchers.Main.immediate) {
                    observer(nextState)
                }
            }
        }
    }

    fun clearObserver() {
        observerJob?.cancel()
        observerJob = null
    }

    fun selectMarket(market: MarketSymbol) {
        updateSelection(selection.copy(market = market))
    }

    fun selectPrecision(precision: PricePrecision) {
        updateSelection(selection.copy(precision = precision))
    }

    fun close() {
        clearObserver()
        scope.cancel()
    }

    private fun updateSelection(nextSelection: OrderBookSelection) {
        selectionMutableState.value = nextSelection
    }

    companion object {
        fun preview(): OrderBookFeature = OrderBookFeature(
            repository = object : OrderBookRepository {
                override fun states(selection: StateFlow<OrderBookSelection>): Flow<OrderBookUiState> =
                    flowOf(OrderBookUiState.Connecting)
            },
        )
    }
}
