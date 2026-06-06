package com.example.tikstok.data.local

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

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

@Entity(tableName = "series_meta")
data class SeriesMetaEntity(
    @PrimaryKey val key: String,
    val currency: String,
    val fetchedAt: Long,
)
