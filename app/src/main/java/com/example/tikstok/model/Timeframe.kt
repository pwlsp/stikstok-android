package com.example.tikstok.model

/**
 * A chart timeframe shown in the selector. Each maps to a Yahoo Finance `range` + `interval`
 * pair; [displayCandles], when set, trims the response to the last N candles so e.g. "30m"
 * shows the last 30 one-minute candles rather than the whole trading day.
 *
 * [label] is intentionally not localized — "30m", "1h", "1y" read the same in every language.
 */
enum class Timeframe(
    val label: String,
    val range: String,
    val interval: String,
    val displayCandles: Int?,
) {
    M30("30m", "1d", "1m", 30),
    H1("1h", "1d", "1m", 60),
    H4("4h", "1d", "5m", 48),
    D1("1d", "1d", "5m", null),
    W1("1w", "5d", "60m", null),
    MO1("1mo", "1mo", "1d", null),
    Y1("1y", "1y", "1d", null),
    Y5("5y", "5y", "1wk", null);

    companion object {
        val DEFAULT = D1
    }
}
