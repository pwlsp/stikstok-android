package com.example.tikstok.data.repository

import com.example.tikstok.TikStokApplication
import com.example.tikstok.data.local.CandleEntity
import com.example.tikstok.data.local.MarketCacheDao
import com.example.tikstok.data.local.SeriesMetaEntity
import com.example.tikstok.data.local.TikStokDatabase
import com.example.tikstok.data.remote.yahoo.PriceSeries
import com.example.tikstok.data.remote.yahoo.YahooFinanceService
import com.example.tikstok.model.Candle
import com.example.tikstok.model.Timeframe
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Provides market price data to the UI layer, backed by a Room cache so views don't hit the network
 * on every switch. Per request it serves a fresh cached series when one exists (within the frame's
 * [Timeframe.cacheTtlMillis]); otherwise it fetches from Yahoo, stores the result, and returns it.
 * If the fetch fails (e.g. offline) it falls back to whatever is cached, even if stale, so the app
 * keeps working without a connection.
 */
class MarketRepository(
    private val yahoo: YahooFinanceService = YahooFinanceService(),
    private val dao: MarketCacheDao =
        TikStokDatabase.get(TikStokApplication.appContext).marketCacheDao(),
) {
    suspend fun candles(symbol: String, timeframe: Timeframe): PriceSeries =
        withContext(Dispatchers.IO) {
            val key = seriesKey(symbol, timeframe)
            val meta = dao.meta(key)
            val now = System.currentTimeMillis()

            // Fresh cache hit — skip the network entirely.
            if (meta != null && now - meta.fetchedAt < timeframe.cacheTtlMillis) {
                val cached = dao.candles(symbol, timeframe.name)
                if (cached.isNotEmpty()) {
                    return@withContext PriceSeries(cached.map { it.toCandle() }, meta.currency)
                }
            }

            try {
                val series = yahoo.fetchCandles(symbol, timeframe)
                dao.replaceSeries(
                    symbol = symbol,
                    timeframe = timeframe.name,
                    candles = series.candles.map { it.toEntity(symbol, timeframe.name) },
                    meta = SeriesMetaEntity(key, series.currency, now),
                )
                series
            } catch (t: Throwable) {
                // Offline / fetch failed: serve stale cache if we have any, otherwise surface the error.
                val cached = dao.candles(symbol, timeframe.name)
                if (cached.isNotEmpty()) {
                    PriceSeries(cached.map { it.toCandle() }, meta?.currency ?: "USD")
                } else {
                    throw t
                }
            }
        }

    private fun seriesKey(symbol: String, timeframe: Timeframe) = "$symbol:${timeframe.name}"
}

private fun CandleEntity.toCandle() = Candle(timestamp, open, high, low, close)

private fun Candle.toEntity(symbol: String, timeframe: String) =
    CandleEntity(symbol, timeframe, timestamp, open, high, low, close)
