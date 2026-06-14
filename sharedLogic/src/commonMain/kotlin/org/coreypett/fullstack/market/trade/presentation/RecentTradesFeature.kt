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
import org.coreypett.fullstack.market.trade.model.RecentTradesSelection
import org.coreypett.fullstack.market.trade.model.RecentTradesUiState
import org.coreypett.fullstack.market.trade.repository.RecentTradesRepository

class RecentTradesFeature(
    repository: RecentTradesRepository,
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val selectionMutableState = MutableStateFlow(RecentTradesSelection())
    private var observerJob: Job? = null

    val selectionState: StateFlow<RecentTradesSelection> = selectionMutableState.asStateFlow()

    val state: StateFlow<RecentTradesViewState> = repository.states(selectionMutableState)
        .map(RecentTradesViewState::from)
        .stateIn(
            scope = scope,
            started = SharingStarted.WhileSubscribed(stopTimeoutMillis = 5_000),
            initialValue = RecentTradesViewState.Connecting,
        )

    val selection: RecentTradesSelection
        get() = selectionState.value

    val currentState: RecentTradesViewState
        get() = state.value

    fun observe(observer: (RecentTradesViewState) -> Unit) {
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
        fun preview(): RecentTradesFeature = RecentTradesFeature(
            repository = object : RecentTradesRepository {
                override fun states(selection: StateFlow<RecentTradesSelection>): Flow<RecentTradesUiState> =
                    flowOf(RecentTradesUiState.Connecting)
            },
        )
    }
}
