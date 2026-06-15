package org.coreypett.fullstack.features.marketdetails.components

import android.annotation.SuppressLint
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.draw.clip
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.coreypett.fullstack.market.MR
import org.coreypett.fullstack.market.model.MarketSymbol
import org.coreypett.fullstack.market.trade.model.RecentTradesSide
import org.coreypett.fullstack.market.trade.presentation.RecentTradesRowDisplay
import org.coreypett.fullstack.market.trade.presentation.RecentTradesStatus
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
        if (viewState.status == RecentTradesStatus.Failed) {
            EmptyState(message = viewState.centerMessage ?: "Loading trades")
        } else {
            RecentTradesSkeleton(market = market)
        }
        return
    }

    var selectedExplorerUrl by remember { mutableStateOf<String?>(null) }

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(0.dp),
    ) {
        RecentTradesHeader(market = market)
        viewState.recentTrades.take(18).forEach { recentTrade ->
            key(recentTrade.animationKey()) {
                RecentTradesAnimatedRow(
                    recentTrade = recentTrade,
                    onOpenTransaction = {
                        selectedExplorerUrl = recentTrade.hyperliquidExplorerUrl()
                    },
                )
            }
        }
    }

    selectedExplorerUrl?.let { url ->
        HyperliquidExplorerDialog(
            url = url,
            onDismiss = { selectedExplorerUrl = null },
        )
    }
}

@Composable
private fun RecentTradesSkeleton(
    market: MarketSymbol,
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(0.dp),
    ) {
        RecentTradesHeader(market = market)
        repeat(7) { seed ->
            SkeletonRecentTradeRow(seed = seed)
        }
    }
}

@Composable
private fun SkeletonRecentTradeRow(
    seed: Int,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(30.dp)
            .background(MR.colors.app_background.toComposeColor())
            .padding(horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        SkeletonCell(
            width = recentTradePriceSkeletonWidth(seed),
            alignment = Alignment.CenterStart,
        )
        Box(
            modifier = Modifier.weight(1f),
            contentAlignment = Alignment.Center,
        ) {
            Box(
                modifier = Modifier.width(RecentTradesSizeColumnWidth),
                contentAlignment = Alignment.CenterStart,
            ) {
                SkeletonBlock(
                    width = recentTradeSizeSkeletonWidth(seed),
                    height = 13.dp,
                    cornerRadius = 3.dp,
                )
            }
        }
        SkeletonCell(
            width = 58.dp,
            alignment = Alignment.CenterEnd,
        )
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
private fun RecentTradesAnimatedRow(
    recentTrade: RecentTradesRowDisplay,
    onOpenTransaction: () -> Unit,
) {
    val animationKey = recentTrade.animationKey()
    var isVisible by remember(animationKey) { mutableStateOf(false) }
    val flashAlpha = remember(animationKey) { Animatable(RecentTradeFlashAlpha) }

    LaunchedEffect(animationKey) {
        isVisible = true
        flashAlpha.snapTo(RecentTradeFlashAlpha)
        flashAlpha.animateTo(
            targetValue = 0f,
            animationSpec = tween(durationMillis = 850),
        )
    }

    AnimatedVisibility(
        visible = isVisible,
        enter = slideInVertically(
            animationSpec = tween(durationMillis = 260),
            initialOffsetY = { -it / 2 },
        ) + expandVertically(
            animationSpec = tween(durationMillis = 260),
        ) + fadeIn(
            animationSpec = tween(durationMillis = 180),
        ),
        exit = slideOutVertically(
            animationSpec = tween(durationMillis = 180),
            targetOffsetY = { it / 3 },
        ) + shrinkVertically(
            animationSpec = tween(durationMillis = 180),
        ) + fadeOut(
            animationSpec = tween(durationMillis = 120),
        ),
    ) {
        RecentTradesRow(
            recentTrade = recentTrade,
            flashAlpha = flashAlpha.value,
            onOpenTransaction = onOpenTransaction,
        )
    }
}

@Composable
private fun RecentTradesRow(
    recentTrade: RecentTradesRowDisplay,
    flashAlpha: Float,
    onOpenTransaction: () -> Unit,
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
            .background(sideColor.copy(alpha = flashAlpha))
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
        Row(
            modifier = Modifier.weight(1f),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.End),
        ) {
            Text(
                text = recentTrade.timeMillis.toTradeTime(),
                color = MR.colors.text_secondary.toComposeColor(),
                fontSize = 13.sp,
                fontFamily = FontFamily.Monospace,
                textAlign = TextAlign.End,
                maxLines = 1,
            )
            TransactionExplorerButton(onClick = onOpenTransaction)
        }
    }
}

