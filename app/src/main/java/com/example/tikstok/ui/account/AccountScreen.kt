package com.example.tikstok.ui.account

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.ArrowDropUp
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
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
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.tikstok.R
import com.example.tikstok.data.portfolio.PortfolioStore
import com.example.tikstok.data.portfolio.Profile
import com.example.tikstok.ui.components.MoneyText
import com.example.tikstok.ui.invest.DownColor
import com.example.tikstok.ui.invest.UpColor
import com.example.tikstok.ui.invest.formatMonthYear
import com.example.tikstok.ui.invest.formatSignedPercent
import com.example.tikstok.ui.invest.formatSignedUsd
import com.example.tikstok.ui.invest.isFlatUsd
import com.example.tikstok.ui.invest.plColor
import com.example.tikstok.ui.invest.formatUsd

@Composable
fun AccountScreen(
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier,
    refreshTick: Int = 0,
    viewModel: AccountViewModel = viewModel(),
) {
    var showCreate by remember { mutableStateOf(false) }
    var managing by remember { mutableStateOf<Profile?>(null) }
    var deleting by remember { mutableStateOf<Profile?>(null) }

    val profileCount = PortfolioStore.profiles.size
    LaunchedEffect(refreshTick, profileCount) { viewModel.refresh(PortfolioStore.profiles) }

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
                onLongClick = { managing = profile },
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

    managing?.let { profile ->
        ManageProfileDialog(
            profile = profile,
            canDelete = PortfolioStore.profiles.size > 1,
            onSave = { PortfolioStore.renameProfile(profile, it) },
            onDelete = {
                deleting = profile
                managing = null
            },
            onDismiss = { managing = null },
        )
    }

    deleting?.let { profile ->
        DeleteProfileDialog(
            profile = profile,
            onConfirm = { PortfolioStore.deleteProfile(profile) },
            onDismiss = { deleting = null },
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
                text = pluralStringResource(
                    R.plurals.account_profiles_meta,
                    profileCount,
                    profileCount,
                    joined,
                ),
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

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun ProfileCard(
    profile: Profile,
    isActive: Boolean,
    portfolioValue: Double,
    profit: Double,
    profitPct: Double,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
) {
    val up = profit >= 0.0
    val flat = isFlatUsd(profit)
    val plColor = plColor(profit)
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(onClick = onClick, onLongClick = onLongClick),
        border = if (isActive) BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary) else null,
    ) {
        Column(Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(
                        text = profile.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (!flat) {
                            Icon(
                                imageVector = if (up) Icons.Filled.ArrowDropUp else Icons.Filled.ArrowDropDown,
                                contentDescription = null,
                                tint = plColor,
                                modifier = Modifier.size(18.dp),
                            )
                        }
                        MoneyText(
                            text = formatSignedUsd(profit) + " " + stringResource(R.string.account_all_time),
                            style = MaterialTheme.typography.bodySmall,
                            color = plColor,
                            modifier = Modifier.weight(1f, fill = false),
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
            .padding(vertical = 12.dp, horizontal = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
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
            modifier = Modifier.weight(0.7f),
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
        MoneyText(
            text = value,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
            color = valueColor ?: MaterialTheme.colorScheme.onSurface,
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
private fun ManageProfileDialog(
    profile: Profile,
    canDelete: Boolean,
    onSave: (String) -> Unit,
    onDelete: () -> Unit,
    onDismiss: () -> Unit,
) {
    var name by remember { mutableStateOf(profile.name) }
    val valid = name.trim().isNotEmpty()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.account_manage_title)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    singleLine = true,
                    label = { Text(stringResource(R.string.account_profile_name)) },
                    modifier = Modifier.fillMaxWidth(),
                )
                if (canDelete) {
                    TextButton(
                        onClick = onDelete,
                        colors = ButtonDefaults.textButtonColors(
                            contentColor = MaterialTheme.colorScheme.error,
                        ),
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.DeleteOutline,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp),
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(stringResource(R.string.account_delete_profile))
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    onSave(name)
                    onDismiss()
                },
                enabled = valid,
            ) {
                Text(stringResource(R.string.settings_save))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) }
        },
    )
}

@Composable
private fun DeleteProfileDialog(
    profile: Profile,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.account_delete_title)) },
        text = { Text(stringResource(R.string.account_delete_message, profile.name)) },
        confirmButton = {
            TextButton(
                onClick = {
                    onConfirm()
                    onDismiss()
                },
                colors = ButtonDefaults.textButtonColors(
                    contentColor = MaterialTheme.colorScheme.error,
                ),
            ) {
                Text(stringResource(R.string.action_delete))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) }
        },
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun CreateProfileDialog(
    onCreate: (String, Double) -> Unit,
    onDismiss: () -> Unit,
) {
    var name by remember { mutableStateOf(PortfolioStore.defaultProfileName()) }
    var amount by remember { mutableStateOf("1000") }
    val parsed = amount.replace(',', '.').toDoubleOrNull()
    val valid = parsed != null && parsed > 0.0 && parsed <= PortfolioStore.MAX_CASH

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.account_create_title)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    singleLine = true,
                    label = { Text(stringResource(R.string.account_profile_name)) },
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = amount,
                    onValueChange = { new ->
                        amount = new.filter { it.isDigit() || it == '.' || it == ',' }
                    },
                    singleLine = true,
                    label = { Text(stringResource(R.string.account_starting_balance)) },
                    leadingIcon = { Text("$", style = MaterialTheme.typography.titleMedium) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    isError = amount.isNotEmpty() && !valid,
                    supportingText = {
                        Text(
                            stringResource(
                                R.string.account_max_balance,
                                "$" + formatUsd(PortfolioStore.MAX_CASH),
                            ),
                        )
                    },
                    modifier = Modifier.fillMaxWidth(),
                )
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    PortfolioStore.STARTING_BALANCES.forEach { option ->
                        FilterChip(
                            selected = parsed == option,
                            onClick = { amount = option.toLong().toString() },
                            label = { Text("$" + formatUsd(option).removeSuffix(".00")) },
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { onCreate(name, parsed ?: 0.0) }, enabled = valid) {
                Text(stringResource(R.string.account_create))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) }
        },
    )
}

private fun Modifier.dashedBorder(color: Color): Modifier = drawBehind {
    val stroke = Stroke(
        width = 1.5.dp.toPx(),
        pathEffect = PathEffect.dashPathEffect(floatArrayOf(12f, 10f)),
    )
    drawRoundRect(color = color, cornerRadius = CornerRadius(16.dp.toPx()), style = stroke)
}
