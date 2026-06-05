package com.example.tikstok.ui.portfolio

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.tikstok.R
import com.example.tikstok.data.portfolio.PortfolioStore
import com.example.tikstok.ui.invest.AssetAvatar
import com.example.tikstok.ui.invest.DownColor
import com.example.tikstok.ui.invest.UpColor
import com.example.tikstok.ui.invest.formatSignedPercent
import com.example.tikstok.ui.invest.formatSignedUsd
import com.example.tikstok.ui.invest.formatUnits
import com.example.tikstok.ui.invest.formatUsd

/**
 * Portfolio overview (deck slide 7): cash balance with top-up, total portfolio value with P/L, and
 * the holdings list. Tapping the total value opens the full transaction history; tapping a holding
 * opens the history pre-filtered to that asset.
 *
 * @param onOpenHistory navigates to the history screen; null symbol means "show everything".
 */
@Composable
fun PortfolioScreen(
    onOpenHistory: (String?) -> Unit,
    onOpenCashHistory: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: PortfolioViewModel = viewModel(),
) {
    val state = viewModel.uiState
    var showTopUp by remember { mutableStateOf(false) }

    // Re-price whenever a trade or top-up happens (and on first show); reading these snapshot
    // values here is what makes the effect re-run.
    val tradeTick = PortfolioStore.lastTradeTimestamp
    LaunchedEffect(tradeTick) { viewModel.refresh(PortfolioStore.positions) }

    LazyColumn(
        modifier = modifier.fillMaxWidth(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            CashCard(
                cash = PortfolioStore.cash,
                onAdd = { showTopUp = true },
                onClick = onOpenCashHistory,
            )
        }
        item {
            TotalValueCard(
                value = state.totalValue,
                profit = state.totalProfit,
                profitPct = state.totalProfitPct,
                onClick = { onOpenHistory(null) },
            )
        }
        item { HoldingsHeader(count = state.rows.size) }

        when {
            state.rows.isEmpty() && state.isLoading -> item { LoadingRow() }
            state.rows.isEmpty() -> item { EmptyHoldings() }
            else -> items(state.rows, key = { it.asset.symbol }) { row ->
                HoldingRowCard(row = row, onClick = { onOpenHistory(row.asset.symbol) })
            }
        }
    }

    if (showTopUp) {
        TopUpDialog(
            onPick = { PortfolioStore.addCash(it); showTopUp = false },
            onDismiss = { showTopUp = false },
        )
    }
}

@Composable
private fun CashCard(cash: Double, onAdd: () -> Unit, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, end = 8.dp, top = 12.dp, bottom = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(UpColor.copy(alpha = 0.18f)),
                contentAlignment = Alignment.Center,
            ) {
                Text("$", color = UpColor, fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    text = stringResource(R.string.portfolio_cash_balance).uppercase(),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text = "$" + formatUsd(cash),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                )
            }
            TextButton(onClick = onAdd) {
                Text("+ " + stringResource(R.string.portfolio_add))
            }
        }
    }
}

@Composable
private fun TotalValueCard(value: Double, profit: Double, profitPct: Double, onClick: () -> Unit) {
    val up = profit >= 0.0
    val color = if (up) UpColor else DownColor
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Text(
                text = stringResource(R.string.portfolio_total_value),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = "$" + formatUsd(value),
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Bold,
            )
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(50))
                    .background(color.copy(alpha = 0.15f))
                    .padding(horizontal = 10.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Icon(
                    imageVector = if (up) Icons.Filled.ArrowUpward else Icons.Filled.ArrowDownward,
                    contentDescription = null,
                    tint = color,
                    modifier = Modifier.size(14.dp),
                )
                Text(
                    text = formatSignedUsd(profit) + " (" + formatSignedPercent(profitPct.toFloat()) + ")",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = color,
                    maxLines = 1,
                )
            }
        }
    }
}

@Composable
private fun HoldingsHeader(count: Int) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 4.dp, start = 4.dp, end = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = stringResource(R.string.portfolio_holdings).uppercase() + " · $count",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = stringResource(R.string.portfolio_sort_value),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun HoldingRowCard(row: HoldingRow, onClick: () -> Unit) {
    val up = row.profit >= 0.0
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            AssetAvatar(row.asset, size = 40.dp)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    text = row.asset.ticker,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    text = formatUnits(row.quantity) + " " + row.asset.ticker,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                )
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "$" + formatUsd(row.value),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                )
                val color = if (up) UpColor else DownColor
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(50))
                        .background(color.copy(alpha = 0.15f))
                        .padding(horizontal = 6.dp, vertical = 2.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(2.dp),
                ) {
                    Icon(
                        imageVector = if (up) Icons.Filled.ArrowUpward else Icons.Filled.ArrowDownward,
                        contentDescription = null,
                        tint = color,
                        modifier = Modifier.size(10.dp),
                    )
                    Text(
                        text = formatSignedUsd(row.profit),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = color,
                        maxLines = 1,
                    )
                    Text(
                        text = "(" + formatSignedPercent(row.profitPct.toFloat()) + ")",
                        style = MaterialTheme.typography.labelSmall,
                        color = color,
                        maxLines = 1,
                    )
                }
            }
        }
    }
}

@Composable
private fun LoadingRow() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(24.dp),
        contentAlignment = Alignment.Center,
    ) {
        CircularProgressIndicator()
    }
}

@Composable
private fun EmptyHoldings() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 32.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = stringResource(R.string.portfolio_empty),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun TopUpDialog(onPick: (Double) -> Unit, onDismiss: () -> Unit) {
    val amounts = listOf(100.0, 500.0, 1_000.0, 5_000.0)
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.portfolio_add_cash)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                amounts.forEach { amount ->
                    Text(
                        text = "+ $" + formatUsd(amount),
                        style = MaterialTheme.typography.titleMedium,
                        color = UpColor,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .clickable { onPick(amount) }
                            .padding(vertical = 12.dp, horizontal = 8.dp),
                    )
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) }
        },
    )
}
