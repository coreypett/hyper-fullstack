package org.coreypett.fullstack.features.marketdetails.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.coreypett.fullstack.market.MR
import org.coreypett.fullstack.market.model.MarketSymbol
import org.coreypett.fullstack.support.toComposeColor
import org.coreypett.fullstack.support.tokenIconResource

@Composable
fun MarketTabs(
    selectedMarket: MarketSymbol,
    onMarketSelected: (MarketSymbol) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        MarketSymbol.entries.forEach { market ->
            val isSelected = market == selectedMarket
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(38.dp)
                    .clip(RoundedCornerShape(percent = 50))
                    .background(
                        if (isSelected) {
                            MR.colors.app_selection.toComposeColor()
                        } else {
                            MR.colors.app_panel.toComposeColor()
                        },
                    )
                    .border(
                        width = 1.dp,
                        color = if (isSelected) {
                            MR.colors.text_secondary.toComposeColor().copy(alpha = 0.42f)
                        } else {
                            MR.colors.app_border.toComposeColor()
                        },
                        shape = RoundedCornerShape(percent = 50),
                    )
                    .clickable { onMarketSelected(market) }
                    .padding(horizontal = 10.dp),
                contentAlignment = Alignment.Center,
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center,
                ) {
                    Image(
                        painter = painterResource(id = market.tokenIconResource().drawableResId),
                        contentDescription = null,
                        modifier = Modifier.size(20.dp),
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = market.displayName,
                        color = if (isSelected) {
                            MR.colors.text_primary.toComposeColor()
                        } else {
                            MR.colors.text_secondary.toComposeColor()
                        },
                        fontSize = 14.sp,
                        fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium,
                    )
                }
            }
        }
    }
}
