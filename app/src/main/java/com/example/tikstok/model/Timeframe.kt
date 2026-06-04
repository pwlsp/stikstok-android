package com.example.tikstok.model

/**
 * A chart timeframe shown in the selector. Each maps to a Yahoo Finance `range` + `interval`
 * pair; [displayCandles], when set, trims the response to the last N candles.
 *
 * The intraday timeframes deliberately request a longer `range` (5d) than they show and then trim
 * to a fixed, dense candle count. This keeps the chart well-populated even right after the market
 * opens, when `range=1d` would return only a handful of candles and they'd stretch very wide.
 * The trim count matches the label's span at the chosen interval (e.g. 1h = 30 × 2m candles).
 *
 * [label] is intentionally not localized — "30m", "1h", "1y" read the same in every language.
 */
enum class Timeframe(
    val label: String,
    val range: String,
    val interval: String,
    val displayCandles: Int?,
) {
    M30("30m", "5d", "1m", 30),
    H1("1h", "5d", "2m", 30),
    H4("4h", "5d", "5m", 48),
    D1("1d", "5d", "15m", 26),
    W1("1w", "5d", "60m", null),
    MO1("1mo", "1mo", "1d", null),
    Y1("1y", "1y", "1d", null),
    Y5("5y", "5y", "1wk", null);

    companion object {
        val DEFAULT = D1
    }
}
