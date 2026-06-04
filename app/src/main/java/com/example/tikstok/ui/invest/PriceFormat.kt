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

/** Time/date pattern appropriate for a timeframe's granularity, used for chart axis + readout. */
private fun timePattern(timeframe: Timeframe): String = when (timeframe) {
    Timeframe.M30, Timeframe.H1, Timeframe.H4, Timeframe.D1 -> "HH:mm"
    Timeframe.W1 -> "EEE HH:mm"
    Timeframe.MO1, Timeframe.Y1 -> "dd MMM"
    Timeframe.Y5 -> "MMM yyyy"
}

fun timeFormatter(timeframe: Timeframe): SimpleDateFormat =
    SimpleDateFormat(timePattern(timeframe), Locale.getDefault())

/** Formats an epoch-seconds timestamp for the given timeframe. */
fun formatTimestamp(timestamp: Long, timeframe: Timeframe): String =
    timeFormatter(timeframe).format(Date(timestamp * 1000L))
