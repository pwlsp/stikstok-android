package com.example.tikstok.model

enum class Timeframe(
    val label: String,
    val range: String,
    val interval: String,
    val displayCandles: Int?,
) {
    M30("30m", "5d", "1m", 30),
    H1("1h", "5d", "2m", 30),
    H3("3h", "5d", "5m", 36),
    D1("1d", "5d", "5m", 78),
    W1("1w", "5d", "15m", null),
    MO1("1mo", "1mo", "60m", null),
    Y1("1y", "1y", "1d", null),
    Y5("5y", "5y", "1wk", null);

    val isScrollable: Boolean get() = ordinal >= D1.ordinal

    val cacheTtlMillis: Long
        get() = when (this) {
            M30, H1, H3, D1 -> 60_000L
            W1, MO1 -> 15 * 60_000L
            Y1, Y5 -> 6 * 60 * 60_000L
        }

    companion object {
        val DEFAULT = D1
    }
}
