package org.coreypett.fullstack.features.marketdetails.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.coreypett.fullstack.market.MR
import org.coreypett.fullstack.orderbook.model.LevelChange
import org.coreypett.fullstack.orderbook.model.OrderBookSide
import org.coreypett.fullstack.orderbook.presentation.OrderBookRowDisplay
import org.coreypett.fullstack.orderbook.presentation.OrderBookRowPair
import org.coreypett.fullstack.orderbook.presentation.OrderBookStatus
import org.coreypett.fullstack.orderbook.presentation.OrderBookViewState
import org.coreypett.fullstack.support.toComposeColor

@Composable
fun OrderBookSection(
    viewState: OrderBookViewState,
) {
    if (!viewState.hasRows) {
        if (viewState.status == OrderBookStatus.Failed) {
            EmptyState(message = viewState.centerMessage ?: "Loading order book")
        } else {
            OrderBookSkeleton()
        }
        return
    }

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(0.dp),
    ) {
        OrderBookHeader()
        SpreadRow(
            spreadText = viewState.spreadText ?: "--",
            spreadPercentText = viewState.spreadPercentText ?: "--",
        )
        viewState.pairedRows.take(MaxOrderBookRows).forEach { pair ->
            PairedOrderBookRow(
                pair = pair,
            )
        }
    }
}

@Composable
private fun OrderBookSkeleton() {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(0.dp),
    ) {
        OrderBookHeader()
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(38.dp)
                .background(MR.colors.app_panel.toComposeColor())
                .padding(horizontal = 4.dp),
            contentAlignment = Alignment.Center,
        ) {
            SkeletonBlock(
                width = 116.dp,
                height = 16.dp,
                cornerRadius = 4.dp,
            )
        }
        repeat(8) { seed ->
            SkeletonOrderBookRow(seed = seed)
        }
    }
}

@Composable
private fun SkeletonOrderBookRow(
    seed: Int,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(30.dp)
            .background(MR.colors.app_background.toComposeColor()),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(OrderBookTextCenterGap),
    ) {
        Row(
            modifier = Modifier
                .weight(1f)
                .padding(start = OrderBookOuterPadding, end = OrderBookCenterPadding),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            SkeletonCell(
                width = skeletonSizeWidth(seed),
                alignment = Alignment.CenterStart,
            )
            SkeletonCell(
                width = skeletonPriceWidth(seed),
                alignment = Alignment.CenterEnd,
            )
        }
        Row(
            modifier = Modifier
                .weight(1f)
                .padding(start = OrderBookCenterPadding, end = OrderBookOuterPadding),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            SkeletonCell(
                width = skeletonPriceWidth(seed + 2),
                alignment = Alignment.CenterStart,
            )
            SkeletonCell(
                width = skeletonSizeWidth(seed + 1),
                alignment = Alignment.CenterEnd,
            )
        }
    }
}

@Composable
private fun RowScope.SkeletonCell(
    width: Dp,
    alignment: Alignment,
) {
    Box(
        modifier = Modifier.weight(1f),
        contentAlignment = alignment,
    ) {
        SkeletonBlock(
            width = width,
            height = 13.dp,
            cornerRadius = 3.dp,
        )
    }
}

