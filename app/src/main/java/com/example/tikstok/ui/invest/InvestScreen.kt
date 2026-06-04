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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
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

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(vertical = 16.dp),
    ) {
        // The chart card sits slightly wider (8dp insets) than the rest of the content (16dp).
        val sidePadding = Modifier.padding(horizontal = 16.dp)
        AssetHeader(state = state, onClick = { showPicker = true }, modifier = sidePadding)
        Spacer(Modifier.height(12.dp))
        ChartCard(
            state = state,
            onSelectCandle = viewModel::selectCandle,
            onRetry = viewModel::reload,
            modifier = Modifier.padding(horizontal = 8.dp),
        )
        Spacer(Modifier.height(12.dp))
        TimeframeSelector(
            selected = state.timeframe,
            onSelect = viewModel::selectTimeframe,
            modifier = sidePadding,
        )
        Spacer(Modifier.weight(1f))
        TradeButtons(modifier = sidePadding)
    }

    if (showPicker) {
        AssetPickerSheet(
            selected = state.asset,
            onSelect = {
                viewModel.selectAsset(it)
                showPicker = false
            },
            onDismiss = { showPicker = false },
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

        Spacer(Modifier.weight(1f))

        Column(horizontalAlignment = Alignment.End) {
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
    }
}

@Composable
private fun ChartCard(
    state: InvestUiState,
    onSelectCandle: (Int) -> Unit,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(modifier = modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(12.dp)) {
            val readoutCandle = state.candles.getOrNull(
                state.selectedIndex ?: state.candles.lastIndex,
            )
            OhlcReadout(candle = readoutCandle, timeframe = state.timeframe)
            Spacer(Modifier.height(8.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(290.dp),
                contentAlignment = Alignment.Center,
            ) {
                when {
                    state.candles.isNotEmpty() -> CandlestickChart(
                        candles = state.candles,
                        selectedIndex = state.selectedIndex,
                        onSelect = onSelectCandle,
                        timeframe = state.timeframe,
                        modifier = Modifier.fillMaxSize(),
                    )

                    state.isLoading -> CircularProgressIndicator()
                    state.error != null -> ErrorState(onRetry = onRetry)
                }
            }
        }
    }
}

@Composable
private fun OhlcReadout(candle: Candle?, timeframe: Timeframe) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(
            text = candle?.let { formatTimestamp(it.timestamp, timeframe) } ?: "—",
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Medium,
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            OhlcItem(stringResource(R.string.ohlc_open), candle?.open)
            OhlcItem(stringResource(R.string.ohlc_high), candle?.high)
            OhlcItem(stringResource(R.string.ohlc_low), candle?.low)
            OhlcItem(
                label = stringResource(R.string.ohlc_close),
                value = candle?.close,
                valueColor = candle?.let { if (it.close >= it.open) UpColor else DownColor },
            )
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
private fun TradeButtons(modifier: Modifier = Modifier) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Button(
            onClick = { /* TODO: open the trade screen */ },
            modifier = Modifier.weight(1f),
            colors = ButtonDefaults.buttonColors(containerColor = UpColor),
        ) {
            Text(stringResource(R.string.invest_buy))
        }
        Button(
            onClick = { /* TODO: open the trade screen */ },
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
