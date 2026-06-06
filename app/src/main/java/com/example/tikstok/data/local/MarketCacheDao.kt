package com.example.tikstok.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction

@Dao
interface MarketCacheDao {

    @Query("SELECT * FROM candle WHERE symbol = :symbol AND timeframe = :timeframe ORDER BY timestamp ASC")
    suspend fun candles(symbol: String, timeframe: String): List<CandleEntity>

    @Query("SELECT * FROM series_meta WHERE `key` = :key")
    suspend fun meta(key: String): SeriesMetaEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertCandles(candles: List<CandleEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertMeta(meta: SeriesMetaEntity)

    @Query("DELETE FROM candle WHERE symbol = :symbol AND timeframe = :timeframe")
    suspend fun clearCandles(symbol: String, timeframe: String)

    @Transaction
    suspend fun replaceSeries(
        symbol: String,
        timeframe: String,
        candles: List<CandleEntity>,
        meta: SeriesMetaEntity,
    ) {
        clearCandles(symbol, timeframe)
        upsertCandles(candles)
        upsertMeta(meta)
    }
}
