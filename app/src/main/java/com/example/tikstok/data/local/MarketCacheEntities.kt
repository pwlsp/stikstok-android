package com.example.tikstok.data.local

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * One cached OHLC candle for a (symbol, timeframe) series. The natural key is
 * symbol + timeframe + timestamp, so refetching the same series upserts its rows in place.
 */
@Entity(
    tableName = "candle",
    primaryKeys = ["symbol", "timeframe", "timestamp"],
    indices = [Index(value = ["symbol", "timeframe"])],
)
data class CandleEntity(
    val symbol: String,
    val timeframe: String,
    val timestamp: Long,
    val open: Float,
    val high: Float,
    val low: Float,
    val close: Float,
)

/**
 * Per-series metadata: when the series was last fetched (for staleness checks) and the currency it
 * was quoted in (Yahoo reports it alongside the candles, but it isn't stored on each row).
 */
@Entity(tableName = "series_meta")
data class SeriesMetaEntity(
    @PrimaryKey val key: String,
    val currency: String,
    val fetchedAt: Long,
)
