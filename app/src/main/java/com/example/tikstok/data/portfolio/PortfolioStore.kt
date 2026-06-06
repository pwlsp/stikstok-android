package com.example.tikstok.data.portfolio

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

data class Holding(val quantity: Double, val avgCost: Double)

enum class TransactionType { BUY, SELL }

data class Transaction(
    val symbol: String,
    val type: TransactionType,
    val quantity: Double,
    val unitPrice: Double,
    val amount: Double,
    val timestamp: Long,
)

enum class CashFlowType { DEPOSIT, WITHDRAWAL }

data class CashEntry(
    val type: CashFlowType,
    val amount: Double,
    val timestamp: Long,
)

object PortfolioStore {

    const val MAX_DEPOSIT = 100_000.0

    const val MAX_CASH = 1_000_000.0

    val STARTING_BALANCES = listOf(500.0, 1_000.0, 5_000.0, 10_000.0, 100_000.0)

    private var nextId = 1L

    private val _profiles = mutableStateListOf<Profile>()

    val profiles: List<Profile> get() = _profiles

    var active: Profile by mutableStateOf(newProfile("profile #1", 1_000.0))
        private set

    var accountCreatedAt by mutableStateOf(System.currentTimeMillis())
        private set

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private var uid: String? = null

    var loaded by mutableStateOf(false)
        private set

    var nickname by mutableStateOf("Alex")
        private set
    var email by mutableStateOf("alex@example.com")
        private set

    var lastTradeDelta by mutableStateOf<Double?>(null)
        private set
    var lastTradeTimestamp by mutableStateOf(0L)
        private set

    var cashCapTick by mutableStateOf(0)
        private set

    init {
        _profiles.add(active)
    }

    fun bindUser(uid: String) {
        this.uid = uid
    }

    suspend fun load(uid: String) {
        this.uid = uid
        try {
            val snapshot = PortfolioRepository.load(uid)
            if (snapshot.profiles.isNotEmpty()) {
                _profiles.clear()
                _profiles.addAll(snapshot.profiles)
                nextId = _profiles.maxOf { it.id } + 1
                active = snapshot.profiles.firstOrNull { it.id == snapshot.activeProfileId }
                    ?: snapshot.profiles.first()
                snapshot.nickname?.let { nickname = it }
                snapshot.createdAt?.let { accountCreatedAt = it }
            } else {
                seedDefaultProfile()
            }
        } catch (_: Throwable) {
            if (_profiles.size <= 1 && _profiles.firstOrNull()?.transactions.isNullOrEmpty()) {
                seedDefaultProfile()
            }
        }
        loaded = true
    }

    fun reset() {
        uid = null
        loaded = false
        nextId = 1L
        val placeholder = newProfile("profile #1", 1_000.0)
        _profiles.clear()
        _profiles.add(placeholder)
        active = placeholder
        accountCreatedAt = System.currentTimeMillis()
        lastTradeDelta = null
    }

    private fun seedDefaultProfile() {
        val profile = newProfile("profile #1", 1_000.0)
        _profiles.clear()
        _profiles.add(profile)
        active = profile
        persistProfile(profile)
        persistAccount()
    }

    private fun persistProfile(profile: Profile) {
        val uid = uid ?: return
        scope.launch { runCatching { PortfolioRepository.saveProfile(uid, profile) } }
    }

    private fun persistAccount() {
        val uid = uid ?: return
        scope.launch {
            runCatching { PortfolioRepository.saveAccount(uid, nickname, accountCreatedAt, active.id) }
        }
    }

    private fun deleteProfileDoc(profileId: Long) {
        val uid = uid ?: return
        scope.launch { runCatching { PortfolioRepository.deleteProfile(uid, profileId) } }
    }

    val cash: Double get() = active.cash

    val positions: Map<String, Holding> get() = active.holdings

    val transactions: List<Transaction> get() = active.transactions

    val cashEntries: List<CashEntry> get() = active.cashEntries

    fun holding(symbol: String): Holding? = active.holdings[symbol]

    private fun newProfile(name: String, startingBalance: Double): Profile =
        Profile(id = nextId++, name = name, startingBalance = startingBalance)

    fun defaultProfileName(): String = "profile #${_profiles.size + 1}"

    fun completeOnboarding(nickname: String, profileName: String, startingBalance: Double) {
        updateNickname(nickname)
        val trimmed = profileName.trim().ifBlank { "profile #1" }
        val balance = startingBalance.coerceIn(0.0, MAX_CASH)
        val profile = newProfile(trimmed, balance)
        _profiles.clear()
        _profiles.add(profile)
        active = profile
        loaded = true
        persistProfile(profile)
        persistAccount()
    }

    fun createProfile(name: String, startingBalance: Double) {
        val trimmed = name.trim().ifBlank { defaultProfileName() }
        val balance = startingBalance.coerceIn(0.0, MAX_CASH)
        val profile = newProfile(trimmed, balance)
        _profiles.add(profile)
        active = profile
        persistProfile(profile)
        persistAccount()
    }

    fun selectProfile(profile: Profile) {
        if (profile == active) return
        active = profile
        persistAccount()
    }

    fun cycleActiveProfile(offset: Int) {
        if (_profiles.size <= 1) return
        val index = _profiles.indexOf(active)
        if (index < 0) return
        val size = _profiles.size
        val next = ((index + offset) % size + size) % size
        selectProfile(_profiles[next])
    }

    fun renameProfile(profile: Profile, name: String) {
        val trimmed = name.trim()
        if (trimmed.isEmpty()) return
        profile.name = trimmed
        persistProfile(profile)
    }

    fun deleteProfile(profile: Profile) {
        if (_profiles.size <= 1) return
        val index = _profiles.indexOf(profile)
        if (index < 0) return
        _profiles.removeAt(index)
        if (active.id == profile.id) {
            active = _profiles[index.coerceAtMost(_profiles.lastIndex)]
        }
        deleteProfileDoc(profile.id)
        persistAccount()
    }

    fun updateNickname(value: String) {
        value.trim().takeIf { it.isNotEmpty() }?.let {
            nickname = it
            persistAccount()
        }
    }

    fun updateEmail(value: String) { value.trim().takeIf { it.isNotEmpty() }?.let { email = it } }

    fun addCash(amount: Double) {
        if (amount <= 0.0) return
        val deposit = amount.coerceAtMost(MAX_DEPOSIT)
        active.cash += deposit
        val now = System.currentTimeMillis()
        active.cashEntries.add(CashEntry(CashFlowType.DEPOSIT, deposit, now))
        markDelta(deposit, now)
        enforceCashCap()
        persistProfile(active)
    }

    private fun enforceCashCap() {
        val excess = active.cash - MAX_CASH
        if (excess <= 0.0) return
        active.cash = MAX_CASH
        val now = System.currentTimeMillis()
        active.cashEntries.add(CashEntry(CashFlowType.WITHDRAWAL, excess, now))
        markDelta(-excess, now)
        cashCapTick++
    }

    fun withdrawCash(amount: Double) {
        if (amount <= 0.0) return
        val take = amount.coerceAtMost(active.cash)
        if (take <= 0.0) return
        active.cash -= take
        val now = System.currentTimeMillis()
        active.cashEntries.add(CashEntry(CashFlowType.WITHDRAWAL, take, now))
        markDelta(-take, now)
        persistProfile(active)
    }

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
        persistProfile(active)
    }

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
        persistProfile(active)
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
}
