package org.coreypett.fullstack.market.candle.model

enum class CandleInterval(
    val wireName: String,
    val displayName: String,
    val chartLabel: String = displayName,
) {
    OneMinute("1m", "1m"),
    ThreeMinutes("3m", "3m"),
    FiveMinutes("5m", "5m"),
    FifteenMinutes("15m", "15m"),
    ThirtyMinutes("30m", "30m"),
    OneHour("1h", "1h"),
    TwoHours("2h", "2h"),
    FourHours("4h", "4h"),
    EightHours("8h", "8h"),
    TwelveHours("12h", "12h"),
    OneDay("1d", "1d", "1D"),
    ThreeDays("3d", "3d"),
    OneWeek("1w", "1w"),
    OneMonth("1M", "1M"),
}
