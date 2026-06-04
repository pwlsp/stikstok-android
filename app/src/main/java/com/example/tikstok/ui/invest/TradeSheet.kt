package com.example.tikstok.ui.invest

import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.SouthEast
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.example.tikstok.R
import com.example.tikstok.data.portfolio.Holding
import com.example.tikstok.model.Asset
import kotlin.math.abs
import kotlin.math.roundToInt
import kotlinx.coroutines.launch

enum class TradeSide { BUY, SELL }

/**
 * Buy / sell panel from slide 6 of the deck, shown as a bottom sheet. The dollar amount is driven by
 * a slider and the $10/$25/$50/$100/MAX quick-picks; the bottom is a swipe-to-confirm gesture. Every
 * number is laid out on a single line (`softWrap = false`) so values never wrap.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TradeSheet(
    asset: Asset,
    price: Double,
    changePct: Float,
    initialSide: TradeSide,
    cash: Double,
    holding: Holding?,
    onBuy: (Double) -> Unit,
    onSell: (Double) -> Unit,
    onDismiss: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        var side by remember { mutableStateOf(initialSide) }
        var amount by remember { mutableStateOf(0.0) }
        // Reset the amount whenever the user flips between buy and sell.
        LaunchedEffect(side) { amount = 0.0 }

        val sideColor = if (side == TradeSide.BUY) UpColor else DownColor
        val holdingQty = holding?.quantity ?: 0.0
        val holdingValue = holdingQty * price
        val maxAmount = (if (side == TradeSide.BUY) cash else holdingValue).coerceAtLeast(0.0)
        val clamped = amount.coerceIn(0.0, maxAmount)
        val units = if (price > 0.0) clamped / price else 0.0

        Column(modifier = Modifier.padding(start = 20.dp, end = 20.dp, bottom = 24.dp)) {
            TradeHeader(asset, price, changePct, side, onSideChange = { side = it })

            Spacer(Modifier.height(20.dp))
            AmountDisplay(clamped)
            Spacer(Modifier.height(10.dp))
            UnitEquivalent(units, asset.ticker)

            Spacer(Modifier.height(20.dp))
            // YOU OWN sits well above the slider, with its own row, so the slider can't overlap it.
            YouOwnRow(holdingQty, holdingValue, asset.ticker)
            Spacer(Modifier.height(12.dp))
            Slider(
                value = clamped.toFloat(),
                onValueChange = { amount = it.toDouble() },
                valueRange = 0f..maxAmount.coerceAtLeast(0.01).toFloat(),
                enabled = maxAmount > 0.0,
                colors = SliderDefaults.colors(thumbColor = sideColor, activeTrackColor = sideColor),
            )
            Spacer(Modifier.height(12.dp))
            QuickAmounts(selected = clamped, maxAmount = maxAmount, onPick = { amount = it })

            Spacer(Modifier.height(20.dp))
            TradeSummary(side = side, price = price, amount = clamped, units = units, holding = holding)

            Spacer(Modifier.height(20.dp))
            val canConfirm = clamped > 0.0
            val confirmLabel = when {
                canConfirm -> stringResource(
                    if (side == TradeSide.BUY) R.string.trade_swipe_buy else R.string.trade_swipe_sell,
                    "$" + formatUsd(clamped),
                )
                maxAmount <= 0.0 -> stringResource(
                    if (side == TradeSide.BUY) R.string.trade_hint_no_cash else R.string.trade_hint_nothing_to_sell,
                )
                else -> stringResource(R.string.trade_hint_select_amount)
            }
            SwipeToConfirm(
                label = confirmLabel,
                color = sideColor,
                enabled = canConfirm,
                onConfirm = {
                    if (side == TradeSide.BUY) onBuy(clamped) else onSell(clamped)
                    amount = 0.0
                },
            )
            Spacer(Modifier.height(12.dp))
            Text(
                text = stringResource(R.string.trade_practice_mode),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

@Composable
private fun TradeHeader(
    asset: Asset,
    price: Double,
    changePct: Float,
    side: TradeSide,
    onSideChange: (TradeSide) -> Unit,
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        AssetAvatar(asset, size = 36.dp)
        Spacer(Modifier.width(10.dp))
        Column(Modifier.weight(1f)) {
            Text(
                text = asset.ticker,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                softWrap = false,
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "$" + formatPrice(price.toFloat()),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    softWrap = false,
                )
                Spacer(Modifier.width(6.dp))
                Text(
                    text = formatSignedPercent(changePct),
                    style = MaterialTheme.typography.bodySmall,
                    color = if (changePct >= 0f) UpColor else DownColor,
                    maxLines = 1,
                    softWrap = false,
                )
            }
        }
        Spacer(Modifier.width(8.dp))
        SideToggle(side, onSideChange)
    }
}

@Composable
private fun SideToggle(side: TradeSide, onSideChange: (TradeSide) -> Unit) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(3.dp),
        horizontalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        SideSegment(stringResource(R.string.invest_buy), side == TradeSide.BUY, UpColor) { onSideChange(TradeSide.BUY) }
        SideSegment(stringResource(R.string.invest_sell), side == TradeSide.SELL, DownColor) { onSideChange(TradeSide.SELL) }
    }
}

@Composable
private fun SideSegment(text: String, selected: Boolean, color: Color, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(if (selected) color else Color.Transparent)
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 6.dp),
    ) {
        Text(
            text = text.uppercase(),
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.SemiBold,
            color = if (selected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            softWrap = false,
        )
    }
}

@Composable
private fun AmountDisplay(amount: Double) {
    val parts = formatUsd(amount).split(".")
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.Bottom,
    ) {
        Text(
            text = "$",
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            softWrap = false,
        )
        Spacer(Modifier.width(2.dp))
        Text(
            text = parts[0],
            style = MaterialTheme.typography.displaySmall,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            softWrap = false,
        )
        Text(
            text = "." + parts.getOrElse(1) { "00" },
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            softWrap = false,
        )
    }
}

@Composable
private fun UnitEquivalent(units: Double, ticker: String) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
        Row(
            modifier = Modifier
                .clip(RoundedCornerShape(50))
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .padding(horizontal = 12.dp, vertical = 5.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Icon(
                imageVector = Icons.Filled.SouthEast,
                contentDescription = null,
                modifier = Modifier.size(14.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = "${formatUnits(units)} $ticker",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                softWrap = false,
            )
        }
    }
}

@Composable
private fun YouOwnRow(quantity: Double, value: Double, ticker: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            text = stringResource(R.string.trade_you_own).uppercase(),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            softWrap = false,
        )
        Text(
            text = "${formatUnits(quantity)} $ticker  ·  $" + formatUsd(value),
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Medium,
            maxLines = 1,
            softWrap = false,
        )
    }
}

@Composable
private fun QuickAmounts(selected: Double, maxAmount: Double, onPick: (Double) -> Unit) {
    val presets = listOf(10.0, 25.0, 50.0, 100.0)
    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        presets.forEach { value ->
            val capped = value.coerceAtMost(maxAmount)
            QuickChip(
                text = "$" + value.roundToInt(),
                selected = selected > 0.0 && abs(selected - capped) < 0.005,
                enabled = maxAmount > 0.0,
                onClick = { onPick(capped) },
                modifier = Modifier.weight(1f),
            )
        }
        QuickChip(
            text = stringResource(R.string.trade_max),
            selected = maxAmount > 0.0 && abs(selected - maxAmount) < 0.005,
            enabled = maxAmount > 0.0,
            onClick = { onPick(maxAmount) },
            modifier = Modifier.weight(1f),
        )
    }
}

/** Compact pill that always fits its short label — narrower than a default FilterChip. */
@Composable
private fun QuickChip(
    text: String,
    selected: Boolean,
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val bg = if (selected) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.surfaceVariant
    val fg = if (selected) MaterialTheme.colorScheme.onSecondaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
    Box(
        modifier = modifier
            .height(40.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(if (enabled) bg else bg.copy(alpha = 0.4f))
            .clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Medium,
            color = fg,
            maxLines = 1,
            softWrap = false,
        )
    }
}

