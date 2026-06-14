package org.coreypett.fullstack.market.trade.presentation

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
import org.coreypett.fullstack.market.trade.model.TradeSelection
import org.coreypett.fullstack.market.trade.model.TradeUiState
import org.coreypett.fullstack.market.trade.repository.TradeRepository

class TradeFeature(
    repository: TradeRepository,
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val selectionMutableState = MutableStateFlow(TradeSelection())
    private var observerJob: Job? = null

    val selectionState: StateFlow<TradeSelection> = selectionMutableState.asStateFlow()

    val state: StateFlow<TradeViewState> = repository.states(selectionMutableState)
        .map(TradeViewState::from)
        .stateIn(
            scope = scope,
            started = SharingStarted.WhileSubscribed(stopTimeoutMillis = 5_000),
            initialValue = TradeViewState.Connecting,
        )

    val selection: TradeSelection
        get() = selectionState.value

    val currentState: TradeViewState
        get() = state.value

    fun observe(observer: (TradeViewState) -> Unit) {
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
        selectionMutableState.value = selection.copy(market = market)
    }

    fun close() {
        clearObserver()
        scope.cancel()
    }

    companion object {
        fun preview(): TradeFeature = TradeFeature(
            repository = object : TradeRepository {
                override fun states(selection: StateFlow<TradeSelection>): Flow<TradeUiState> =
                    flowOf(TradeUiState.Connecting)
            },
        )
    }
}
