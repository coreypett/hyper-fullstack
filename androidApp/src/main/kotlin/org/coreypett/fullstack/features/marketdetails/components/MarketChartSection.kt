package org.coreypett.fullstack.features.marketdetails.components

import android.view.ViewGroup
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
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
import com.tradingview.lightweightcharts.api.options.models.HistogramSeriesOptions
import com.tradingview.lightweightcharts.api.options.models.LayoutOptions
import com.tradingview.lightweightcharts.api.options.models.LocalizationOptions
import com.tradingview.lightweightcharts.api.options.models.PriceScaleMargins
import com.tradingview.lightweightcharts.api.options.models.PriceScaleOptions
import com.tradingview.lightweightcharts.api.options.models.TimeScaleOptions
import com.tradingview.lightweightcharts.api.series.common.SeriesData
import com.tradingview.lightweightcharts.api.series.models.CandlestickData
import com.tradingview.lightweightcharts.api.series.models.HistogramData
import com.tradingview.lightweightcharts.api.series.models.PriceFormat
import com.tradingview.lightweightcharts.api.series.models.PriceScaleId
import com.tradingview.lightweightcharts.api.series.models.Time
import com.tradingview.lightweightcharts.runtime.plugins.Eval
import com.tradingview.lightweightcharts.view.ChartsView
import org.coreypett.fullstack.features.marketdetails.MarketDetailsChartIntervals
import org.coreypett.fullstack.market.MR
import org.coreypett.fullstack.market.candle.model.CandleBar
import org.coreypett.fullstack.market.candle.model.CandleInterval
import org.coreypett.fullstack.market.candle.presentation.CandleChartStatus
import org.coreypett.fullstack.market.candle.presentation.CandleChartState
import org.coreypett.fullstack.support.toComposeColor

@Composable
fun MarketChartSection(
    selectedInterval: CandleInterval,
    candleState: CandleChartState,
    onIntervalSelected: (CandleInterval) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(MR.colors.app_background.toComposeColor()),
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
            Image(
                painter = painterResource(id = MR.images.fullstack_logo.drawableResId),
                contentDescription = null,
                modifier = Modifier
                    .align(Alignment.Center)
                    .size(150.dp)
                    .alpha(0.18f),
            )
            if (candleState.bars.isEmpty()) {
                if (candleState.status == CandleChartStatus.Failed) {
                    FillEmptyState(
                        message = candleState.message ?: "Loading chart",
                        modifier = Modifier.fillMaxSize(),
                    )
                } else {
                    ChartSkeleton(modifier = Modifier.fillMaxSize())
                }
            } else {
                LightweightCandleChart(
                    bars = candleState.bars,
                    modifier = Modifier.fillMaxSize(),
                )
            }
        }
    }
}

