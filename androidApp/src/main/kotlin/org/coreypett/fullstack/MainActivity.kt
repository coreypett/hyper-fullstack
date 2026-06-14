package org.coreypett.fullstack

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import org.coreypett.fullstack.di.SharedDependencyGraph
import org.coreypett.fullstack.features.marketdetails.MarketDetailsFeatures
import org.coreypett.fullstack.features.marketdetails.MarketDetailsPreview

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        setTheme(R.style.Theme_Fullstack)
        SharedDependencyGraph.start()
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        val features = MarketDetailsFeatures(
            orderBookFeature = SharedDependencyGraph.orderBookFeature(),
            candleChartFeature = SharedDependencyGraph.candleChartFeature(),
            marketSummaryFeature = SharedDependencyGraph.marketSummaryFeature(),
            recentTradesFeature = SharedDependencyGraph.recentTradesFeature(),
        )
        setContent {
            App(features = features)
        }
    }
}

@Preview
@Composable
fun AppAndroidPreview() {
    MarketDetailsPreview()
}
