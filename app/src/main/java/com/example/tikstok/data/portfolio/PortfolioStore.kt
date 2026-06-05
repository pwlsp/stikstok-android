package com.example.tikstok.data.portfolio

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

/** A position in one asset: how many units are held and the average price paid per unit. */
data class Holding(val quantity: Double, val avgCost: Double)

enum class TransactionType { BUY, SELL }

/** A single executed trade, kept so the history screen can show what happened and at what price. */
data class Transaction(
    val symbol: String,
    val type: TransactionType,
    /** Units bought or sold. */
    val quantity: Double,
    /** Price per unit at execution time — the "bought/sold at" price. */
    val unitPrice: Double,
    /** Cash that changed hands (quantity * unitPrice). */
    val amount: Double,
    val timestamp: Long,
)

enum class CashFlowType { DEPOSIT, WITHDRAWAL }

/** A cash deposit (top-up) or withdrawal — money entering or leaving the wallet, not a trade. */
data class CashEntry(
    val type: CashFlowType,
    val amount: Double,
    val timestamp: Long,
)

/**
 * Session-scoped, in-memory portfolio for the active profile. Backed by Compose snapshot state so
 * the UI recomposes on every trade. This is a placeholder for the real per-profile wallet that will
 * live in Firestore + Room — buys, sells and history reset when the app is killed for now.
 */
object PortfolioStore {

    /** Largest single deposit allowed. */
    const val MAX_DEPOSIT = 100_000.0

    /** Hard ceiling on cash held; anything above this is auto-withdrawn after a transaction. */
    const val MAX_CASH = 1_000_000.0

    /** Virtual cash available to spend, in USD. Mirrors the profile's starting balance. */
    var cash by mutableStateOf(1_000.0)
        private set

    var lastTradeDelta by mutableStateOf<Double?>(null)
        private set
    var lastTradeTimestamp by mutableStateOf(0L)
        private set

    private val holdings = mutableStateMapOf<String, Holding>()

    /** Live, read-only view of all open positions keyed by symbol. */
    val positions: Map<String, Holding> get() = holdings

    private val _transactions = mutableStateListOf<Transaction>()

    /** All executed trades, oldest first. */
    val transactions: List<Transaction> get() = _transactions

    private val _cashEntries = mutableStateListOf<CashEntry>()

    /** All cash deposits and withdrawals, oldest first. */
    val cashEntries: List<CashEntry> get() = _cashEntries

    init {
        // The starting balance counts as the first deposit so the cash history is complete.
        _cashEntries.add(CashEntry(CashFlowType.DEPOSIT, cash, System.currentTimeMillis()))
    }

    fun holding(symbol: String): Holding? = holdings[symbol]

    /** Top up the virtual wallet, capped at [MAX_DEPOSIT] per deposit. */
    fun addCash(amount: Double) {
        if (amount <= 0.0) return
        val deposit = amount.coerceAtMost(MAX_DEPOSIT)
        cash += deposit
        val now = System.currentTimeMillis()
        _cashEntries.add(CashEntry(CashFlowType.DEPOSIT, deposit, now))
        lastTradeDelta = deposit
        lastTradeTimestamp = now
        enforceCashCap()
    }

    /** Auto-withdraws anything above [MAX_CASH] so the wallet never holds more than the ceiling. */
    private fun enforceCashCap() {
        val excess = cash - MAX_CASH
        if (excess <= 0.0) return
        cash = MAX_CASH
        val now = System.currentTimeMillis()
        _cashEntries.add(CashEntry(CashFlowType.WITHDRAWAL, excess, now))
        lastTradeDelta = -excess
        lastTradeTimestamp = now
    }

    /** Take cash out of the wallet, capped at the available balance. */
    fun withdrawCash(amount: Double) {
        if (amount <= 0.0) return
        val take = amount.coerceAtMost(cash)
        if (take <= 0.0) return
        cash -= take
        val now = System.currentTimeMillis()
        _cashEntries.add(CashEntry(CashFlowType.WITHDRAWAL, take, now))
        lastTradeDelta = -take
        lastTradeTimestamp = now
    }

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
        recordTrade(symbol, TransactionType.BUY, units, price, spend, -spend)
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
        recordTrade(symbol, TransactionType.SELL, units, price, sellValue, sellValue)
        enforceCashCap()
    }

    private fun recordTrade(
        symbol: String,
        type: TransactionType,
        quantity: Double,
        unitPrice: Double,
        amount: Double,
        cashDelta: Double,
    ) {
        val now = System.currentTimeMillis()
        _transactions.add(Transaction(symbol, type, quantity, unitPrice, amount, now))
        lastTradeDelta = cashDelta
        lastTradeTimestamp = now
    }
}
