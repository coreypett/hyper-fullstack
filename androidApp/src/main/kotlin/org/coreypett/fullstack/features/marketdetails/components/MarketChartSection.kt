package org.coreypett.fullstack.features.marketdetails.components

import android.view.ViewGroup
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.tradingview.lightweightcharts.api.chart.models.color.IntColor
import com.tradingview.lightweightcharts.api.chart.models.color.surface.SolidColor
import com.tradingview.lightweightcharts.api.interfaces.SeriesApi
import com.tradingview.lightweightcharts.api.options.models.CandlestickSeriesOptions
import com.tradingview.lightweightcharts.api.options.models.ChartOptions
import com.tradingview.lightweightcharts.api.options.models.GridLineOptions
import com.tradingview.lightweightcharts.api.options.models.GridOptions
import com.tradingview.lightweightcharts.api.options.models.LayoutOptions
import com.tradingview.lightweightcharts.api.options.models.TimeScaleOptions
import com.tradingview.lightweightcharts.api.series.common.SeriesData
import com.tradingview.lightweightcharts.api.series.models.CandlestickData
import com.tradingview.lightweightcharts.api.series.models.Time
import com.tradingview.lightweightcharts.view.ChartsView
import org.coreypett.fullstack.features.marketdetails.MarketDetailsChartIntervals
import org.coreypett.fullstack.market.MR
import org.coreypett.fullstack.market.candle.model.CandleBar
import org.coreypett.fullstack.market.candle.model.CandleInterval
import org.coreypett.fullstack.market.candle.presentation.CandleChartState
import org.coreypett.fullstack.support.toComposeColor

@Composable
fun MarketChartSection(
    selectedInterval: CandleInterval,
    candleState: CandleChartState,
    onIntervalSelected: (CandleInterval) -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(MR.colors.app_panel.toComposeColor())
            .border(1.dp, MR.colors.app_border.toComposeColor(), RoundedCornerShape(8.dp))
            .padding(10.dp),
    ) {
        Image(
            painter = painterResource(id = MR.images.fullstack_logo.drawableResId),
            contentDescription = null,
            modifier = Modifier
                .align(Alignment.Center)
                .size(120.dp)
                .alpha(0.07f),
        )
        androidx.compose.foundation.layout.Column(
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            IntervalSelector(
                selectedInterval = selectedInterval,
                onIntervalSelected = onIntervalSelected,
            )
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(260.dp),
            ) {
                if (candleState.bars.isEmpty()) {
                    FillEmptyState(
                        message = candleState.message ?: "Loading chart",
                        modifier = Modifier.fillMaxSize(),
                    )
                } else {
                    LightweightCandleChart(
                        bars = candleState.bars,
                        modifier = Modifier.fillMaxSize(),
                    )
                }
            }
        }
    }
}

@Composable
private fun IntervalSelector(
    selectedInterval: CandleInterval,
    onIntervalSelected: (CandleInterval) -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        MarketDetailsChartIntervals.forEach { interval ->
            val isSelected = interval == selectedInterval
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(32.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(
                        if (isSelected) {
                            MR.colors.app_selection.toComposeColor()
                        } else {
                            MR.colors.app_background.toComposeColor()
                        },
                    )
                    .clickable { onIntervalSelected(interval) },
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = interval.displayName,
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

@Composable
private fun LightweightCandleChart(
    bars: List<CandleBar>,
    modifier: Modifier = Modifier,
) {
    val backgroundColor = MR.colors.app_panel.toComposeColor().toArgb()
    val textColor = MR.colors.text_tertiary.toComposeColor().toArgb()
    val borderColor = MR.colors.app_border.toComposeColor().toArgb()
    val bidColor = MR.colors.bid.toComposeColor().toArgb()
    val askColor = MR.colors.ask.toComposeColor().toArgb()
    val holder = remember { CandleChartHolder() }
    val seriesData = remember(bars, bidColor, askColor) {
        bars.map { bar ->
            bar.toCandlestickData(
                upColor = bidColor,
                downColor = askColor,
            )
        }
    }

    AndroidView(
        modifier = modifier,
        factory = { context ->
            ChartsView(context).apply {
                layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT,
                )
                setBackgroundColor(backgroundColor)
                subscribeOnChartStateChange { chartState ->
                    if (chartState is ChartsView.State.Ready && holder.series == null) {
                        api.applyOptions(
                            ChartOptions(
                                layout = LayoutOptions(
                                    background = SolidColor(backgroundColor),
                                    textColor = IntColor(textColor),
                                    fontSize = 11,
                                ),
                                grid = GridOptions(
                                    vertLines = GridLineOptions(
                                        color = IntColor(borderColor),
                                        visible = false,
                                    ),
                                    horzLines = GridLineOptions(
                                        color = IntColor(borderColor),
                                        visible = true,
                                    ),
                                ),
                                timeScale = TimeScaleOptions(
                                    borderVisible = false,
                                    timeVisible = true,
                                    secondsVisible = false,
                                    rightOffset = 4f,
                                ),
                            ),
                        )
                        api.addCandlestickSeries(
                            CandlestickSeriesOptions(
                                upColor = IntColor(bidColor),
                                downColor = IntColor(askColor),
                                borderUpColor = IntColor(bidColor),
                                borderDownColor = IntColor(askColor),
                                wickUpColor = IntColor(bidColor),
                                wickDownColor = IntColor(askColor),
                            ),
                        ) { series ->
                            holder.series = series
                            series.setData(holder.pendingData.ifEmpty { seriesData })
                            api.timeScale.fitContent()
                        }
                    }
                }
            }
        },
        update = { chartView ->
            holder.pendingData = seriesData
            val series = holder.series
            if (series != null && seriesData.isNotEmpty()) {
                series.setData(seriesData)
                chartView.api.timeScale.fitContent()
            }
        },
    )
}

private class CandleChartHolder {
    var series: SeriesApi? = null
    var pendingData: List<SeriesData> = emptyList()
}

private fun CandleBar.toCandlestickData(
    upColor: Int,
    downColor: Int,
): SeriesData {
    val candleColor = if (close >= open) upColor else downColor
    return CandlestickData(
        time = Time.Utc(openTimeMillis / 1_000L),
        open = open.toFloat(),
        high = high.toFloat(),
        low = low.toFloat(),
        close = close.toFloat(),
        color = IntColor(candleColor),
        borderColor = IntColor(candleColor),
        wickColor = IntColor(candleColor),
    )
}
