package com.example.tikstok.ui.invest

import com.example.tikstok.model.Timeframe
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

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

fun formatSignedPercent(percent: Float): String = "%+.2f%%".format(percent)

/** A USD amount, always two decimals with thousands separators, e.g. "1,001.92". */
fun formatUsd(value: Double): String = "%,.2f".format(Locale.US, value)

/** A signed USD amount for P/L readouts, e.g. "+$7.42" / "-$0.43". */
fun formatSignedUsd(value: Double): String {
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
