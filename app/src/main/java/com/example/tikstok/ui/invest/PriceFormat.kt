package com.example.tikstok.ui.invest

import androidx.compose.ui.graphics.Color
import com.example.tikstok.model.Timeframe
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

val FlatColor = Color(0xFFB8B8B8)

fun isFlatUsd(value: Double): Boolean = kotlin.math.abs(value) < 0.005

fun isFlatPercent(percent: Float): Boolean = kotlin.math.abs(percent) < 0.005f

fun plColor(value: Double): Color = when {
    isFlatUsd(value) -> FlatColor
    value > 0 -> UpColor
    else -> DownColor
}

fun plColorPct(percent: Float): Color = when {
    isFlatPercent(percent) -> FlatColor
    percent > 0 -> UpColor
    else -> DownColor
}

fun formatPrice(value: Float): String = when {
    value >= 1_000f -> "%,.2f".format(value)
    value >= 1f -> "%.2f".format(value)
    value >= 0.01f -> "%.4f".format(value)
    else -> "%.6f".format(value)
}

fun formatSignedPercent(percent: Float): String =
    if (isFlatPercent(percent)) "0.00%" else "%+.2f%%".format(percent)

fun formatUsd(value: Double): String = "%,.2f".format(Locale.US, value)

fun formatSignedUsd(value: Double): String {
    if (isFlatUsd(value)) return "$%,.2f".format(Locale.US, 0.0)
    val sign = if (value >= 0) "+" else "-"
    return "$sign$%,.2f".format(Locale.US, kotlin.math.abs(value))
}

fun formatDateTime(timestampMillis: Long): String =
    SimpleDateFormat("d MMM yyyy, HH:mm", Locale.getDefault()).format(Date(timestampMillis))

fun formatMonthYear(timestampMillis: Long): String =
    SimpleDateFormat("MMM yyyy", Locale.getDefault()).format(Date(timestampMillis))

fun formatUnits(units: Double): String {
    if (units == 0.0) return "0"
    val s = "%.6f".format(Locale.US, units)
    return s.trimEnd('0').trimEnd('.')
}

private fun timePattern(timeframe: Timeframe): String = when (timeframe) {
    Timeframe.M30, Timeframe.H1, Timeframe.H3, Timeframe.D1 -> "HH:mm"
    Timeframe.W1 -> "EEE HH:mm"
    Timeframe.MO1, Timeframe.Y1 -> "dd MMM"
    Timeframe.Y5 -> "MMM yyyy"
}

fun timeFormatter(timeframe: Timeframe): SimpleDateFormat =
    SimpleDateFormat(timePattern(timeframe), Locale.getDefault())

fun formatTimestamp(timestamp: Long, timeframe: Timeframe): String =
    timeFormatter(timeframe).format(Date(timestamp * 1000L))