@Composable
private fun OrderBookHeader() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(OrderBookTextCenterGap),
    ) {
        Row(
            modifier = Modifier
                .weight(1f)
                .padding(start = OrderBookOuterPadding, end = OrderBookCenterPadding),
        ) {
            HeaderCell("Size", TextAlign.Start)
            HeaderCell("Price (Bid)", TextAlign.End)
        }
        Row(
            modifier = Modifier
                .weight(1f)
                .padding(start = OrderBookCenterPadding, end = OrderBookOuterPadding),
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
    pair: OrderBookRowPair,
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(30.dp),
    ) {
        DepthColumns(
            bid = pair.bid,
            ask = pair.ask,
            modifier = Modifier.matchParentSize(),
        )

        Row(
            modifier = Modifier.matchParentSize(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(OrderBookTextCenterGap),
        ) {
            BidColumns(
                level = pair.bid,
                modifier = Modifier.weight(1f),
            )
            AskColumns(
                level = pair.ask,
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun DepthColumns(
    bid: OrderBookRowDisplay?,
    ask: OrderBookRowDisplay?,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(OrderBookDepthCenterGap),
    ) {
        Row(
            modifier = Modifier
                .weight(1f)
                .padding(start = OrderBookOuterPadding, end = OrderBookCenterPadding),
        ) {
            PriceDepth(
                level = bid,
                side = OrderBookSide.Bid,
            )
        }
        Row(
            modifier = Modifier
                .weight(1f)
                .padding(start = OrderBookCenterPadding, end = OrderBookOuterPadding),
        ) {
            PriceDepth(
                level = ask,
                side = OrderBookSide.Ask,
            )
        }
    }
}

@Composable
private fun RowScope.PriceDepth(
    level: OrderBookRowDisplay?,
    side: OrderBookSide,
) {
    val sideColor = if (side == OrderBookSide.Bid) MR.colors.bid.toComposeColor() else MR.colors.ask.toComposeColor()
    val targetDepth = level?.depthFraction?.coerceIn(0f, 1f) ?: 0f
    BoxWithConstraints(
        modifier = Modifier
            .weight(1f)
            .fillMaxHeight()
    ) {
        if (targetDepth > 0f) {
            Box(
                modifier = Modifier
                    .align(if (side == OrderBookSide.Bid) Alignment.CenterEnd else Alignment.CenterStart)
                    .width(maxWidth * targetDepth)
                    .fillMaxHeight()
                    .background(sideColor.copy(alpha = 0.11f)),
            )
        }
    }
}

@Composable
private fun BidColumns(
    level: OrderBookRowDisplay?,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.padding(start = OrderBookOuterPadding, end = OrderBookCenterPadding),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        SizeText(
            level = level,
            side = OrderBookSide.Bid,
            textAlign = TextAlign.Start,
        )
        LevelText(
            text = level?.priceText,
            color = MR.colors.bid.toComposeColor(),
            textAlign = TextAlign.End,
        )
    }
}

@Composable
private fun AskColumns(
    level: OrderBookRowDisplay?,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.padding(start = OrderBookCenterPadding, end = OrderBookOuterPadding),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        LevelText(
            text = level?.priceText,
            color = MR.colors.ask.toComposeColor(),
            textAlign = TextAlign.Start,
        )
        SizeText(
            level = level,
            side = OrderBookSide.Ask,
            textAlign = TextAlign.End,
        )
    }
}

@Composable
private fun RowScope.SizeText(
    level: OrderBookRowDisplay?,
    side: OrderBookSide,
    textAlign: TextAlign,
) {
    val sideColor = if (side == OrderBookSide.Bid) MR.colors.bid.toComposeColor() else MR.colors.ask.toComposeColor()
    val textColor = MR.colors.text_primary.toComposeColor()
    val flashProgress = remember(level?.rowKey) { Animatable(0f) }

    LaunchedEffect(level?.rowKey, level?.sizeText, level?.change, level?.sizeChangeFraction) {
        val startProgress = when {
            level == null -> 0f
            level.change == LevelChange.None -> 0f
            level.sizeChangeFraction < OrderBookFlashMinChangeFraction -> 0f
            level.change == LevelChange.Up -> OrderBookFlashUpTextProgress
            else -> OrderBookFlashDownTextProgress
        }
        if (startProgress > 0f) {
            flashProgress.snapTo(startProgress)
            flashProgress.animateTo(
                targetValue = 0f,
                animationSpec = tween(durationMillis = 140, easing = FastOutSlowInEasing),
            )
        } else {
            flashProgress.snapTo(0f)
        }
    }

    Box(
        modifier = Modifier
            .weight(1f)
            .fillMaxHeight(),
        contentAlignment = if (textAlign == TextAlign.End) Alignment.CenterEnd else Alignment.CenterStart,
    ) {
        Text(
            text = level?.sizeText ?: "",
            color = lerp(textColor, sideColor, flashProgress.value),
            fontSize = 13.sp,
            fontFamily = FontFamily.Monospace,
            textAlign = textAlign,
            maxLines = 1,
        )
    }
}

@Composable
private fun RowScope.LevelText(
    text: String?,
    color: Color,
    textAlign: TextAlign,
) {
    Text(
        text = text ?: "",
        modifier = Modifier.weight(1f),
        color = color,
        fontSize = 13.sp,
        fontFamily = FontFamily.Monospace,
        textAlign = textAlign,
        maxLines = 1,
    )
}

@Composable
private fun SpreadRow(
    spreadText: String,
    spreadPercentText: String,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(38.dp)
            .background(MR.colors.app_panel.toComposeColor())
            .padding(horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(OrderBookSpreadItemGap, Alignment.CenterHorizontally),
    ) {
        Text(
            text = "Spread",
            color = MR.colors.text_tertiary.toComposeColor(),
            fontSize = 12.sp,
            maxLines = 1,
        )
        Text(
            text = spreadText,
            color = MR.colors.brand_orange.toComposeColor(),
            fontSize = 15.sp,
            fontWeight = FontWeight.SemiBold,
            fontFamily = FontFamily.Monospace,
            maxLines = 1,
        )
        Text(
            text = spreadPercentText,
            color = MR.colors.text_tertiary.toComposeColor(),
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
            fontFamily = FontFamily.Monospace,
            maxLines = 1,
        )
    }
}

private val OrderBookTextCenterGap = 8.dp
private val OrderBookDepthCenterGap = 0.dp
private val OrderBookCenterPadding = 0.dp
private val OrderBookOuterPadding = 4.dp
private val OrderBookSpreadItemGap = 12.dp
private const val OrderBookFlashMinChangeFraction = 0.08f
private const val OrderBookFlashUpTextProgress = 0.88f
private const val OrderBookFlashDownTextProgress = 0.78f
private const val MaxOrderBookRows = 12

private val SkeletonPriceWidths = listOf(72.dp, 84.dp, 64.dp, 78.dp)
private val SkeletonSizeWidths = listOf(34.dp, 46.dp, 38.dp, 52.dp)

private fun skeletonPriceWidth(seed: Int): Dp = SkeletonPriceWidths[seed % SkeletonPriceWidths.size]

private fun skeletonSizeWidth(seed: Int): Dp = SkeletonSizeWidths[seed % SkeletonSizeWidths.size]
