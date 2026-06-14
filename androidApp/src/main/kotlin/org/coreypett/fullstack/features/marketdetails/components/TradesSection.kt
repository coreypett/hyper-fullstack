package org.coreypett.fullstack.features.marketdetails.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.coreypett.fullstack.market.MR
import org.coreypett.fullstack.market.trade.model.TradeSide
import org.coreypett.fullstack.market.trade.presentation.TradeRowDisplay
import org.coreypett.fullstack.market.trade.presentation.TradeViewState
import org.coreypett.fullstack.support.toComposeColor
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun TradesSection(
    viewState: TradeViewState,
) {
    if (!viewState.hasTrades) {
        EmptyState(message = viewState.centerMessage ?: "Loading trades")
        return
    }

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        TradesHeader()
        viewState.trades.take(14).forEach { trade ->
            TradeRow(trade = trade)
        }
    }
}

@Composable
private fun TradesHeader() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        HeaderCell("Side", TextAlign.Start)
        HeaderCell("Price", TextAlign.End)
        HeaderCell("Size", TextAlign.End)
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
private fun TradeRow(
    trade: TradeRowDisplay,
) {
    val sideColor = when (trade.side) {
        TradeSide.Buy -> MR.colors.bid.toComposeColor()
        TradeSide.Sell -> MR.colors.ask.toComposeColor()
        TradeSide.Unknown -> MR.colors.text_secondary.toComposeColor()
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(30.dp)
            .background(MR.colors.app_background.toComposeColor())
            .padding(horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = trade.sideText,
            modifier = Modifier.weight(1f),
            color = sideColor,
            fontSize = 12.sp,
            fontFamily = FontFamily.Monospace,
            maxLines = 1,
        )
        Text(
            text = trade.priceText,
            modifier = Modifier.weight(1f),
            color = sideColor,
            fontSize = 12.sp,
            fontFamily = FontFamily.Monospace,
            textAlign = TextAlign.End,
            maxLines = 1,
        )
        Text(
            text = trade.sizeText,
            modifier = Modifier.weight(1f),
            color = MR.colors.text_primary.toComposeColor(),
            fontSize = 12.sp,
            fontFamily = FontFamily.Monospace,
            textAlign = TextAlign.End,
            maxLines = 1,
        )
        Text(
            text = trade.timeMillis.toTradeTime(),
            modifier = Modifier.weight(1f),
            color = MR.colors.text_secondary.toComposeColor(),
            fontSize = 12.sp,
            fontFamily = FontFamily.Monospace,
            textAlign = TextAlign.End,
            maxLines = 1,
        )
    }
}

private fun Long.toTradeTime(): String {
    return SimpleDateFormat("HH:mm:ss", Locale.US).format(Date(this))
}
