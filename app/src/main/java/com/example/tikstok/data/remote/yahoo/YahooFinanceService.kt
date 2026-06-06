package com.example.tikstok.data.remote.yahoo

import com.example.tikstok.model.Candle
import com.example.tikstok.model.Timeframe
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

data class PriceSeries(val candles: List<Candle>, val currency: String)

class YahooFinanceService {

    fun fetchCandles(symbol: String, timeframe: Timeframe): PriceSeries {
        val url = URL(
            "https://query1.finance.yahoo.com/v8/finance/chart/$symbol" +
                "?interval=${timeframe.interval}&range=${timeframe.range}"
        )
        val connection = (url.openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
            connectTimeout = 10_000
            readTimeout = 10_000
            setRequestProperty("User-Agent", USER_AGENT)
        }

        val code = connection.responseCode
        val stream = if (code in 200..299) connection.inputStream else connection.errorStream
        val body = stream.bufferedReader().use { it.readText() }
        if (code !in 200..299) error("HTTP $code from Yahoo: ${body.take(120)}")

        val result = JSONObject(body)
            .getJSONObject("chart")
            .getJSONArray("result")
            .getJSONObject(0)
        val currency = result.getJSONObject("meta").optString("currency", "USD")
        val timestamps = result.getJSONArray("timestamp")
        val quote = result.getJSONObject("indicators").getJSONArray("quote").getJSONObject(0)
        val opens = quote.getJSONArray("open")
        val highs = quote.getJSONArray("high")
        val lows = quote.getJSONArray("low")
        val closes = quote.getJSONArray("close")

        val candles = ArrayList<Candle>(timestamps.length())
        for (i in 0 until timestamps.length()) {
            if (opens.isNull(i) || highs.isNull(i) || lows.isNull(i) || closes.isNull(i)) continue
            candles += Candle(
                timestamp = timestamps.getLong(i),
                open = opens.getDouble(i).toFloat(),
                high = highs.getDouble(i).toFloat(),
                low = lows.getDouble(i).toFloat(),
                close = closes.getDouble(i).toFloat(),
            )
        }
        if (candles.isEmpty()) error("No price data for $symbol")

        val trimmed = timeframe.displayCandles
            ?.let { n -> if (candles.size > n) candles.subList(candles.size - n, candles.size) else candles }
            ?: candles
        return PriceSeries(trimmed.toList(), currency)
    }

    companion object {
        private const val USER_AGENT = "Mozilla/5.0 (Android) TikStok/1.0"
    }
}
