package com.example.tikstok.model

/**
 * A chart timeframe shown in the selector. Each maps to a Yahoo Finance `range` + `interval`
 * pair; [displayCandles], when set, trims the response to the last N candles.
 *
 * The short intraday frames deliberately request a longer `range` (5d) than they show and trim to
 * a fixed, dense count, so the chart stays populated even right after the open when `range=1d`
 * would return only a handful of candles. The wider frames pick a fine enough interval to also
 * carry plenty of candles. Frames from 1d up ([isScrollable]) hold more candles than fit on screen
 * — they scroll, and offer the non-scrolling line-chart overview.
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
    H3("3h", "5d", "5m", 36),
    D1("1d", "5d", "5m", 78),
    W1("1w", "5d", "15m", null),
    MO1("1mo", "1mo", "60m", null),
    Y1("1y", "1y", "1d", null),
    Y5("5y", "5y", "1wk", null);

    /** Frames from 1d up pack enough candles to scroll, and offer the line-chart overview. */
    val isScrollable: Boolean get() = ordinal >= D1.ordinal

    /**
     * How long a cached series for this frame stays fresh before the repository refetches. Short,
     * fast-moving intraday frames expire in a minute; the wider, coarser frames barely change within
     * an hour (or a day), so they're cached far longer to spare the network.
     */
    val cacheTtlMillis: Long
        get() = when (this) {
            M30, H1, H3, D1 -> 60_000L            // 1 minute
            W1, MO1 -> 15 * 60_000L               // 15 minutes
            Y1, Y5 -> 6 * 60 * 60_000L            // 6 hours
        }

    companion object {
        val DEFAULT = D1
    }
}
