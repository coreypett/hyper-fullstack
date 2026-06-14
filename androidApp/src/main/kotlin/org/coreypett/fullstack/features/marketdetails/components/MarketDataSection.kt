package org.coreypett.fullstack.features.marketdetails.components

import androidx.compose.foundation.background
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.coreypett.fullstack.features.marketdetails.MarketDetailsPanel
import org.coreypett.fullstack.market.MR
import org.coreypett.fullstack.market.model.MarketSymbol
import org.coreypett.fullstack.market.trade.presentation.RecentTradesViewState
import org.coreypett.fullstack.orderbook.model.PricePrecision
import org.coreypett.fullstack.orderbook.presentation.OrderBookViewState
import org.coreypett.fullstack.support.toComposeColor

@Composable
fun MarketDataSection(
    selectedPanel: MarketDetailsPanel,
    selectedMarket: MarketSymbol,
    selectedPrecision: PricePrecision,
    orderBookState: OrderBookViewState,
    recentTradesState: RecentTradesViewState,
    onPanelSelected: (MarketDetailsPanel) -> Unit,
    onPrecisionSelected: (PricePrecision) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(MR.colors.app_background.toComposeColor()),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            PanelSelector(
                selectedPanel = selectedPanel,
                onPanelSelected = onPanelSelected,
            )
            Spacer(Modifier.weight(1f))
            if (selectedPanel == MarketDetailsPanel.OrderBook) {
                PrecisionSelector(
                    selectedPrecision = selectedPrecision,
                    onPrecisionSelected = onPrecisionSelected,
                )
            }
        }

        if (selectedPanel == MarketDetailsPanel.OrderBook) {
            OrderBookSection(viewState = orderBookState)
        } else {
            RecentTradesSection(
                market = selectedMarket,
                viewState = recentTradesState,
            )
        }
    }
}

@Composable
private fun PanelSelector(
    selectedPanel: MarketDetailsPanel,
    onPanelSelected: (MarketDetailsPanel) -> Unit,
) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(percent = 50))
            .background(MR.colors.app_panel.toComposeColor().copy(alpha = 0.62f))
            .padding(3.dp),
        horizontalArrangement = Arrangement.spacedBy(3.dp),
    ) {
        MarketDetailsPanel.entries.forEach { panel ->
            val isSelected = panel == selectedPanel
            Box(
                modifier = Modifier
                    .height(34.dp)
                    .widthIn(min = 86.dp)
                    .clip(RoundedCornerShape(percent = 50))
                    .background(
                        if (isSelected) {
                            MR.colors.app_selection.toComposeColor()
                        } else {
                            MR.colors.app_panel.toComposeColor().copy(alpha = 0f)
                        },
                    )
                    .clickable { onPanelSelected(panel) }
                    .padding(horizontal = 12.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = panel.title,
                    color = if (isSelected) {
                        MR.colors.text_primary.toComposeColor()
                    } else {
                        MR.colors.text_secondary.toComposeColor()
                    },
                    fontSize = 13.sp,
                    fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium,
                )
            }
        }
    }
}

@Composable
private fun PrecisionSelector(
    selectedPrecision: PricePrecision,
    onPrecisionSelected: (PricePrecision) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }

    Box {
        Row(
            modifier = Modifier
                .height(34.dp)
                .clip(RoundedCornerShape(percent = 50))
                .background(MR.colors.app_panel.toComposeColor().copy(alpha = 0.62f))
                .clickable { expanded = true }
                .padding(horizontal = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Text(
                text = "Grouping",
                color = MR.colors.text_tertiary.toComposeColor(),
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
            )
            Text(
                text = selectedPrecision.nSigFigs.toString(),
                color = MR.colors.text_primary.toComposeColor(),
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
            )
            ChevronDown()
        }

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
        ) {
            PricePrecision.entries.forEach { precision ->
                val isSelected = precision == selectedPrecision
                DropdownMenuItem(
                    text = {
                        Text(
                            text = groupingLabel(precision),
                            color = if (isSelected) {
                                MR.colors.text_primary.toComposeColor()
                            } else {
                                MR.colors.text_secondary.toComposeColor()
                            },
                            fontSize = 13.sp,
                            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium,
                        )
                    },
                    onClick = {
                        expanded = false
                        onPrecisionSelected(precision)
                    },
                )
            }
        }
    }
}

private fun groupingLabel(precision: PricePrecision): String =
    "${precision.nSigFigs} significant figures"

@Composable
private fun ChevronDown() {
    val color = MR.colors.text_secondary.toComposeColor()
    Canvas(modifier = Modifier.size(width = 10.dp, height = 6.dp)) {
        val strokeWidth = 1.6.dp.toPx()
        val centerY = size.height * 0.58f
        drawLine(
            color = color,
            start = androidx.compose.ui.geometry.Offset(size.width * 0.2f, size.height * 0.25f),
            end = androidx.compose.ui.geometry.Offset(size.width * 0.5f, centerY),
            strokeWidth = strokeWidth,
            cap = StrokeCap.Round,
        )
        drawLine(
            color = color,
            start = androidx.compose.ui.geometry.Offset(size.width * 0.8f, size.height * 0.25f),
            end = androidx.compose.ui.geometry.Offset(size.width * 0.5f, centerY),
            strokeWidth = strokeWidth,
            cap = StrokeCap.Round,
        )
    }
}
