package com.example.tikstok.ui.portfolio

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Card
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import com.example.tikstok.R
import com.example.tikstok.data.portfolio.PortfolioStore
import com.example.tikstok.data.portfolio.Transaction
import com.example.tikstok.data.portfolio.TransactionType
import com.example.tikstok.model.Asset
import com.example.tikstok.model.Assets
import com.example.tikstok.ui.invest.AssetAvatar
import com.example.tikstok.ui.invest.DownColor
import com.example.tikstok.ui.invest.UpColor
import com.example.tikstok.ui.invest.formatDateTime
import com.example.tikstok.ui.invest.formatPrice
import com.example.tikstok.ui.invest.formatUnits
import com.example.tikstok.ui.invest.formatUsd

@Composable
fun TransactionHistoryScreen(
    initialSymbol: String?,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val transactions = PortfolioStore.transactions
    val assets = remember(transactions.size) {
        val present = transactions.map { it.symbol }.toSet()
        Assets.all.filter { it.symbol in present }
    }
    var selected by remember { mutableStateOf(setOfNotNull(initialSymbol)) }

    val visible = transactions
        .filter { selected.isEmpty() || it.symbol in selected }
        .sortedByDescending { it.timestamp }

    Column(modifier = modifier.fillMaxSize()) {
        HistoryHeader(onBack = onBack)
        FilterRow(
            assets = assets,
            selected = selected,
            onAll = { selected = emptySet() },
            onToggle = { symbol ->
                selected = if (symbol in selected) selected - symbol else selected + symbol
            },
        )
        if (visible.isEmpty()) {
            EmptyHistory()
        } else {
            LazyColumn(
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(visible) { tx -> TransactionRow(tx) }
            }
        }
    }
}

@Composable
private fun HistoryHeader(onBack: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 4.dp, end = 16.dp, top = 4.dp, bottom = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(onClick = onBack) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = stringResource(R.string.cd_back),
            )
        }
        Spacer(Modifier.width(4.dp))
        Text(
            text = stringResource(R.string.history_title),
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
        )
    }
}

@Composable
private fun FilterRow(
    assets: List<Asset>,
    selected: Set<String>,
    onAll: () -> Unit,
    onToggle: (String) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        FilterChip(
            selected = selected.isEmpty(),
            onClick = onAll,
            label = { Text(stringResource(R.string.history_filter_all)) },
        )
        assets.forEach { asset ->
            FilterChip(
                selected = asset.symbol in selected,
                onClick = { onToggle(asset.symbol) },
                label = { Text(asset.ticker) },
            )
        }
    }
}

@Composable
private fun TransactionRow(tx: Transaction) {
    val asset = Assets.bySymbol(tx.symbol)
    val isBuy = tx.type == TransactionType.BUY
    val sideColor = if (isBuy) UpColor else DownColor
    Card(Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (asset != null) {
                AssetAvatar(asset, size = 40.dp)
                Spacer(Modifier.width(12.dp))
            }
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    SideBadge(isBuy = isBuy, color = sideColor)
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = asset?.ticker ?: tx.symbol,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
                Spacer(Modifier.height(2.dp))
                Text(
                    text = formatDateTime(tx.timestamp),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                )
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = (if (isBuy) "-" else "+") + "$" + formatUsd(tx.amount),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = sideColor,
                    maxLines = 1,
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text = formatUnits(tx.quantity) + " @ $" + formatPrice(tx.unitPrice.toFloat()),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                )
            }
        }
    }
}

@Composable
private fun SideBadge(isBuy: Boolean, color: Color) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(color.copy(alpha = 0.18f))
            .padding(horizontal = 8.dp, vertical = 2.dp),
    ) {
        Text(
            text = stringResource(if (isBuy) R.string.invest_buy else R.string.invest_sell).uppercase(),
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = color,
        )
    }
}

@Composable
private fun EmptyHistory() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = stringResource(R.string.history_empty),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
