package org.coreypett.fullstack

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeContentPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.flow.MutableStateFlow
import org.coreypett.fullstack.orderbook.model.LevelChange
import org.coreypett.fullstack.orderbook.model.MarketSymbol
import org.coreypett.fullstack.orderbook.model.OrderBookLevel
import org.coreypett.fullstack.orderbook.model.OrderBookSelection
import org.coreypett.fullstack.orderbook.model.OrderBookSide
import org.coreypett.fullstack.orderbook.model.OrderBookSnapshot
import org.coreypett.fullstack.orderbook.model.OrderBookUiState
import org.coreypett.fullstack.orderbook.model.PricePrecision
import org.coreypett.fullstack.orderbook.repository.OrderBookRepository

@Composable
@Preview
fun App() {
    MaterialTheme {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = AppColors.Background,
        ) {
            var selection by remember { mutableStateOf(OrderBookSelection()) }
            val selectionFlow = remember { MutableStateFlow(selection) }
            val repository = remember { OrderBookRepository() }
            val uiState by remember(repository) {
                repository.states(selectionFlow)
            }.collectAsState(OrderBookUiState.Connecting)

            LaunchedEffect(selection) {
                selectionFlow.value = selection
            }

            OrderBookScreen(
                selection = selection,
                uiState = uiState,
                onSelectionChange = { selection = it },
            )
        }
    }
}

@Composable
private fun OrderBookScreen(
    selection: OrderBookSelection,
    uiState: OrderBookUiState,
    onSelectionChange: (OrderBookSelection) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .safeContentPadding()
            .padding(horizontal = 16.dp, vertical = 10.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column {
                Text(
                    text = "${selection.market.displayName}-USD",
                    color = AppColors.TextPrimary,
                    fontSize = 28.sp,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    text = stateLabel(uiState),
                    color = stateColor(uiState),
                    fontSize = 12.sp,
                )
            }
            Text(
                text = "Hyperliquid",
                color = AppColors.Accent,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
            )
        }

        Spacer(Modifier.height(16.dp))

        SegmentedSelector(
            values = MarketSymbol.entries.toList(),
            selected = selection.market,
            label = MarketSymbol::displayName,
            onSelected = { onSelectionChange(selection.copy(market = it)) },
        )

        Spacer(Modifier.height(10.dp))

        SegmentedSelector(
            values = PricePrecision.entries.toList(),
            selected = selection.precision,
            label = { it.nSigFigs.toString() },
            onSelected = { onSelectionChange(selection.copy(precision = it)) },
        )

        Spacer(Modifier.height(14.dp))

        when (uiState) {
            OrderBookUiState.Connecting -> CenterMessage("Connecting")
            is OrderBookUiState.Failed -> CenterMessage(uiState.message)
            is OrderBookUiState.Live -> OrderBook(snapshot = uiState.snapshot)
        }
    }
}

@Composable
private fun <T> SegmentedSelector(
    values: List<T>,
    selected: T,
    label: (T) -> String,
    onSelected: (T) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(AppColors.Panel)
            .border(1.dp, AppColors.Border, RoundedCornerShape(8.dp))
            .padding(3.dp),
        horizontalArrangement = Arrangement.spacedBy(3.dp),
    ) {
        values.forEach { value ->
            val isSelected = value == selected
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(34.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(if (isSelected) AppColors.Selection else Color.Transparent)
                    .clickable { onSelected(value) },
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = label(value),
                    color = if (isSelected) AppColors.TextPrimary else AppColors.TextSecondary,
                    fontSize = 13.sp,
                    fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

@Composable
private fun OrderBook(snapshot: OrderBookSnapshot) {
    Column(
        modifier = Modifier.fillMaxSize(),
    ) {
        HeaderRow()
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 12.dp),
        ) {
            items(snapshot.asks.asReversed(), key = { "ask:${it.price}" }) { level ->
                LevelRow(level = level)
            }
            item {
                SpreadRow(snapshot.spreadText)
            }
            items(snapshot.bids, key = { "bid:${it.price}" }) { level ->
                LevelRow(level = level)
            }
        }
    }
}

@Composable
private fun HeaderRow() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        HeaderCell("Price", TextAlign.Start)
        HeaderCell("Size", TextAlign.End)
        HeaderCell("Orders", TextAlign.End)
    }
}

