package org.coreypett.fullstack.market.candle.presentation

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
import org.coreypett.fullstack.market.candle.model.CandleBar
import org.coreypett.fullstack.market.candle.model.CandleChartUiState
import org.coreypett.fullstack.market.candle.model.CandleHistoryRange
import org.coreypett.fullstack.market.candle.model.CandleInterval
import org.coreypett.fullstack.market.candle.model.CandleSelection
import org.coreypett.fullstack.market.candle.repository.CandleRepository
import org.coreypett.fullstack.market.model.MarketSymbol
import org.coreypett.fullstack.time.currentTimeMillis

class CandleChartFeature(
    repository: CandleRepository,
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val selectionMutableState = MutableStateFlow(CandleSelection())
    private val historyRangeMutableState = MutableStateFlow(defaultHistoryRange(CandleSelection().interval))
    private var observerJob: Job? = null

    val selectionState: StateFlow<CandleSelection> = selectionMutableState.asStateFlow()

    val state: StateFlow<CandleChartState> = repository.states(
        selection = selectionMutableState,
        historyRange = historyRangeMutableState,
    )
        .map(CandleChartState::from)
        .stateIn(
            scope = scope,
            started = SharingStarted.WhileSubscribed(stopTimeoutMillis = 5_000),
            initialValue = CandleChartState.Connecting,
        )

    val selection: CandleSelection
        get() = selectionState.value

    val currentState: CandleChartState
        get() = state.value

    fun observe(observer: (CandleChartState) -> Unit) {
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
        historyRangeMutableState.value = defaultHistoryRange(selection.interval)
    }

    fun selectInterval(interval: CandleInterval) {
        selectionMutableState.value = selection.copy(interval = interval)
        historyRangeMutableState.value = defaultHistoryRange(interval)
    }

    fun close() {
        clearObserver()
        scope.cancel()
    }
}

data class CandleChartState(
    val status: CandleChartStatus,
    val message: String?,
    val bars: List<CandleBar>,
) {
    companion object {
        val Connecting = CandleChartState(
            status = CandleChartStatus.Connecting,
            message = "Loading candles",
            bars = emptyList(),
        )

        fun from(uiState: CandleChartUiState): CandleChartState = when (uiState) {
            CandleChartUiState.Connecting -> Connecting
            is CandleChartUiState.Failed -> CandleChartState(
                status = CandleChartStatus.Failed,
                message = uiState.message,
                bars = emptyList(),
            )
            is CandleChartUiState.Live -> CandleChartState(
                status = CandleChartStatus.Live,
                message = null,
                bars = uiState.bars,
            )
            is CandleChartUiState.Stale -> CandleChartState(
                status = CandleChartStatus.Stale,
                message = uiState.message,
                bars = uiState.bars,
            )
        }
    }
}

enum class CandleChartStatus {
    Connecting,
    Live,
    Stale,
    Failed,
}

private fun defaultHistoryRange(interval: CandleInterval): CandleHistoryRange {
    val endTimeMillis = currentTimeMillis()
    return CandleHistoryRange(
        startTimeMillis = endTimeMillis - interval.defaultHistoryDurationMillis,
        endTimeMillis = endTimeMillis,
    )
}

private val CandleInterval.defaultHistoryDurationMillis: Long
    get() = when (this) {
        CandleInterval.OneMinute -> 12L.hours
        CandleInterval.FiveMinutes -> 3L.days
        CandleInterval.OneHour -> 30L.days
        CandleInterval.FourHours -> 90L.days
        CandleInterval.OneDay -> 365L.days
        else -> 30L.days
    }

private val Long.hours: Long
    get() = this * 60 * 60 * 1_000

private val Long.days: Long
    get() = this * 24 * 60 * 60 * 1_000
