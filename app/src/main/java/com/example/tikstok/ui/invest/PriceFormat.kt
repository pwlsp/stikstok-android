package com.example.tikstok.ui.invest

import androidx.compose.ui.graphics.Color
import com.example.tikstok.model.Timeframe
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** Light neutral for a flat (≈0) P/L, so a zero change reads as neither a gain nor a loss. */
val FlatColor = Color(0xFFB8B8B8)

/** True when a USD amount rounds to $0.00 at the two decimals we display. */
fun isFlatUsd(value: Double): Boolean = kotlin.math.abs(value) < 0.005

/** True when a percent rounds to 0.00% at the two decimals we display. */
fun isFlatPercent(percent: Float): Boolean = kotlin.math.abs(percent) < 0.005f

/** Gain → green, loss → red, flat (≈0) → neutral. Driven by a USD P/L amount. */
fun plColor(value: Double): Color = when {
    isFlatUsd(value) -> FlatColor
    value > 0 -> UpColor
    else -> DownColor
}

/** Gain → green, loss → red, flat (≈0) → neutral. Driven by a percent change. */
fun plColorPct(percent: Float): Color = when {
    isFlatPercent(percent) -> FlatColor
    percent > 0 -> UpColor
    else -> DownColor
}

/**
 * Formats a price with a sensible number of decimals for its magnitude — BTC (~$100k) needs none
 * past cents, while DOGE (~$0.10) needs several.
 */
fun formatPrice(value: Float): String = when {
    value >= 1_000f -> "%,.2f".format(value)
    value >= 1f -> "%.2f".format(value)
    value >= 0.01f -> "%.4f".format(value)
    else -> "%.6f".format(value)
}

fun formatSignedPercent(percent: Float): String =
    if (isFlatPercent(percent)) "0.00%" else "%+.2f%%".format(percent)

/** A USD amount, always two decimals with thousands separators, e.g. "1,001.92". */
fun formatUsd(value: Double): String = "%,.2f".format(Locale.US, value)

/** A signed USD amount for P/L readouts, e.g. "+$7.42" / "-$0.43". */
fun formatSignedUsd(value: Double): String {
    if (isFlatUsd(value)) return "$%,.2f".format(Locale.US, 0.0)
    val sign = if (value >= 0) "+" else "-"
    return "$sign$%,.2f".format(Locale.US, kotlin.math.abs(value))
}

/** A full date + time for the transaction history, e.g. "5 Jun 2026, 14:03". */
fun formatDateTime(timestampMillis: Long): String =
    SimpleDateFormat("d MMM yyyy, HH:mm", Locale.getDefault()).format(Date(timestampMillis))

/** A month + year for the account "joined" line, e.g. "Jun 2026". */
fun formatMonthYear(timestampMillis: Long): String =
    SimpleDateFormat("MMM yyyy", Locale.getDefault()).format(Date(timestampMillis))

/** An asset-unit quantity with up to 6 decimals, trailing zeros trimmed, e.g. "0.185336". */
fun formatUnits(units: Double): String {
    if (units == 0.0) return "0"
    val s = "%.6f".format(Locale.US, units)
    return s.trimEnd('0').trimEnd('.')
}

/** Time/date pattern appropriate for a timeframe's granularity, used for chart axis + readout. */
private fun timePattern(timeframe: Timeframe): String = when (timeframe) {
    Timeframe.M30, Timeframe.H1, Timeframe.H3, Timeframe.D1 -> "HH:mm"
    Timeframe.W1 -> "EEE HH:mm"
    Timeframe.MO1, Timeframe.Y1 -> "dd MMM"
    Timeframe.Y5 -> "MMM yyyy"
}

fun timeFormatter(timeframe: Timeframe): SimpleDateFormat =
    SimpleDateFormat(timePattern(timeframe), Locale.getDefault())

/** Formats an epoch-seconds timestamp for the given timeframe. */
fun formatTimestamp(timestamp: Long, timeframe: Timeframe): String =
    timeFormatter(timeframe).format(Date(timestamp * 1000L))