@Composable
private fun RowScope.HeaderCell(text: String, textAlign: TextAlign) {
    Text(
        text = text,
        modifier = Modifier.weight(1f),
        color = AppColors.TextTertiary,
        fontSize = 11.sp,
        textAlign = textAlign,
        maxLines = 1,
    )
}

@Composable
private fun LevelRow(level: OrderBookLevel) {
    val sideColor = if (level.side == OrderBookSide.Bid) AppColors.Bid else AppColors.Ask
    val flashColor = when (level.change) {
        LevelChange.Up -> AppColors.UpFlash
        LevelChange.Down -> AppColors.DownFlash
        LevelChange.None -> Color.Transparent
    }
    val animatedFlash by animateColorAsState(flashColor)

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxWidth()
            .height(30.dp)
            .background(animatedFlash),
    ) {
        Box(
            modifier = Modifier
                .align(if (level.side == OrderBookSide.Bid) Alignment.CenterStart else Alignment.CenterEnd)
                .width(maxWidth * level.depthFraction)
                .fillMaxHeight()
                .background(sideColor.copy(alpha = 0.14f)),
        )
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = level.priceText,
                modifier = Modifier.weight(1f),
                color = sideColor,
                fontSize = 13.sp,
                fontFamily = FontFamily.Monospace,
                maxLines = 1,
            )
            Text(
                text = level.sizeText,
                modifier = Modifier.weight(1f),
                color = AppColors.TextPrimary,
                fontSize = 13.sp,
                fontFamily = FontFamily.Monospace,
                textAlign = TextAlign.End,
                maxLines = 1,
            )
            Text(
                text = level.orderCount.toString(),
                modifier = Modifier.weight(1f),
                color = AppColors.TextSecondary,
                fontSize = 13.sp,
                fontFamily = FontFamily.Monospace,
                textAlign = TextAlign.End,
                maxLines = 1,
            )
        }
    }
}

@Composable
private fun SpreadRow(spreadText: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(38.dp)
            .background(AppColors.Panel)
            .padding(horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = "Spread",
            modifier = Modifier.weight(1f),
            color = AppColors.TextTertiary,
            fontSize = 12.sp,
        )
        Text(
            text = spreadText,
            modifier = Modifier.weight(2f),
            color = AppColors.Accent,
            fontSize = 15.sp,
            fontWeight = FontWeight.SemiBold,
            fontFamily = FontFamily.Monospace,
            textAlign = TextAlign.End,
        )
    }
}

@Composable
private fun CenterMessage(message: String) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = message,
            color = AppColors.TextSecondary,
            fontSize = 14.sp,
            textAlign = TextAlign.Center,
        )
    }
}

private fun stateLabel(uiState: OrderBookUiState): String = when (uiState) {
    OrderBookUiState.Connecting -> "Connecting"
    is OrderBookUiState.Failed -> "Disconnected"
    is OrderBookUiState.Live -> "Live"
}

private fun stateColor(uiState: OrderBookUiState): Color = when (uiState) {
    OrderBookUiState.Connecting -> AppColors.TextSecondary
    is OrderBookUiState.Failed -> AppColors.Ask
    is OrderBookUiState.Live -> AppColors.Bid
}

private object AppColors {
    val Background = Color(0xFF06080B)
    val Panel = Color(0xFF10161C)
    val Selection = Color(0xFF1B2834)
    val Border = Color(0xFF1E2A35)
    val TextPrimary = Color(0xFFE8EDF2)
    val TextSecondary = Color(0xFF9AA8B4)
    val TextTertiary = Color(0xFF63717D)
    val Accent = Color(0xFFE9B44C)
    val Bid = Color(0xFF28C084)
    val Ask = Color(0xFFE85D75)
    val UpFlash = Color(0x3328C084)
    val DownFlash = Color(0x33E85D75)
}
