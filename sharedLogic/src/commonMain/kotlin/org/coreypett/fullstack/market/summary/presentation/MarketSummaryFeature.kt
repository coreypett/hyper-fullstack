package org.coreypett.fullstack.market.summary.presentation

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.coreypett.fullstack.market.model.MarketSymbol
import org.coreypett.fullstack.market.summary.model.MarketSummaryViewState
import org.coreypett.fullstack.market.summary.repository.MarketSummaryRepository

class MarketSummaryFeature(
    repository: MarketSummaryRepository,
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val marketMutableState = MutableStateFlow(MarketSymbol.BTC)
    private var observerJob: Job? = null

    val marketState: StateFlow<MarketSymbol> = marketMutableState.asStateFlow()

    val state: StateFlow<MarketSummaryViewState> = repository.states(marketMutableState)
        .map(MarketSummaryViewState::from)
        .stateIn(
            scope = scope,
            started = SharingStarted.WhileSubscribed(stopTimeoutMillis = 5_000),
            initialValue = MarketSummaryViewState.Connecting,
        )

    val market: MarketSymbol
        get() = marketState.value

    val currentState: MarketSummaryViewState
        get() = state.value

    fun observe(observer: (MarketSummaryViewState) -> Unit) {
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
        marketMutableState.value = market
    }

    fun close() {
        clearObserver()
        scope.cancel()
    }
}
