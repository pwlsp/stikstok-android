package com.example.tikstok.ui.account

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.ArrowDropUp
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.tikstok.R
import com.example.tikstok.data.portfolio.PortfolioStore
import com.example.tikstok.data.portfolio.Profile
import com.example.tikstok.ui.invest.DownColor
import com.example.tikstok.ui.invest.UpColor
import com.example.tikstok.ui.invest.formatMonthYear
import com.example.tikstok.ui.invest.formatSignedPercent
import com.example.tikstok.ui.invest.formatSignedUsd
import com.example.tikstok.ui.invest.formatUsd

/**
 * Account home (deck slide 8): the user header with a settings entry point, the list of profiles —
 * each its own isolated portfolio, the active one highlighted — and a card to spin up a new one.
 * Tapping a profile makes it active; the whole app then follows that profile.
 */
@Composable
fun AccountScreen(
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: AccountViewModel = viewModel(),
) {
    var showCreate by remember { mutableStateOf(false) }

    // Re-price when a trade happens, a profile is added, or the active profile changes.
    val tradeTick = PortfolioStore.lastTradeTimestamp
    val profileCount = PortfolioStore.profiles.size
    LaunchedEffect(tradeTick, profileCount) { viewModel.refresh(PortfolioStore.profiles) }

    val rowsById = viewModel.rows.associateBy { it.profile.id }

    LazyColumn(
        modifier = modifier.fillMaxWidth(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            AccountHeader(
                nickname = PortfolioStore.nickname,
                profileCount = profileCount,
                joined = formatMonthYear(PortfolioStore.accountCreatedAt),
                onOpenSettings = onOpenSettings,
            )
        }
        item { SectionLabel(stringResource(R.string.account_your_profiles)) }
        items(PortfolioStore.profiles, key = { it.id }) { profile ->
            ProfileCard(
                profile = profile,
                isActive = profile.id == PortfolioStore.active.id,
                portfolioValue = rowsById[profile.id]?.portfolioValue ?: profile.cash,
                profit = rowsById[profile.id]?.profit ?: 0.0,
                profitPct = rowsById[profile.id]?.profitPct ?: 0.0,
                onClick = { PortfolioStore.selectProfile(profile) },
            )
        }
        item { CreateProfileCard(onClick = { showCreate = true }) }
    }

    if (showCreate) {
        CreateProfileDialog(
            onCreate = { name, balance ->
                PortfolioStore.createProfile(name, balance)
                showCreate = false
            },
            onDismiss = { showCreate = false },
        )
    }
}

@Composable
private fun AccountHeader(
    nickname: String,
    profileCount: Int,
    joined: String,
    onOpenSettings: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        AccountAvatar(nickname)
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(
                text = nickname,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
            )
            Text(
                text = stringResource(R.string.account_profiles_meta, profileCount, joined),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        IconButton(onClick = onOpenSettings) {
            Icon(
                imageVector = Icons.Filled.Settings,
                contentDescription = stringResource(R.string.settings_title),
            )
        }
    }
}

@Composable
private fun AccountAvatar(nickname: String) {
    Box(
        modifier = Modifier
            .size(48.dp)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.primary),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = nickname.trim().firstOrNull()?.uppercase() ?: "?",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = Color.White,
        )
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(
        text = text.uppercase(),
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(top = 4.dp, start = 4.dp),
    )
}

