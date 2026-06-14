package org.coreypett.fullstack

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import org.coreypett.fullstack.features.marketdetails.MarketDetailsFeatures
import org.coreypett.fullstack.features.marketdetails.MarketDetailsRoute
import org.coreypett.fullstack.market.MR
import org.coreypett.fullstack.support.toComposeColor

@Composable
fun App(
    features: MarketDetailsFeatures,
) {
    MaterialTheme {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MR.colors.app_background.toComposeColor(),
        ) {
            MarketDetailsRoute(features = features)
        }
    }
}