@Composable
private fun TransactionExplorerButton(
    onClick: () -> Unit,
) {
    val iconColor = MR.colors.text_secondary.toComposeColor()
    Box(
        modifier = Modifier
            .size(22.dp)
            .clip(CircleShape)
            .background(MR.colors.app_panel.toComposeColor().copy(alpha = 0.82f))
            .clickable(onClick = onClick)
            .semantics { contentDescription = "Open transaction in Hyperliquid explorer" },
        contentAlignment = Alignment.Center,
    ) {
        Canvas(modifier = Modifier.size(12.dp)) {
            val strokeWidth = 1.5.dp.toPx()
            val inset = 2.dp.toPx()
            val start = androidx.compose.ui.geometry.Offset(inset, size.height - inset)
            val end = androidx.compose.ui.geometry.Offset(size.width - inset, inset)

            drawLine(
                color = iconColor,
                start = start,
                end = end,
                strokeWidth = strokeWidth,
                cap = StrokeCap.Round,
            )
            drawLine(
                color = iconColor,
                start = androidx.compose.ui.geometry.Offset(size.width - inset, inset),
                end = androidx.compose.ui.geometry.Offset(size.width - inset, size.height * 0.42f),
                strokeWidth = strokeWidth,
                cap = StrokeCap.Round,
            )
            drawLine(
                color = iconColor,
                start = androidx.compose.ui.geometry.Offset(size.width - inset, inset),
                end = androidx.compose.ui.geometry.Offset(size.width * 0.58f, inset),
                strokeWidth = strokeWidth,
                cap = StrokeCap.Round,
            )
        }
    }
}

@SuppressLint("SetJavaScriptEnabled")
@Composable
private fun HyperliquidExplorerDialog(
    url: String,
    onDismiss: () -> Unit,
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth(0.94f)
                .fillMaxHeight(0.86f)
                .clip(RoundedCornerShape(12.dp))
                .background(MR.colors.app_background.toComposeColor()),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(44.dp)
                    .background(MR.colors.app_panel.toComposeColor())
                    .padding(start = 14.dp, end = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                val closeColor = MR.colors.text_secondary.toComposeColor()
                Text(
                    text = "Hyperliquid",
                    modifier = Modifier.weight(1f),
                    color = MR.colors.text_primary.toComposeColor(),
                    fontSize = 14.sp,
                    maxLines = 1,
                )
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .clickable(onClick = onDismiss)
                        .semantics { contentDescription = "Close explorer" },
                    contentAlignment = Alignment.Center,
                ) {
                    Canvas(modifier = Modifier.size(12.dp)) {
                        val strokeWidth = 1.5.dp.toPx()
                        drawLine(
                            color = closeColor,
                            start = androidx.compose.ui.geometry.Offset(0f, 0f),
                            end = androidx.compose.ui.geometry.Offset(size.width, size.height),
                            strokeWidth = strokeWidth,
                            cap = StrokeCap.Round,
                        )
                        drawLine(
                            color = closeColor,
                            start = androidx.compose.ui.geometry.Offset(size.width, 0f),
                            end = androidx.compose.ui.geometry.Offset(0f, size.height),
                            strokeWidth = strokeWidth,
                            cap = StrokeCap.Round,
                        )
                    }
                }
            }

            AndroidView(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                factory = { context ->
                    WebView(context).apply {
                        webViewClient = WebViewClient()
                        settings.javaScriptEnabled = true
                        settings.domStorageEnabled = true
                        loadUrl(url)
                    }
                },
                update = { webView ->
                    if (webView.url != url) {
                        webView.loadUrl(url)
                    }
                },
            )
        }
    }
}

private fun Long.toTradeTime(): String {
    return SimpleDateFormat("HH:mm:ss", Locale.US).format(Date(this))
}

private fun RecentTradesRowDisplay.hyperliquidExplorerUrl(): String =
    "https://app.hyperliquid.xyz/explorer/tx/$transactionHash"

private fun RecentTradesRowDisplay.animationKey(): String =
    "$tradeId-$transactionHash"

private val RecentTradesSizeColumnWidth = 64.dp
private const val RecentTradeFlashAlpha = 0.22f

private val RecentTradesPriceSkeletonWidths = listOf(72.dp, 84.dp, 64.dp, 78.dp)
private val RecentTradesSizeSkeletonWidths = listOf(34.dp, 46.dp, 38.dp, 52.dp)

private fun recentTradePriceSkeletonWidth(seed: Int): Dp =
    RecentTradesPriceSkeletonWidths[seed % RecentTradesPriceSkeletonWidths.size]

private fun recentTradeSizeSkeletonWidth(seed: Int): Dp =
    RecentTradesSizeSkeletonWidths[seed % RecentTradesSizeSkeletonWidths.size]
