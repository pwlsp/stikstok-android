package com.example.tikstok.ui.portfolio

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.tikstok.data.repository.MarketRepository
import com.example.tikstok.model.Asset
import com.example.tikstok.model.Assets
import com.example.tikstok.model.Timeframe
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch

/** One holding enriched with its live price so the screen can show value and unrealized P/L. */
data class HoldingRow(
    val asset: Asset,
    val quantity: Double,
    val avgCost: Double,
    val currentPrice: Double,
) {
    val value: Double get() = quantity * currentPrice
    val costBasis: Double get() = quantity * avgCost
    val profit: Double get() = value - costBasis
    val profitPct: Double get() = if (avgCost > 0.0) (currentPrice - avgCost) / avgCost * 100.0 else 0.0
}

data class PortfolioUiState(
    val isLoading: Boolean = false,
    val rows: List<HoldingRow> = emptyList(),
) {
    val totalValue: Double get() = rows.sumOf { it.value }
    val totalCost: Double get() = rows.sumOf { it.costBasis }
    val totalProfit: Double get() = totalValue - totalCost
    val totalProfitPct: Double get() = if (totalCost > 0.0) totalProfit / totalCost * 100.0 else 0.0
}

class PortfolioViewModel : ViewModel() {

    private val repository = MarketRepository()

    var uiState by mutableStateOf(PortfolioUiState())
        private set

    private var loadJob: Job? = null

    /** Re-prices every open position. Call when the screen appears or after a trade. */
    fun refresh(positions: Map<String, com.example.tikstok.data.portfolio.Holding>) {
        loadJob?.cancel()
        if (positions.isEmpty()) {
            uiState = PortfolioUiState(isLoading = false, rows = emptyList())
            return
        }
        uiState = uiState.copy(isLoading = true)
        loadJob = viewModelScope.launch {
            val rows = coroutineScope {
                positions.mapNotNull { (symbol, holding) ->
                    val asset = Assets.bySymbol(symbol) ?: return@mapNotNull null
                    async {
                        val price = runCatching {
                            repository.candles(symbol, Timeframe.D1).candles.lastOrNull()?.close?.toDouble()
                        }.getOrNull() ?: holding.avgCost // fall back to cost basis if the fetch fails
                        HoldingRow(asset, holding.quantity, holding.avgCost, price)
                    }
                }.awaitAll()
            }.sortedByDescending { it.value }
            uiState = PortfolioUiState(isLoading = false, rows = rows)
        }
    }
}
