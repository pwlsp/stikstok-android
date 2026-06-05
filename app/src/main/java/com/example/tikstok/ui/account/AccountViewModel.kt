package com.example.tikstok.ui.account

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.tikstok.data.portfolio.Profile
import com.example.tikstok.data.repository.MarketRepository
import com.example.tikstok.model.Timeframe
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch

/**
 * One profile enriched with the live value of its holdings so the Account list can show each
 * profile's portfolio value and all-time P/L. P/L is the current total value (cash + holdings)
 * measured against the net cash ever put in.
 */
data class ProfileRow(
    val profile: Profile,
    /** Cash plus the live market value of every holding. */
    val portfolioValue: Double,
) {
    val cash: Double get() = profile.cash
    val profit: Double get() = portfolioValue - profile.netDeposits
    val profitPct: Double
        get() = profile.netDeposits.takeIf { it > 0.0 }?.let { profit / it * 100.0 } ?: 0.0
}

/**
 * Prices every profile's holdings so the Account screen can rank them by performance. Mirrors
 * [com.example.tikstok.ui.portfolio.PortfolioViewModel], but across all profiles at once: each
 * distinct symbol is fetched a single time and shared.
 */
class AccountViewModel : ViewModel() {

    private val repository = MarketRepository()

    var rows by mutableStateOf<List<ProfileRow>>(emptyList())
        private set
    var isLoading by mutableStateOf(false)
        private set

    private var loadJob: Job? = null

    /** Re-prices every profile. Call on first show, after creating a profile, or on manual refresh. */
    fun refresh(profiles: List<Profile>) {
        loadJob?.cancel()
        // Only seed cost-basis values on the very first load; afterwards keep the last priced rows
        // visible while the refetch runs, so a refresh doesn't flash the cards back to a guess.
        if (rows.isEmpty()) {
            rows = profiles.map { ProfileRow(it, costBasisValue(it)) }
        }
        isLoading = true
        loadJob = viewModelScope.launch {
            val symbols = profiles.flatMap { it.holdings.keys }.toSet()
            val prices = coroutineScope {
                symbols.map { symbol ->
                    async { symbol to fetchPrice(symbol) }
                }.awaitAll()
            }.toMap()
            rows = profiles.map { profile ->
                val holdingsValue = profile.holdings.entries.sumOf { (symbol, holding) ->
                    holding.quantity * (prices[symbol] ?: holding.avgCost)
                }
                ProfileRow(profile, profile.cash + holdingsValue)
            }
            isLoading = false
        }
    }

    private suspend fun fetchPrice(symbol: String): Double? = runCatching {
        repository.candles(symbol, Timeframe.D1).candles.lastOrNull()?.close?.toDouble()
    }.getOrNull()

    /** Falls back to cost basis (the price paid) until live prices arrive. */
    private fun costBasisValue(profile: Profile): Double =
        profile.cash + profile.holdings.values.sumOf { it.quantity * it.avgCost }
}
