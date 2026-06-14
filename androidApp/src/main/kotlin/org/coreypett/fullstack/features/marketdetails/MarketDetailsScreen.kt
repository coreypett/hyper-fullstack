package org.coreypett.fullstack.features.marketdetails

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeContentPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import org.coreypett.fullstack.features.marketdetails.components.MarketChartSection
import org.coreypett.fullstack.features.marketdetails.components.MarketDataSection
import org.coreypett.fullstack.features.marketdetails.components.MarketSummarySection
import org.coreypett.fullstack.features.marketdetails.components.MarketTabs
import org.coreypett.fullstack.market.MR
import org.coreypett.fullstack.support.toComposeColor

@Composable
fun MarketDetailsRoute(
    features: MarketDetailsFeatures,
) {
    val orderBookSelection by features.orderBookFeature.selectionState.collectAsState()
    val candleSelection by features.candleChartFeature.selectionState.collectAsState()
    val summaryState by features.marketSummaryFeature.state.collectAsState()
    val candleState by features.candleChartFeature.state.collectAsState()
    val orderBookState by features.orderBookFeature.state.collectAsState()
    val tradeState by features.tradeFeature.state.collectAsState()
    var selectedPanel by rememberSaveable { mutableStateOf(MarketDetailsPanel.OrderBook) }

    DisposableEffect(features) {
        onDispose {
            features.orderBookFeature.close()
            features.candleChartFeature.close()
            features.marketSummaryFeature.close()
            features.tradeFeature.close()
        }
    }

    val state = remember(
        orderBookSelection,
        candleSelection,
        selectedPanel,
        summaryState,
        candleState,
        orderBookState,
        tradeState,
    ) {
        MarketDetailsUiState(
            selectedMarket = orderBookSelection.market,
            selectedInterval = candleSelection.interval,
            selectedPrecision = orderBookSelection.precision,
            selectedPanel = selectedPanel,
            summaryState = summaryState,
            candleState = candleState,
            orderBookState = orderBookState,
            tradeState = tradeState,
        )
    }

    MarketDetailsScreen(
        state = state,
        callbacks = MarketDetailsCallbacks(
            onMarketSelected = { market ->
                features.orderBookFeature.selectMarket(market)
                features.candleChartFeature.selectMarket(market)
                features.marketSummaryFeature.selectMarket(market)
                features.tradeFeature.selectMarket(market)
            },
            onIntervalSelected = features.candleChartFeature::selectInterval,
            onPrecisionSelected = features.orderBookFeature::selectPrecision,
            onPanelSelected = { selectedPanel = it },
        ),
    )
}

@Composable
fun MarketDetailsScreen(
    state: MarketDetailsUiState,
    callbacks: MarketDetailsCallbacks,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MR.colors.app_background.toComposeColor())
            .safeContentPadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        MarketTabs(
            selectedMarket = state.selectedMarket,
            onMarketSelected = callbacks.onMarketSelected,
        )
        MarketSummarySection(
            selectedMarket = state.selectedMarket,
            summaryState = state.summaryState,
            orderBookStatusLabel = state.orderBookState.statusLabel,
        )
        MarketChartSection(
            selectedInterval = state.selectedInterval,
            candleState = state.candleState,
            onIntervalSelected = callbacks.onIntervalSelected,
        )
        MarketDataSection(
            selectedPanel = state.selectedPanel,
            selectedPrecision = state.selectedPrecision,
            orderBookState = state.orderBookState,
            tradeState = state.tradeState,
            onPanelSelected = callbacks.onPanelSelected,
            onPrecisionSelected = callbacks.onPrecisionSelected,
        )
        Spacer(Modifier.height(8.dp))
    }
}

@Preview
@Composable
fun MarketDetailsPreview() {
    MaterialTheme {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MR.colors.app_background.toComposeColor(),
        ) {
            MarketDetailsScreen(
                state = MarketDetailsUiState.Preview,
                callbacks = MarketDetailsCallbacks.Empty,
            )
        }
    }
}
