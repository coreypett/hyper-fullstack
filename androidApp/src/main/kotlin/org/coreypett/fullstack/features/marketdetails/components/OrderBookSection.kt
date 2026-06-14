package org.coreypett.fullstack.features.marketdetails.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
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
        verticalArrangement = Arrangement.spacedBy(0.dp),
    ) {
        OrderBookHeader()
        SpreadRow(spreadText = viewState.spreadText ?: "--")
        repeat(rowCount) { index ->
            PairedOrderBookRow(
                bid = bids.getOrNull(index),
                ask = asks.getOrNull(index),
            )
        }
    }
}

@Composable
private fun OrderBookHeader() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(OrderBookPriceGap),
    ) {
        Row(
            modifier = Modifier
                .weight(1f)
                .padding(start = 4.dp, end = 6.dp),
        ) {
            HeaderCell("Size", TextAlign.Start)
            HeaderCell("Price (Bid)", TextAlign.End)
        }
        Row(
            modifier = Modifier
                .weight(1f)
                .padding(start = 6.dp, end = 4.dp),
        ) {
            HeaderCell("Price (Ask)", TextAlign.Start)
            HeaderCell("Size", TextAlign.End)
        }
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
        horizontalArrangement = Arrangement.spacedBy(OrderBookPriceGap),
    ) {
        BidColumns(
            level = bid,
            modifier = Modifier.weight(1f),
        )
        AskColumns(
            level = ask,
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun BidColumns(
    level: OrderBookRowDisplay?,
    modifier: Modifier = Modifier,
) {
    OrderBookSideColumns(
        level = level,
        side = OrderBookSide.Bid,
        modifier = modifier.padding(start = 4.dp, end = 6.dp),
    )
}

@Composable
private fun AskColumns(
    level: OrderBookRowDisplay?,
    modifier: Modifier = Modifier,
) {
    OrderBookSideColumns(
        level = level,
        side = OrderBookSide.Ask,
        modifier = modifier.padding(start = 6.dp, end = 4.dp),
    )
}

@Composable
private fun OrderBookSideColumns(
    level: OrderBookRowDisplay?,
    side: OrderBookSide,
    modifier: Modifier = Modifier,
) {
    val sideColor = if (side == OrderBookSide.Bid) MR.colors.bid.toComposeColor() else MR.colors.ask.toComposeColor()
    val flashColor = when (level?.change) {
        LevelChange.Up -> MR.colors.up_flash.toComposeColor()
        LevelChange.Down -> MR.colors.down_flash.toComposeColor()
        LevelChange.None,
        null,
        -> Color.Transparent
    }
    val animatedFlash by animateColorAsState(flashColor, label = "order-book-flash")

    Row(
        modifier = modifier
            .fillMaxHeight()
            .background(animatedFlash),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (side == OrderBookSide.Bid) {
            SizeText(level = level, textAlign = TextAlign.Start)
            PriceDepthText(
                level = level,
                side = side,
                color = sideColor,
                textAlign = TextAlign.End,
            )
        } else {
            PriceDepthText(
                level = level,
                side = side,
                color = sideColor,
                textAlign = TextAlign.Start,
            )
            SizeText(level = level, textAlign = TextAlign.End)
        }
    }
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
private fun RowScope.PriceDepthText(
    level: OrderBookRowDisplay?,
    side: OrderBookSide,
    color: Color,
    textAlign: TextAlign,
) {
    BoxWithConstraints(
        modifier = Modifier
            .weight(1f)
            .fillMaxHeight(),
    ) {
        if (level != null) {
            Box(
                modifier = Modifier
                    .align(if (side == OrderBookSide.Bid) Alignment.CenterEnd else Alignment.CenterStart)
                    .width(maxWidth * level.depthFraction.coerceIn(0f, 1f))
                    .fillMaxHeight()
                    .background(color.copy(alpha = 0.11f)),
            )
        }
        Text(
            text = level?.priceText ?: "",
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.Center),
            color = if (level == null) MR.colors.text_tertiary.toComposeColor() else color,
            fontSize = 13.sp,
            fontFamily = FontFamily.Monospace,
            textAlign = textAlign,
            maxLines = 1,
        )
    }
}

@Composable
private fun SpreadRow(
    spreadText: String,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(38.dp)
            .background(MR.colors.app_panel.toComposeColor())
            .padding(horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
    ) {
        Text(
            text = "Spread",
            color = MR.colors.text_tertiary.toComposeColor(),
            fontSize = 12.sp,
        )
        Spacer(Modifier.width(8.dp))
        Text(
            text = spreadText,
            color = MR.colors.brand_orange.toComposeColor(),
            fontSize = 15.sp,
            fontWeight = FontWeight.SemiBold,
            fontFamily = FontFamily.Monospace,
            maxLines = 1,
        )
    }
}

private val OrderBookPriceGap = 8.dp
