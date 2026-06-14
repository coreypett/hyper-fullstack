package org.coreypett.fullstack.support

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import dev.icerock.moko.resources.ColorResource
import dev.icerock.moko.resources.ImageResource
import org.coreypett.fullstack.market.MR
import org.coreypett.fullstack.market.model.MarketSymbol

@Composable
fun ColorResource.toComposeColor(): Color {
    return Color(getColor(LocalContext.current))
}

fun MarketSymbol.tokenIconResource(): ImageResource = when (this) {
    MarketSymbol.BTC -> MR.images.bitcoin_token
    MarketSymbol.ETH -> MR.images.ethereum_token
    MarketSymbol.SOL -> MR.images.solana_token
}
