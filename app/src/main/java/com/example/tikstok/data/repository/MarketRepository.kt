package com.example.tikstok.data.repository

import com.example.tikstok.data.remote.yahoo.PriceSeries
import com.example.tikstok.data.remote.yahoo.YahooFinanceService
import com.example.tikstok.model.Timeframe
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Provides market price data to the UI layer. For now it fetches straight from Yahoo on each
 * request; Room caching will slot in here later without changing the call site.
 */
class MarketRepository(
    private val yahoo: YahooFinanceService = YahooFinanceService(),
) {
    suspend fun candles(symbol: String, timeframe: Timeframe): PriceSeries =
        withContext(Dispatchers.IO) { yahoo.fetchCandles(symbol, timeframe) }
}
