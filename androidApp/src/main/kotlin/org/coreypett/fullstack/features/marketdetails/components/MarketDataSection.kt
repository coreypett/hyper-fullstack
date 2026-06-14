package org.coreypett.fullstack.features.marketdetails.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.coreypett.fullstack.features.marketdetails.MarketDetailsPanel
import org.coreypett.fullstack.market.MR
import org.coreypett.fullstack.market.trade.presentation.TradeViewState
import org.coreypett.fullstack.orderbook.model.PricePrecision
import org.coreypett.fullstack.orderbook.presentation.OrderBookViewState
import org.coreypett.fullstack.support.toComposeColor

@Composable
fun MarketDataSection(
    selectedPanel: MarketDetailsPanel,
    selectedPrecision: PricePrecision,
    orderBookState: OrderBookViewState,
    tradeState: TradeViewState,
    onPanelSelected: (MarketDetailsPanel) -> Unit,
    onPrecisionSelected: (PricePrecision) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(MR.colors.app_panel.toComposeColor())
            .border(1.dp, MR.colors.app_border.toComposeColor(), RoundedCornerShape(8.dp))
            .padding(10.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        PanelSelector(
            selectedPanel = selectedPanel,
            onPanelSelected = onPanelSelected,
        )
        if (selectedPanel == MarketDetailsPanel.OrderBook) {
            PrecisionSelector(
                selectedPrecision = selectedPrecision,
                onPrecisionSelected = onPrecisionSelected,
            )
            OrderBookSection(viewState = orderBookState)
        } else {
            TradesSection(viewState = tradeState)
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
            .fillMaxWidth()
            .clip(RoundedCornerShape(6.dp))
            .background(MR.colors.app_background.toComposeColor())
            .padding(3.dp),
        horizontalArrangement = Arrangement.spacedBy(3.dp),
    ) {
        MarketDetailsPanel.entries.forEach { panel ->
            val isSelected = panel == selectedPanel
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(34.dp)
                    .clip(RoundedCornerShape(5.dp))
                    .background(
                        if (isSelected) {
                            MR.colors.app_selection.toComposeColor()
                        } else {
                            MR.colors.app_background.toComposeColor()
                        },
                    )
                    .clickable { onPanelSelected(panel) },
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
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        PricePrecision.entries.forEach { precision ->
            val isSelected = precision == selectedPrecision
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(30.dp)
                    .clip(RoundedCornerShape(5.dp))
                    .background(
                        if (isSelected) {
                            MR.colors.app_selection.toComposeColor()
                        } else {
                            MR.colors.app_background.toComposeColor()
                        },
                    )
                    .clickable { onPrecisionSelected(precision) },
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = precision.nSigFigs.toString(),
                    color = if (isSelected) {
                        MR.colors.text_primary.toComposeColor()
                    } else {
                        MR.colors.text_secondary.toComposeColor()
                    },
                    fontSize = 12.sp,
                    fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium,
                )
            }
        }
    }
}
