package com.example.tikstok.ui.invest

import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.CandlestickChart
import androidx.compose.material.icons.filled.ShowChart
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.tikstok.R
import com.example.tikstok.model.Candle
import com.example.tikstok.model.Timeframe

@Composable
fun InvestScreen(
    modifier: Modifier = Modifier,
    viewModel: InvestViewModel = viewModel(),
) {
    val state = viewModel.uiState
    var showPicker by remember { mutableStateOf(false) }
    var tradeSide by remember { mutableStateOf<TradeSide?>(null) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(vertical = 16.dp),
    ) {
        // The chart card sits slightly wider (8dp insets) than the rest of the content (16dp).
        val sidePadding = Modifier.padding(horizontal = 16.dp)
        ChartCard(
            state = state,
            onSelectCandle = viewModel::selectCandle,
            onRetry = viewModel::reload,
            onToggleMode = viewModel::toggleChartMode,
            modifier = Modifier.padding(horizontal = 8.dp),
        )
        Spacer(Modifier.height(12.dp))
        TimeframeSelector(
            selected = state.timeframe,
            onSelect = viewModel::selectTimeframe,
            modifier = sidePadding,
        )
        Spacer(Modifier.height(16.dp))
        AssetHeader(state = state, onClick = { showPicker = true }, modifier = sidePadding)
        Spacer(Modifier.weight(1f))
        TradeButtons(
            enabled = state.candles.isNotEmpty(),
            onBuy = { tradeSide = TradeSide.BUY },
            onSell = { tradeSide = TradeSide.SELL },
            modifier = sidePadding,
        )
    }

    if (showPicker) {
        AssetPickerSheet(
            selected = state.asset,
            // Keep the sheet open; the chart behind it updates live as assets are tapped.
            onSelect = { viewModel.selectAsset(it) },
            onDismiss = { showPicker = false },
        )
    }

    val side = tradeSide
    val price = viewModel.currentPrice
    if (side != null && price != null) {
        val first = state.candles.firstOrNull()
        val latest = state.candles.lastOrNull()
        val changePct = if (latest != null && first != null && first.open != 0f) {
            (latest.close - first.open) / first.open * 100f
        } else {
            0f
        }
        TradeSheet(
            asset = state.asset,
            price = price,
            changePct = changePct,
            initialSide = side,
            cash = viewModel.cash,
            holding = viewModel.holding(),
            onBuy = viewModel::buy,
            onSell = viewModel::sell,
            onDismiss = { tradeSide = null },
        )
    }
}

