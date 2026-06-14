package org.coreypett.fullstack.features.marketdetails.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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

@Composable
fun MarketSummarySection(
    selectedMarket: MarketSymbol,
    summaryState: MarketSummaryViewState,
    modifier: Modifier = Modifier,
) {
    val midPriceText = summaryState.midPriceText

    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(MR.colors.app_background.toComposeColor())
            .padding(vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Column(
            modifier = Modifier
                .weight(1f)
                .defaultMinSize(minHeight = 84.dp),
            verticalArrangement = Arrangement.Center,
        ) {
            Text(
                text = "${selectedMarket.displayName}-USDC",
                color = MR.colors.text_tertiary.toComposeColor(),
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )

            Spacer(Modifier.height(3.dp))

            if (midPriceText == null) {
                SkeletonBlock(
                    width = 210.dp,
                    height = 42.dp,
                    cornerRadius = 6.dp,
                )
            } else {
                MidPriceText(text = midPriceText)
            }

            if (summaryState.priceChangeText == null || summaryState.priceChangePercentText == null) {
                SkeletonBlock(
                    width = 132.dp,
                    height = 18.dp,
                    cornerRadius = 4.dp,
                )
            } else {
                PriceChangeRow(summaryState = summaryState)
            }
        }

        Column(
            modifier = Modifier
                .width(142.dp)
                .defaultMinSize(minHeight = 84.dp),
            verticalArrangement = Arrangement.spacedBy(0.dp, Alignment.CenterVertically),
            horizontalAlignment = Alignment.End,
        ) {
            val stats = listOf(
                "24h Vol" to summaryState.volume24hText,
                "24h High" to summaryState.high24hText,
                "24h Low" to summaryState.low24hText,
                "Open Int." to summaryState.openInterestText,
            )
            stats.forEach { (label, value) ->
                SummaryMetricRow(label = label, value = value)
            }
        }
    }
}

@Composable
private fun PriceChangeRow(
    summaryState: MarketSummaryViewState,
) {
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
            text = summaryState.priceChangeText.orEmpty(),
            color = changeColor,
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold,
            fontFamily = FontFamily.Monospace,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Text(
            text = summaryState.priceChangePercentText.orEmpty(),
            color = changeColor,
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold,
            fontFamily = FontFamily.Monospace,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun MidPriceText(
    text: String,
) {
    var fontSize by remember(text) { mutableStateOf(36.sp) }

    Text(
        text = text,
        color = MR.colors.text_primary.toComposeColor(),
        fontSize = fontSize,
        fontWeight = FontWeight.SemiBold,
        fontFamily = FontFamily.Monospace,
        maxLines = 1,
        softWrap = false,
        overflow = TextOverflow.Clip,
        onTextLayout = { result ->
            if (result.hasVisualOverflow && fontSize > 24.sp) {
                fontSize = (fontSize.value - 1f).coerceAtLeast(24f).sp
            }
        },
    )
}

@Composable
private fun SummaryMetricRow(
    label: String,
    value: String?,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = label,
            color = MR.colors.text_tertiary.toComposeColor(),
            fontSize = 10.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Spacer(Modifier.width(8.dp))
        if (value == null) {
            Box(
                modifier = Modifier.weight(1f),
                contentAlignment = Alignment.CenterEnd,
            ) {
                SkeletonBlock(
                    width = 58.dp,
                    height = 13.dp,
                    cornerRadius = 3.dp,
                )
            }
        } else {
            Text(
                text = value,
                color = MR.colors.text_primary.toComposeColor(),
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                fontFamily = FontFamily.Monospace,
                textAlign = TextAlign.End,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
        }
    }
}