@Composable
private fun TradeSummary(side: TradeSide, price: Double, amount: Double, units: Double, holding: Holding?) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        SummaryRow(stringResource(R.string.trade_price), "$" + formatPrice(price.toFloat()))
        if (side == TradeSide.BUY) {
            SummaryRow(stringResource(R.string.trade_units), formatUnits(units))
            SummaryRow(stringResource(R.string.trade_total_cost), "$" + formatUsd(amount))
        } else {
            val profit = (price - (holding?.avgCost ?: price)) * units
            SummaryRow(
                label = stringResource(R.string.trade_potential_profit),
                value = formatSignedUsd(profit),
                valueColor = if (profit >= 0.0) UpColor else DownColor,
            )
            SummaryRow(stringResource(R.string.trade_you_receive), "$" + formatUsd(amount))
        }
    }
}

@Composable
private fun SummaryRow(label: String, value: String, valueColor: Color? = null) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            softWrap = false,
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
            color = valueColor ?: MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            softWrap = false,
        )
    }
}

/** Drag the knob to the far end to confirm. Snaps back if released before ~90% of the track. */
@Composable
private fun SwipeToConfirm(label: String, color: Color, enabled: Boolean, onConfirm: () -> Unit) {
    val density = LocalDensity.current
    val scope = rememberCoroutineScope()
    val trackHeight = 56.dp
    val knob = 48.dp
    val pad = 4.dp
    val offsetX = remember { Animatable(0f) }

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxWidth()
            .height(trackHeight)
            .clip(RoundedCornerShape(50))
            .background(if (enabled) color else color.copy(alpha = 0.4f)),
        contentAlignment = Alignment.Center,
    ) {
        val maxOffsetPx = with(density) { (maxWidth - knob - pad * 2).toPx() }.coerceAtLeast(0f)

        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(label, color = Color.White, fontWeight = FontWeight.SemiBold, maxLines = 1, softWrap = false)
            if (enabled) {
                Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
            }
        }

        if (enabled) {
            Box(
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .padding(pad)
                    .offset { IntOffset(offsetX.value.roundToInt(), 0) }
                    .size(knob)
                    .clip(CircleShape)
                    .background(Color.White)
                    .pointerInput(enabled, maxOffsetPx) {
                        if (!enabled) return@pointerInput
                        detectHorizontalDragGestures(
                            onDragEnd = {
                                if (offsetX.value >= maxOffsetPx * 0.9f) {
                                    onConfirm()
                                    scope.launch { offsetX.snapTo(0f) }
                                } else {
                                    scope.launch { offsetX.animateTo(0f) }
                                }
                            },
                        ) { change, dragAmount ->
                            change.consume()
                            scope.launch {
                                offsetX.snapTo((offsetX.value + dragAmount).coerceIn(0f, maxOffsetPx))
                            }
                        }
                    },
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, tint = color, modifier = Modifier.size(22.dp))
            }
        }
    }
}
