package com.example.tikstok.data.portfolio

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
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
 * Session-scoped, in-memory account. Holds the user's [profiles] and the [active] one, plus the
 * account identity (nickname/email). All wallet reads and mutations delegate to the active profile,
 * so existing screens keep using `PortfolioStore.cash`, `.buy()`, etc. and automatically follow the
 * profile switch. This is a placeholder for the real per-profile wallet that will live in Firestore +
 * Room — everything resets when the app is killed for now.
 */
object PortfolioStore {

    /** Largest single deposit allowed. */
    const val MAX_DEPOSIT = 100_000.0

    /** Hard ceiling on cash held; anything above this is auto-withdrawn after a transaction. */
    const val MAX_CASH = 1_000_000.0

    /** Starting-balance options offered when creating a new profile. */
    val STARTING_BALANCES = listOf(500.0, 1_000.0, 5_000.0, 10_000.0)

    private var nextId = 1L

    private val _profiles = mutableStateListOf<Profile>()

    /** All profiles under the account, in creation order. */
    val profiles: List<Profile> get() = _profiles

    /** The profile every wallet read/write currently targets. */
    var active: Profile by mutableStateOf(newProfile("profile #1", 1_000.0))
        private set

    /** When the account was created — shown as the "joined" date on the Account screen. */
    val accountCreatedAt: Long = System.currentTimeMillis()

    /** Account identity. UI-only for now; real auth lands with Firebase. */
    var nickname by mutableStateOf("Alex")
        private set
    var email by mutableStateOf("alex@example.com")
        private set

    var lastTradeDelta by mutableStateOf<Double?>(null)
        private set
    var lastTradeTimestamp by mutableStateOf(0L)
        private set

    init {
        _profiles.add(active)
    }

    // --- Active-profile wallet views (read by Invest / Portfolio / history screens) -------------

    /** Virtual cash available to spend in the active profile, in USD. */
    val cash: Double get() = active.cash

    /** Live, read-only view of the active profile's open positions keyed by symbol. */
    val positions: Map<String, Holding> get() = active.holdings

    /** The active profile's executed trades, oldest first. */
    val transactions: List<Transaction> get() = active.transactions

    /** The active profile's cash deposits and withdrawals, oldest first. */
    val cashEntries: List<CashEntry> get() = active.cashEntries

    fun holding(symbol: String): Holding? = active.holdings[symbol]

    // --- Profile management ---------------------------------------------------------------------

    private fun newProfile(name: String, startingBalance: Double): Profile =
        Profile(id = nextId++, name = name, startingBalance = startingBalance)

    /** Suggested name for the next profile, e.g. "profile #4". */
    fun defaultProfileName(): String = "profile #${_profiles.size + 1}"

    /** Creates a new isolated profile with [startingBalance] cash and switches to it. */
    fun createProfile(name: String, startingBalance: Double) {
        val trimmed = name.trim().ifBlank { defaultProfileName() }
        val balance = startingBalance.coerceIn(0.0, MAX_CASH)
        val profile = newProfile(trimmed, balance)
        _profiles.add(profile)
        active = profile
        touch()
    }

    /** Switches the active profile; everything else in the app follows. */
    fun selectProfile(profile: Profile) {
        if (profile == active) return
        active = profile
        touch()
    }

    // --- Account identity (Settings screen) -----------------------------------------------------

    fun updateNickname(value: String) { value.trim().takeIf { it.isNotEmpty() }?.let { nickname = it } }

    fun updateEmail(value: String) { value.trim().takeIf { it.isNotEmpty() }?.let { email = it } }

    // --- Cash & trades (operate on the active profile) ------------------------------------------

    /** Top up the active profile's wallet, capped at [MAX_DEPOSIT] per deposit. */
    fun addCash(amount: Double) {
        if (amount <= 0.0) return
        val deposit = amount.coerceAtMost(MAX_DEPOSIT)
        active.cash += deposit
        val now = System.currentTimeMillis()
        active.cashEntries.add(CashEntry(CashFlowType.DEPOSIT, deposit, now))
        markDelta(deposit, now)
        enforceCashCap()
    }

    /** Auto-withdraws anything above [MAX_CASH] so the wallet never holds more than the ceiling. */
    private fun enforceCashCap() {
        val excess = active.cash - MAX_CASH
        if (excess <= 0.0) return
        active.cash = MAX_CASH
        val now = System.currentTimeMillis()
        active.cashEntries.add(CashEntry(CashFlowType.WITHDRAWAL, excess, now))
        markDelta(-excess, now)
    }

    /** Take cash out of the active profile's wallet, capped at the available balance. */
    fun withdrawCash(amount: Double) {
        if (amount <= 0.0) return
        val take = amount.coerceAtMost(active.cash)
        if (take <= 0.0) return
        active.cash -= take
        val now = System.currentTimeMillis()
        active.cashEntries.add(CashEntry(CashFlowType.WITHDRAWAL, take, now))
        markDelta(-take, now)
    }

    /** Spend up to [amount] USD buying [symbol] at [price] per unit. */
    fun buy(symbol: String, price: Double, amount: Double) {
        if (price <= 0.0 || amount <= 0.0) return
        val spend = amount.coerceAtMost(active.cash)
        if (spend <= 0.0) return
        val units = spend / price
        val prev = active.holdings[symbol]
        val newQty = (prev?.quantity ?: 0.0) + units
        val newAvg = if (prev == null) price
        else (prev.quantity * prev.avgCost + units * price) / newQty
        active.holdings[symbol] = Holding(newQty, newAvg)
        active.cash -= spend
        recordTrade(symbol, TransactionType.BUY, units, price, spend, -spend)
    }

    /** Sell up to [amount] USD worth of [symbol] at [price] per unit. */
    fun sell(symbol: String, price: Double, amount: Double) {
        if (price <= 0.0 || amount <= 0.0) return
        val h = active.holdings[symbol] ?: return
        val sellValue = amount.coerceAtMost(h.quantity * price)
        if (sellValue <= 0.0) return
        val units = (sellValue / price).coerceAtMost(h.quantity)
        val remaining = h.quantity - units
        if (remaining <= 1e-9) active.holdings.remove(symbol)
        else active.holdings[symbol] = h.copy(quantity = remaining)
        active.cash += sellValue
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
        active.transactions.add(Transaction(symbol, type, quantity, unitPrice, amount, now))
        markDelta(cashDelta, now)
    }

    private fun markDelta(delta: Double, now: Long) {
        lastTradeDelta = delta
        lastTradeTimestamp = now
    }

    /** Bumps the trade tick (without a cash delta) so screens keyed on it re-price. */
    private fun touch() {
        lastTradeDelta = null
        lastTradeTimestamp = System.currentTimeMillis()
    }
}