@Composable
private fun ProfileCard(
    profile: Profile,
    isActive: Boolean,
    portfolioValue: Double,
    profit: Double,
    profitPct: Double,
    onClick: () -> Unit,
) {
    val up = profit >= 0.0
    val plColor = if (up) UpColor else DownColor
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        border = if (isActive) BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary) else null,
    ) {
        Column(Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                ProfileBadge(profile.name, isActive)
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        text = profile.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = if (up) Icons.Filled.ArrowDropUp else Icons.Filled.ArrowDropDown,
                            contentDescription = null,
                            tint = plColor,
                            modifier = Modifier.size(18.dp),
                        )
                        Text(
                            text = formatSignedUsd(profit) + " " + stringResource(R.string.account_all_time),
                            style = MaterialTheme.typography.bodySmall,
                            color = plColor,
                            maxLines = 1,
                        )
                    }
                }
                if (isActive) ActiveBadge()
            }
            Spacer(Modifier.height(12.dp))
            StatsPanel(
                cash = profile.cash,
                portfolioValue = portfolioValue,
                profitPct = profitPct,
                plColor = plColor,
            )
        }
    }
}

@Composable
private fun ProfileBadge(name: String, isActive: Boolean) {
    val bg = if (isActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant
    val fg = if (isActive) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
    Box(
        modifier = Modifier
            .size(40.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(bg),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = name.trim().firstOrNull()?.uppercase() ?: "#",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = fg,
        )
    }
}

@Composable
private fun ActiveBadge() {
    Text(
        text = stringResource(R.string.account_active).uppercase(),
        style = MaterialTheme.typography.labelSmall,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.18f))
            .padding(horizontal = 8.dp, vertical = 3.dp),
    )
}

@Composable
private fun StatsPanel(cash: Double, portfolioValue: Double, profitPct: Double, plColor: Color) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surfaceContainerLowest)
            .padding(vertical = 12.dp, horizontal = 14.dp),
    ) {
        Stat(
            label = stringResource(R.string.account_stat_cash),
            value = "$" + formatUsd(cash),
            modifier = Modifier.weight(1f),
        )
        Stat(
            label = stringResource(R.string.account_stat_portfolio),
            value = "$" + formatUsd(portfolioValue),
            modifier = Modifier.weight(1f),
        )
        Stat(
            label = stringResource(R.string.account_stat_pl),
            value = formatSignedPercent(profitPct.toFloat()),
            valueColor = plColor,
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun Stat(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    valueColor: Color? = null,
) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(3.dp)) {
        Text(
            text = label.uppercase(),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = value,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
            color = valueColor ?: MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
        )
    }
}

@Composable
private fun CreateProfileCard(onClick: () -> Unit) {
    val outline = MaterialTheme.colorScheme.outline
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .dashedBorder(outline)
            .clickable(onClick = onClick)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant),
            contentAlignment = Alignment.Center,
        ) {
            Icon(Icons.Filled.Add, contentDescription = null)
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(
                text = stringResource(R.string.account_create_profile),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                text = stringResource(R.string.account_create_profile_sub),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Icon(
            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun CreateProfileDialog(
    onCreate: (String, Double) -> Unit,
    onDismiss: () -> Unit,
) {
    var name by remember { mutableStateOf(PortfolioStore.defaultProfileName()) }
    var balance by remember { mutableStateOf(1_000.0) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.account_create_title)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    singleLine = true,
                    label = { Text(stringResource(R.string.account_profile_name)) },
                    modifier = Modifier.fillMaxWidth(),
                )
                Text(
                    text = stringResource(R.string.account_starting_balance).uppercase(),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    PortfolioStore.STARTING_BALANCES.forEach { option ->
                        FilterChip(
                            selected = balance == option,
                            onClick = { balance = option },
                            label = { Text("$" + formatUsd(option).removeSuffix(".00")) },
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { onCreate(name, balance) }) {
                Text(stringResource(R.string.account_create))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) }
        },
    )
}

/** A rounded dashed outline, used to signal the "add a profile" affordance. */
private fun Modifier.dashedBorder(color: Color): Modifier = drawBehind {
    val stroke = Stroke(
        width = 1.5.dp.toPx(),
        pathEffect = PathEffect.dashPathEffect(floatArrayOf(12f, 10f)),
    )
    drawRoundRect(color = color, cornerRadius = CornerRadius(16.dp.toPx()), style = stroke)
}
