package org.coreypett.fullstack.features.marketdetails.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.coreypett.fullstack.market.MR
import org.coreypett.fullstack.orderbook.model.LevelChange
import org.coreypett.fullstack.orderbook.model.OrderBookSide
import org.coreypett.fullstack.orderbook.presentation.OrderBookRowDisplay
import org.coreypett.fullstack.orderbook.presentation.OrderBookViewState
import org.coreypett.fullstack.support.toComposeColor

@Composable
fun OrderBookSection(
    viewState: OrderBookViewState,
) {
    if (!viewState.hasRows) {
        EmptyState(message = viewState.centerMessage ?: "Loading order book")
        return
    }

    val asks = viewState.asks.asReversed()
    val bids = viewState.bids
    val rowCount = maxOf(asks.size, bids.size).coerceAtMost(12)

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        OrderBookHeader()
        repeat(rowCount) { index ->
            PairedOrderBookRow(
                bid = bids.getOrNull(index),
                ask = asks.getOrNull(index),
            )
        }
        SpreadRow(spreadText = viewState.spreadText ?: "--")
    }
}

@Composable
private fun OrderBookHeader() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 2.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        HeaderCell("Bid", TextAlign.Start)
        HeaderCell("Size", TextAlign.End)
        HeaderCell("Size", TextAlign.Start)
        HeaderCell("Ask", TextAlign.End)
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
private fun PairedOrderBookRow(
    bid: OrderBookRowDisplay?,
    ask: OrderBookRowDisplay?,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(30.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        OrderBookSideCell(
            level = bid,
            side = OrderBookSide.Bid,
            modifier = Modifier.weight(1f),
        )
        OrderBookSideCell(
            level = ask,
            side = OrderBookSide.Ask,
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun OrderBookSideCell(
    level: OrderBookRowDisplay?,
    side: OrderBookSide,
    modifier: Modifier = Modifier,
) {
    val sideColor = if (side == OrderBookSide.Bid) {
        MR.colors.bid.toComposeColor()
    } else {
        MR.colors.ask.toComposeColor()
    }
    val flashColor = when (level?.change) {
        LevelChange.Up -> MR.colors.up_flash.toComposeColor()
        LevelChange.Down -> MR.colors.down_flash.toComposeColor()
        LevelChange.None,
        null,
        -> Color.Transparent
    }
    val animatedFlash by animateColorAsState(flashColor, label = "order-book-flash")

    BoxWithConstraints(
        modifier = modifier
            .fillMaxHeight()
            .background(animatedFlash),
    ) {
        if (level != null) {
            Box(
                modifier = Modifier
                    .align(if (side == OrderBookSide.Bid) Alignment.CenterStart else Alignment.CenterEnd)
                    .width(maxWidth * level.depthFraction.coerceIn(0f, 1f))
                    .fillMaxHeight()
                    .background(sideColor.copy(alpha = 0.13f)),
            )
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight()
                .padding(horizontal = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            if (side == OrderBookSide.Bid) {
                PriceText(level = level, color = sideColor, textAlign = TextAlign.Start)
                SizeText(level = level, textAlign = TextAlign.End)
            } else {
                SizeText(level = level, textAlign = TextAlign.Start)
                PriceText(level = level, color = sideColor, textAlign = TextAlign.End)
            }
        }
    }
}

@Composable
private fun RowScope.PriceText(
    level: OrderBookRowDisplay?,
    color: Color,
    textAlign: TextAlign,
) {
    Text(
        text = level?.priceText ?: "--",
        modifier = Modifier.weight(1f),
        color = if (level == null) MR.colors.text_tertiary.toComposeColor() else color,
        fontSize = 12.sp,
        fontFamily = FontFamily.Monospace,
        textAlign = textAlign,
        maxLines = 1,
    )
}

@Composable
private fun RowScope.SizeText(
    level: OrderBookRowDisplay?,
    textAlign: TextAlign,
) {
    Text(
        text = level?.sizeText ?: "--",
        modifier = Modifier.weight(1f),
        color = MR.colors.text_primary.toComposeColor(),
        fontSize = 12.sp,
        fontFamily = FontFamily.Monospace,
        textAlign = textAlign,
        maxLines = 1,
    )
}

@Composable
private fun SpreadRow(
    spreadText: String,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(34.dp)
            .background(MR.colors.app_background.toComposeColor())
            .padding(horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = "Spread",
            modifier = Modifier.weight(1f),
            color = MR.colors.text_tertiary.toComposeColor(),
            fontSize = 12.sp,
        )
        Text(
            text = spreadText,
            modifier = Modifier.weight(1f),
            color = MR.colors.brand_orange.toComposeColor(),
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold,
            fontFamily = FontFamily.Monospace,
            textAlign = TextAlign.End,
            maxLines = 1,
        )
    }
}
