package com.example.tikstok.ui.portfolio

import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.VerticalDivider
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.tikstok.R
import com.example.tikstok.data.portfolio.CashEntry
import com.example.tikstok.data.portfolio.CashFlowType
import com.example.tikstok.data.portfolio.PortfolioStore
import com.example.tikstok.ui.invest.DownColor
import com.example.tikstok.ui.invest.UpColor
import com.example.tikstok.ui.invest.formatDateTime
import com.example.tikstok.ui.invest.formatUsd

/**
 * Cash-only ledger reached by tapping the cash-balance card: total deposited and withdrawn on top,
 * then the list of every deposit and withdrawal. Money in/out lives here; buys and sells live in the
 * transaction history instead.
 */
@Composable
fun CashHistoryScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val entries = PortfolioStore.cashEntries
    val deposited = entries.filter { it.type == CashFlowType.DEPOSIT }.sumOf { it.amount }
    val withdrawn = entries.filter { it.type == CashFlowType.WITHDRAWAL }.sumOf { it.amount }
    val visible = entries.sortedByDescending { it.timestamp }

    var showDeposit by remember { mutableStateOf(false) }
    var showWithdraw by remember { mutableStateOf(false) }

    Column(modifier = modifier.fillMaxSize()) {
        Header(onBack = onBack)
        LazyColumn(
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item { CurrentBalance(PortfolioStore.cash) }
            item { TotalsCard(deposited = deposited, withdrawn = withdrawn) }
            item {
                ActionButtons(
                    onDeposit = { showDeposit = true },
                    onWithdraw = { showWithdraw = true },
                )
            }
            if (visible.isEmpty()) {
                item { Empty() }
            } else {
                items(visible) { entry -> CashRow(entry) }
            }
        }
    }

    if (showDeposit) {
        AmountDialog(
            title = stringResource(R.string.cash_deposit_title),
            confirmLabel = stringResource(R.string.cash_deposit),
            max = PortfolioStore.MAX_DEPOSIT,
            supporting = stringResource(R.string.cash_max, "$" + formatUsd(PortfolioStore.MAX_DEPOSIT)),
            onConfirm = { PortfolioStore.addCash(it); showDeposit = false },
            onDismiss = { showDeposit = false },
        )
    }
    if (showWithdraw) {
        AmountDialog(
            title = stringResource(R.string.cash_withdraw_title),
            confirmLabel = stringResource(R.string.cash_withdraw),
            max = PortfolioStore.cash,
            supporting = stringResource(R.string.cash_available, "$" + formatUsd(PortfolioStore.cash)),
            onConfirm = { PortfolioStore.withdrawCash(it); showWithdraw = false },
            onDismiss = { showWithdraw = false },
        )
    }
}

@Composable
private fun CurrentBalance(cash: Double) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Text(
            text = stringResource(R.string.cash_current_balance).uppercase(),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = "$" + formatUsd(cash),
            style = MaterialTheme.typography.headlineLarge,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
        )
    }
}

@Composable
private fun Header(onBack: () -> Unit) {
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
            text = stringResource(R.string.cash_history_title),
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
        )
    }
}

@Composable
private fun TotalsCard(deposited: Double, withdrawn: Double) {
    Card(Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            TotalStat(
                label = stringResource(R.string.cash_deposited),
                value = deposited,
                color = UpColor,
                modifier = Modifier.weight(1f),
            )
            VerticalDivider(Modifier.height(40.dp))
            TotalStat(
                label = stringResource(R.string.cash_withdrawn),
                value = withdrawn,
                color = DownColor,
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun TotalStat(label: String, value: Double, color: Color, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Text(
            text = label.uppercase(),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = "$" + formatUsd(value),
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = color,
            maxLines = 1,
        )
    }
}

@Composable
private fun ActionButtons(onDeposit: () -> Unit, onWithdraw: () -> Unit) {
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Button(
            onClick = onDeposit,
            colors = ButtonDefaults.buttonColors(
                containerColor = UpColor.copy(alpha = 0.7f),
                contentColor = Color.White,
            ),
            modifier = Modifier.weight(1f),
        ) {
            Text(stringResource(R.string.cash_deposit))
        }
        Button(
            onClick = onWithdraw,
            colors = ButtonDefaults.buttonColors(
                containerColor = DownColor.copy(alpha = 0.7f),
                contentColor = Color.White,
            ),
            modifier = Modifier.weight(1f),
        ) {
            Text(stringResource(R.string.cash_withdraw))
        }
    }
}

@Composable
private fun CashRow(entry: CashEntry) {
    val isDeposit = entry.type == CashFlowType.DEPOSIT
    val color = if (isDeposit) UpColor else DownColor
    Card(Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(color.copy(alpha = 0.18f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = if (isDeposit) Icons.Filled.ArrowDownward else Icons.Filled.ArrowUpward,
                    contentDescription = null,
                    tint = color,
                    modifier = Modifier.size(20.dp),
                )
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    text = stringResource(
                        if (isDeposit) R.string.cash_entry_deposit else R.string.cash_entry_withdrawal,
                    ),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    text = formatDateTime(entry.timestamp),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                )
            }
            Text(
                text = (if (isDeposit) "+" else "-") + "$" + formatUsd(entry.amount),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = color,
                maxLines = 1,
            )
        }
    }
}

@Composable
private fun Empty() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 32.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = stringResource(R.string.cash_empty),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun AmountDialog(
    title: String,
    confirmLabel: String,
    max: Double,
    supporting: String,
    onConfirm: (Double) -> Unit,
    onDismiss: () -> Unit,
) {
    var input by remember { mutableStateOf("") }
    val amount = input.replace(',', '.').toDoubleOrNull()
    val valid = amount != null && amount > 0.0 && amount <= max
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            OutlinedTextField(
                value = input,
                onValueChange = { new -> input = new.filter { it.isDigit() || it == '.' || it == ',' } },
                singleLine = true,
                leadingIcon = { Text("$", style = MaterialTheme.typography.titleMedium) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                isError = input.isNotEmpty() && !valid,
                supportingText = { Text(supporting) },
                modifier = Modifier.fillMaxWidth(),
            )
        },
        confirmButton = {
            TextButton(onClick = { amount?.let(onConfirm) }, enabled = valid) {
                Text(confirmLabel)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) }
        },
    )
}
