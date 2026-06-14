package org.coreypett.fullstack.features.marketdetails.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.coreypett.fullstack.market.MR
import org.coreypett.fullstack.market.model.MarketSymbol
import org.coreypett.fullstack.market.summary.model.MarketSummaryChangeDirection
import org.coreypett.fullstack.market.summary.model.MarketSummaryViewState
import org.coreypett.fullstack.support.toComposeColor
import org.coreypett.fullstack.support.tokenIconResource

@Composable
fun MarketSummarySection(
    selectedMarket: MarketSymbol,
    summaryState: MarketSummaryViewState,
    orderBookStatusLabel: String,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(MR.colors.app_background.toComposeColor()),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Image(
                    painter = painterResource(id = selectedMarket.tokenIconResource().drawableResId),
                    contentDescription = null,
                    modifier = Modifier.size(34.dp),
                )
                Column(
                    modifier = Modifier.weight(1f, fill = false),
                ) {
                    Text(
                        text = "${selectedMarket.displayName}-USD",
                        color = MR.colors.text_primary.toComposeColor(),
                        fontSize = 24.sp,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        text = orderBookStatusLabel,
                        color = MR.colors.text_tertiary.toComposeColor(),
                        fontSize = 12.sp,
                    )
                }
            }
            Text(
                text = "Hyperliquid",
                color = MR.colors.brand_orange.toComposeColor(),
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
            )
        }

        Spacer(Modifier.height(16.dp))

        Text(
            text = summaryState.midPriceText ?: "--",
            color = MR.colors.text_primary.toComposeColor(),
            fontSize = 38.sp,
            fontWeight = FontWeight.SemiBold,
            fontFamily = FontFamily.Monospace,
            maxLines = 1,
        )
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            val changeColor = when (summaryState.changeDirection) {
                MarketSummaryChangeDirection.Up -> MR.colors.bid.toComposeColor()
                MarketSummaryChangeDirection.Down -> MR.colors.ask.toComposeColor()
                MarketSummaryChangeDirection.Flat -> MR.colors.text_secondary.toComposeColor()
            }
            Text(
                text = summaryState.priceChangeText ?: "--",
                color = changeColor,
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
                fontFamily = FontFamily.Monospace,
            )
            Text(
                text = summaryState.priceChangePercentText ?: "--",
                color = changeColor,
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
                fontFamily = FontFamily.Monospace,
            )
        }

        Spacer(Modifier.height(16.dp))

        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                SummaryMetric(
                    label = "24h Vol",
                    value = summaryState.volume24hText ?: "--",
                    modifier = Modifier.weight(1f),
                )
                SummaryMetric(
                    label = "24h High",
                    value = summaryState.high24hText ?: "--",
                    modifier = Modifier.weight(1f),
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                SummaryMetric(
                    label = "24h Low",
                    value = summaryState.low24hText ?: "--",
                    modifier = Modifier.weight(1f),
                )
                SummaryMetric(
                    label = "Open Int",
                    value = summaryState.openInterestText ?: "--",
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

@Composable
private fun SummaryMetric(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier) {
        Text(
            text = label,
            color = MR.colors.text_tertiary.toComposeColor(),
            fontSize = 11.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Text(
            text = value,
            color = MR.colors.text_primary.toComposeColor(),
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium,
            fontFamily = FontFamily.Monospace,
            textAlign = TextAlign.Start,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}