@Composable
private fun AssetHeader(state: InvestUiState, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val latest = state.candles.lastOrNull()
    val first = state.candles.firstOrNull()
    val changePct = if (latest != null && first != null && first.open != 0f) {
        (latest.close - first.open) / first.open * 100f
    } else {
        0f
    }

    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column {
            Text(
                text = latest?.let { "$" + formatPrice(it.close) } ?: "—",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
            )
            Text(
                text = formatSignedPercent(changePct),
                style = MaterialTheme.typography.bodyMedium,
                color = if (changePct >= 0f) UpColor else DownColor,
            )
        }

        Spacer(Modifier.weight(1f))

        Row(
            modifier = Modifier
                .clip(RoundedCornerShape(12.dp))
                .clickable(onClick = onClick)
                .padding(4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            AssetAvatar(state.asset)
            Spacer(Modifier.width(8.dp))
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = state.asset.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Icon(
                        imageVector = Icons.Filled.ArrowDropDown,
                        contentDescription = stringResource(R.string.cd_change_asset),
                    )
                }
                Text(
                    text = state.asset.ticker,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun ChartCard(
    state: InvestUiState,
    onSelectCandle: (Int) -> Unit,
    onRetry: () -> Unit,
    onToggleMode: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(modifier = modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(12.dp)) {
            val readoutCandle = state.candles.getOrNull(
                state.selectedIndex ?: state.candles.lastIndex,
            )
            Row(verticalAlignment = Alignment.Top) {
                OhlcReadout(
                    candle = readoutCandle,
                    timeframe = state.timeframe,
                    modifier = Modifier.weight(1f),
                )
                // Candles vs. line-overview toggle — only the scrollable (1d+) timeframes need it,
                // but always reserve its 48dp slot so the header (and everything below the card)
                // keeps the same height across timeframes and nothing jumps on a frame change.
                Box(Modifier.size(48.dp), contentAlignment = Alignment.Center) {
                    if (state.timeframe.isScrollable) {
                        ChartModeToggle(mode = state.chartMode, onToggle = onToggleMode)
                    }
                }
            }
            Spacer(Modifier.height(8.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(290.dp),
                contentAlignment = Alignment.Center,
            ) {
                val showLine = state.chartMode == ChartMode.LINE && state.timeframe.isScrollable
                when {
                    state.candles.isEmpty() && state.isLoading -> CircularProgressIndicator()
                    state.candles.isEmpty() && state.error != null -> ErrorState(onRetry = onRetry)
                    showLine -> LineChart(
                        candles = state.candles,
                        selectedIndex = state.selectedIndex,
                        onSelect = onSelectCandle,
                        timeframe = state.timeframe,
                        modifier = Modifier.fillMaxSize(),
                    )

                    state.candles.isNotEmpty() -> CandlestickChart(
                        candles = state.candles,
                        selectedIndex = state.selectedIndex,
                        onSelect = onSelectCandle,
                        timeframe = state.timeframe,
                        modifier = Modifier.fillMaxSize(),
                    )
                }
            }
        }
    }
}

@Composable
private fun ChartModeToggle(mode: ChartMode, onToggle: () -> Unit) {
    val (icon, descRes) = when (mode) {
        ChartMode.CANDLES -> Icons.Filled.ShowChart to R.string.invest_show_line
        ChartMode.LINE -> Icons.Filled.CandlestickChart to R.string.invest_show_candles
    }
    FilledTonalIconButton(onClick = onToggle) {
        Icon(imageVector = icon, contentDescription = stringResource(descRes))
    }
}

@Composable
private fun OhlcReadout(candle: Candle?, timeframe: Timeframe, modifier: Modifier = Modifier) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(
            text = candle?.let { formatTimestamp(it.timestamp, timeframe) } ?: "—",
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Medium,
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                OhlcItem(stringResource(R.string.ohlc_open), candle?.open)
                OhlcItem(stringResource(R.string.ohlc_low), candle?.low)
            }
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                OhlcItem(
                    label = stringResource(R.string.ohlc_close),
                    value = candle?.close,
                    valueColor = candle?.let { if (it.close >= it.open) UpColor else DownColor },
                )
                OhlcItem(stringResource(R.string.ohlc_high), candle?.high)
            }
        }
    }
}

@Composable
private fun OhlcItem(label: String, value: Float?, valueColor: Color? = null) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = value?.let(::formatPrice) ?: "—",
            style = MaterialTheme.typography.labelLarge,
            color = valueColor ?: MaterialTheme.colorScheme.onSurface,
        )
    }
}

@Composable
private fun TimeframeSelector(
    selected: Timeframe,
    onSelect: (Timeframe) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Timeframe.entries.forEach { timeframe ->
            FilterChip(
                selected = timeframe == selected,
                onClick = { onSelect(timeframe) },
                label = { Text(timeframe.label) },
            )
        }
    }
}

@Composable
private fun TradeButtons(
    enabled: Boolean,
    onBuy: () -> Unit,
    onSell: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Button(
            onClick = onBuy,
            enabled = enabled,
            modifier = Modifier.weight(1f),
            colors = ButtonDefaults.buttonColors(containerColor = UpColor),
        ) {
            Text(stringResource(R.string.invest_buy))
        }
        Button(
            onClick = onSell,
            enabled = enabled,
            modifier = Modifier.weight(1f),
            colors = ButtonDefaults.buttonColors(containerColor = DownColor),
        ) {
            Text(stringResource(R.string.invest_sell))
        }
    }
}

@Composable
private fun ErrorState(onRetry: () -> Unit) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            text = stringResource(R.string.invest_error),
            color = MaterialTheme.colorScheme.error,
        )
        Button(onClick = onRetry) {
            Text(stringResource(R.string.invest_retry))
        }
    }
}
