package com.example.tikstok.ui.invest

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.tikstok.data.portfolio.Holding
import com.example.tikstok.data.portfolio.PortfolioStore
import com.example.tikstok.data.repository.MarketRepository
import com.example.tikstok.model.Asset
import com.example.tikstok.model.Assets
import com.example.tikstok.model.Candle
import com.example.tikstok.model.Timeframe
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

enum class ChartMode { CANDLES, LINE }

data class InvestUiState(
    val asset: Asset = Assets.default,
    val timeframe: Timeframe = Timeframe.DEFAULT,
    val candles: List<Candle> = emptyList(),
    val currency: String = "USD",
    val selectedIndex: Int? = null,
    val chartMode: ChartMode = ChartMode.CANDLES,
    val isLoading: Boolean = false,
    val error: String? = null,
)

class InvestViewModel : ViewModel() {

    private val repository = MarketRepository()

    var uiState by mutableStateOf(InvestUiState())
        private set

    private var loadJob: Job? = null

    init {
        reload()
    }

    fun selectAsset(asset: Asset) {
        if (asset == uiState.asset) return
        uiState = uiState.copy(asset = asset, candles = emptyList(), selectedIndex = null)
        reload()
    }

    fun selectTimeframe(timeframe: Timeframe) {
        if (timeframe == uiState.timeframe) return
        uiState = uiState.copy(timeframe = timeframe, selectedIndex = null)
        reload()
    }

    fun selectCandle(index: Int?) {
        uiState = uiState.copy(selectedIndex = index)
    }

    fun toggleChartMode() {
        val next = if (uiState.chartMode == ChartMode.CANDLES) ChartMode.LINE else ChartMode.CANDLES
        uiState = uiState.copy(chartMode = next)
    }

    val cash: Double get() = PortfolioStore.cash

    val currentPrice: Double? get() = uiState.candles.lastOrNull()?.close?.toDouble()

    fun holding(): Holding? = PortfolioStore.holding(uiState.asset.symbol)

    fun buy(amountUsd: Double) {
        val price = currentPrice ?: return
        PortfolioStore.buy(uiState.asset.symbol, price, amountUsd)
    }

    fun sell(amountUsd: Double) {
        val price = currentPrice ?: return
        PortfolioStore.sell(uiState.asset.symbol, price, amountUsd)
    }

    fun reload() {
        loadJob?.cancel()
        uiState = uiState.copy(isLoading = true, error = null)
        loadJob = viewModelScope.launch {
            val asset = uiState.asset
            val timeframe = uiState.timeframe
            try {
                val series = repository.candles(asset.symbol, timeframe)
                if (asset != uiState.asset || timeframe != uiState.timeframe) return@launch
                uiState = uiState.copy(
                    candles = series.candles,
                    currency = series.currency,
                    selectedIndex = null,
                    isLoading = false,
                    error = null,
                )
            } catch (t: Throwable) {
                if (asset != uiState.asset || timeframe != uiState.timeframe) return@launch
                uiState = uiState.copy(isLoading = false, error = t.message ?: "Failed to load")
            }
        }
    }
}
