package org.coreypett.fullstack.features.marketdetails.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.coreypett.fullstack.market.MR
import org.coreypett.fullstack.market.model.MarketSymbol
import org.coreypett.fullstack.market.trade.model.RecentTradesSide
import org.coreypett.fullstack.market.trade.presentation.RecentTradesRowDisplay
import org.coreypett.fullstack.market.trade.presentation.RecentTradesViewState
import org.coreypett.fullstack.support.toComposeColor
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun RecentTradesSection(
    market: MarketSymbol,
    viewState: RecentTradesViewState,
) {
    if (!viewState.hasTrades) {
        EmptyState(message = viewState.centerMessage ?: "Loading trades")
        return
    }

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(0.dp),
    ) {
        RecentTradesHeader(market = market)
        viewState.recentTrades.take(18).forEach { recentTrade ->
            RecentTradesRow(recentTrade = recentTrade)
        }
    }
}

@Composable
private fun RecentTradesHeader(
    market: MarketSymbol,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        HeaderCell("Price(USD)", TextAlign.Start)
        SizeHeaderCell("Size (${market.displayName})")
        HeaderCell("Time", TextAlign.End)
    }
}

@Composable
private fun RowScope.HeaderCell(
    text: String,
    textAlign: TextAlign,
) {
    Text(
        text = text,
        modifier = Modifier.weight(1f),
        color = MR.colors.text_tertiary.toComposeColor(),
        fontSize = 11.sp,
        textAlign = textAlign,
    )
}

@Composable
private fun RowScope.SizeHeaderCell(
    text: String,
) {
    Box(
        modifier = Modifier.weight(1f),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text,
            modifier = Modifier.width(RecentTradesSizeColumnWidth),
            color = MR.colors.text_tertiary.toComposeColor(),
            fontSize = 11.sp,
            textAlign = TextAlign.Start,
            maxLines = 1,
        )
    }
}

@Composable
private fun RecentTradesRow(
    recentTrade: RecentTradesRowDisplay,
) {
    val sideColor = when (recentTrade.side) {
        RecentTradesSide.Buy -> MR.colors.bid.toComposeColor()
        RecentTradesSide.Sell -> MR.colors.ask.toComposeColor()
        RecentTradesSide.Unknown -> MR.colors.text_secondary.toComposeColor()
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(30.dp)
            .background(MR.colors.app_background.toComposeColor())
            .padding(horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Text(
            text = recentTrade.priceText,
            modifier = Modifier.weight(1f),
            color = sideColor,
            fontSize = 13.sp,
            fontFamily = FontFamily.Monospace,
            textAlign = TextAlign.Start,
            maxLines = 1,
        )
        Box(
            modifier = Modifier.weight(1f),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = recentTrade.sizeText,
                modifier = Modifier.width(RecentTradesSizeColumnWidth),
                color = MR.colors.text_primary.toComposeColor(),
                fontSize = 13.sp,
                fontFamily = FontFamily.Monospace,
                textAlign = TextAlign.Start,
                maxLines = 1,
            )
        }
        Text(
            text = recentTrade.timeMillis.toTradeTime(),
            modifier = Modifier.weight(1f),
            color = MR.colors.text_secondary.toComposeColor(),
            fontSize = 13.sp,
            fontFamily = FontFamily.Monospace,
            textAlign = TextAlign.End,
            maxLines = 1,
        )
    }
}

private fun Long.toTradeTime(): String {
    return SimpleDateFormat("HH:mm:ss", Locale.US).format(Date(this))
}

private val RecentTradesSizeColumnWidth = 64.dp
