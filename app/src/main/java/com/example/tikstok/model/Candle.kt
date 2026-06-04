package com.example.tikstok.model

/** A single OHLC price candle. [timestamp] is epoch seconds (matching the Yahoo API). */
data class Candle(
    val timestamp: Long,
    val open: Float,
    val high: Float,
    val low: Float,
    val close: Float,
)