@Composable
private fun ChartSkeleton(
    modifier: Modifier = Modifier,
) {
    BoxWithConstraints(modifier = modifier) {
        val spacing = 8.dp
        val chartHeight = maxHeight
        val barWidth = ((maxWidth - 32.dp - (spacing * (ChartSkeletonFractions.size - 1))) / ChartSkeletonFractions.size)
            .coerceAtLeast(8.dp)

        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp, vertical = 18.dp),
            verticalAlignment = Alignment.Bottom,
            horizontalArrangement = Arrangement.spacedBy(spacing),
        ) {
            ChartSkeletonFractions.forEachIndexed { index, fraction ->
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Bottom,
                ) {
                    SkeletonBlock(
                        width = 2.dp,
                        height = chartHeight * 0.22f,
                        cornerRadius = 1.dp,
                    )
                    Spacer(Modifier.height(4.dp))
                    SkeletonBlock(
                        width = barWidth,
                        height = (chartHeight * fraction * 0.42f).coerceAtLeast(28.dp),
                        cornerRadius = 3.dp,
                        modifier = Modifier.alpha(if (index % 2 == 0) 0.86f else 0.64f),
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
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            text = "Int.",
            color = MR.colors.text_primary.toComposeColor(),
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
            maxLines = 1,
        )
        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            MarketDetailsChartIntervals.forEach { interval ->
                val isSelected = interval == selectedInterval
                Text(
                    text = interval.chartLabel,
                    color = if (isSelected) {
                        MR.colors.text_primary.toComposeColor()
                    } else {
                        MR.colors.control_text_dimmed.toComposeColor()
                    },
                    fontSize = 12.sp,
                    fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium,
                    maxLines = 1,
                    modifier = Modifier.clickable { onIntervalSelected(interval) },
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
    val backgroundColor = android.graphics.Color.TRANSPARENT
    val textColor = MR.colors.text_tertiary.toComposeColor().toArgb()
    val gridColor = MR.colors.app_border.toComposeColor().copy(alpha = 0.36f).toArgb()
    val bidColor = MR.colors.bid.toComposeColor().toArgb()
    val askColor = MR.colors.ask.toComposeColor().toArgb()
    val volumeBidColor = MR.colors.bid.toComposeColor().copy(alpha = 0.26f).toArgb()
    val volumeAskColor = MR.colors.ask.toComposeColor().copy(alpha = 0.26f).toArgb()
    val holder = remember { CandleChartHolder() }
    val priceFormatter = remember { Eval(DollarPriceFormatterJavaScript) }
    val priceFormat = remember(priceFormatter) {
        PriceFormat.priceFormatCustom(formatter = priceFormatter, minMove = 0.01f)
    }
    val candleData = remember(bars, bidColor, askColor) {
        bars.map { bar ->
            bar.toCandlestickData(
                upColor = bidColor,
                downColor = askColor,
            )
        }
    }
    val volumeData = remember(bars, volumeBidColor, volumeAskColor) {
        bars.map { bar ->
            bar.toVolumeData(
                upColor = volumeBidColor,
                downColor = volumeAskColor,
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
                    if (chartState is ChartsView.State.Ready && holder.candleSeries == null) {
                        api.applyOptions(
                            ChartOptions(
                                layout = LayoutOptions(
                                    background = SolidColor(backgroundColor),
                                    textColor = IntColor(textColor),
                                    fontSize = 11,
                                ),
                                localization = LocalizationOptions(
                                    priceFormatter = priceFormatter,
                                ),
                                rightPriceScale = PriceScaleOptions(
                                    autoScale = true,
                                    scaleMargins = PriceScaleMargins(top = 0.08f, bottom = 0.28f),
                                    borderVisible = false,
                                    alignLabels = true,
                                ),
                                grid = GridOptions(
                                    vertLines = GridLineOptions(
                                        color = IntColor(gridColor),
                                        visible = true,
                                    ),
                                    horzLines = GridLineOptions(
                                        color = IntColor(gridColor),
                                        visible = true,
                                    ),
                                ),
                                timeScale = TimeScaleOptions(
                                    borderVisible = false,
                                    timeVisible = true,
                                    secondsVisible = false,
                                    rightOffset = 4f,
                                    barSpacing = 8f,
                                    minBarSpacing = 5f,
                                    shiftVisibleRangeOnNewBar = true,
                                ),
                            ),
                        )
                        api.addCandlestickSeries(
                            CandlestickSeriesOptions(
                                upColor = IntColor(bidColor),
                                downColor = IntColor(askColor),
                                borderVisible = false,
                                borderUpColor = IntColor(bidColor),
                                borderDownColor = IntColor(askColor),
                                wickUpColor = IntColor(bidColor),
                                wickDownColor = IntColor(askColor),
                                priceFormat = priceFormat,
                            ),
                        ) { series ->
                            holder.candleSeries = series
                            series.setData(holder.pendingCandleData.ifEmpty { candleData })
                            api.timeScale.scrollToRealTime()
                        }
                        api.addHistogramSeries(
                            HistogramSeriesOptions(
                                lastValueVisible = false,
                                priceLineVisible = false,
                                priceScaleId = PriceScaleId(VolumePriceScaleId),
                                priceFormat = PriceFormat.priceFormatBuiltIn(
                                    type = PriceFormat.Type.VOLUME,
                                    precision = 0,
                                    minMove = 1f,
                                ),
                            ),
                        ) { volumeSeries ->
                            holder.volumeSeries = volumeSeries
                            volumeSeries.priceScale().applyOptions(
                                PriceScaleOptions(
                                    scaleMargins = PriceScaleMargins(top = 0.78f, bottom = 0f),
                                    borderVisible = false,
                                    visible = false,
                                ),
                            )
                            volumeSeries.setData(holder.pendingVolumeData.ifEmpty { volumeData })
                        }
                    }
                }
            }
        },
        update = { chartView ->
            holder.pendingCandleData = candleData
            holder.pendingVolumeData = volumeData
            val candleSeries = holder.candleSeries
            if (candleSeries != null && candleData.isNotEmpty()) {
                candleSeries.setData(candleData)
                holder.volumeSeries?.setData(volumeData)
                chartView.api.timeScale.scrollToRealTime()
            }
        },
    )
}

private class CandleChartHolder {
    var candleSeries: SeriesApi? = null
    var volumeSeries: SeriesApi? = null
    var pendingCandleData: List<SeriesData> = emptyList()
    var pendingVolumeData: List<SeriesData> = emptyList()
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

private fun CandleBar.toVolumeData(
    upColor: Int,
    downColor: Int,
): SeriesData = HistogramData(
    time = Time.Utc(openTimeMillis / 1_000L),
    value = volume.toFloat(),
    color = IntColor(if (close >= open) upColor else downColor),
)

private val CandleInterval.chartLabel: String
    get() = if (this == CandleInterval.OneDay) "1D" else displayName

private const val VolumePriceScaleId = "volume"

private val ChartSkeletonFractions = listOf(0.42f, 0.66f, 0.54f, 0.76f, 0.48f, 0.58f, 0.82f, 0.62f, 0.44f, 0.70f, 0.52f, 0.64f)

private val DollarPriceFormatterJavaScript = """
function(price) {
    var sign = price < 0 ? '-$' : '$';
    var rounded = Math.round(Math.abs(price) * 100);
    var whole = String(Math.floor(rounded / 100)).replace(/\B(?=(\d{3})+(?!\d))/g, ',');
    var cents = rounded % 100;
    if (cents === 0) {
        return sign + whole;
    }
    return sign + whole + '.' + (cents < 10 ? '0' + cents : String(cents));
}
""".trimIndent()
