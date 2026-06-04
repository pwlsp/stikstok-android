package com.example.tikstok.data.portfolio

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

/** A position in one asset: how many units are held and the average price paid per unit. */
data class Holding(val quantity: Double, val avgCost: Double)

/**
 * Session-scoped, in-memory portfolio for the active profile. Backed by Compose snapshot state so
 * the UI recomposes on every trade. This is a placeholder for the real per-profile wallet that will
 * live in Firestore + Room — buys and sells reset when the app is killed for now.
 */
object PortfolioStore {

    /** Virtual cash available to spend, in USD. Mirrors the profile's starting balance. */
    var cash by mutableStateOf(1_000.0)
        private set

    var lastTradeDelta by mutableStateOf<Double?>(null)
        private set
    var lastTradeTimestamp by mutableStateOf(0L)
        private set

    private val holdings = mutableStateMapOf<String, Holding>()

    fun holding(symbol: String): Holding? = holdings[symbol]

    /** Spend up to [amount] USD buying [symbol] at [price] per unit. */
    fun buy(symbol: String, price: Double, amount: Double) {
        if (price <= 0.0 || amount <= 0.0) return
        val spend = amount.coerceAtMost(cash)
        if (spend <= 0.0) return
        val units = spend / price
        val prev = holdings[symbol]
        val newQty = (prev?.quantity ?: 0.0) + units
        val newAvg = if (prev == null) price
        else (prev.quantity * prev.avgCost + units * price) / newQty
        holdings[symbol] = Holding(newQty, newAvg)
        cash -= spend
        lastTradeDelta = -spend
        lastTradeTimestamp = System.currentTimeMillis()
    }

    /** Sell up to [amount] USD worth of [symbol] at [price] per unit. */
    fun sell(symbol: String, price: Double, amount: Double) {
        if (price <= 0.0 || amount <= 0.0) return
        val h = holdings[symbol] ?: return
        val sellValue = amount.coerceAtMost(h.quantity * price)
        if (sellValue <= 0.0) return
        val units = (sellValue / price).coerceAtMost(h.quantity)
        val remaining = h.quantity - units
        if (remaining <= 1e-9) holdings.remove(symbol) else holdings[symbol] = h.copy(quantity = remaining)
        cash += sellValue
        lastTradeDelta = sellValue
        lastTradeTimestamp = System.currentTimeMillis()
    }
}
