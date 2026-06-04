package com.example.tikstok.ui.invest

import android.graphics.Paint
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.tikstok.model.Candle
import com.example.tikstok.model.Timeframe
import java.util.Date
import kotlin.math.roundToInt

/**
 * A simple close-price line covering the whole series at once — the non-scrolling "overview"
 * counterpart to [CandlestickChart]. Press-and-drag scrubs a crosshair and reports the nearest
 * index via [onSelect], so the same OHLC readout works in both modes.
 */
@Composable
fun LineChart(
    candles: List<Candle>,
    selectedIndex: Int?,
    onSelect: (Int) -> Unit,
    timeframe: Timeframe,
    modifier: Modifier = Modifier,
) {
    val lineColor = MaterialTheme.colorScheme.primary
    val gridColor = MaterialTheme.colorScheme.outlineVariant
    val labelColor = MaterialTheme.colorScheme.onSurfaceVariant
    val crosshairColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
    val timeFmt = remember(timeframe) { timeFormatter(timeframe) }
    val density = LocalDensity.current

    val plotLeft = with(density) { 40.dp.toPx() }
    val plotRight = with(density) { 8.dp.toPx() }
    val topPad = with(density) { 8.dp.toPx() }
    val bottomPad = with(density) { 22.dp.toPx() }
    val labelX = with(density) { 2.dp.toPx() }
    val strokePx = with(density) { 2.dp.toPx() }

    Canvas(
        modifier = modifier.pointerInput(candles) {
            fun indexAt(x: Float): Int {
                val plotW = size.width - plotLeft - plotRight
                if (plotW <= 0f || candles.size < 2) return 0
                return ((x - plotLeft) / plotW * (candles.size - 1)).roundToInt()
                    .coerceIn(0, candles.size - 1)
            }
            awaitEachGesture {
                val down = awaitFirstDown()
                onSelect(indexAt(down.position.x))
                down.consume()
                while (true) {
                    val event = awaitPointerEvent()
                    val active = event.changes.firstOrNull { it.pressed } ?: break
                    onSelect(indexAt(active.position.x))
                    active.consume()
                }
            }
        },
    ) {
        if (candles.size < 2) return@Canvas
        val plotW = size.width - plotLeft - plotRight
        val plotH = size.height - topPad - bottomPad
        val n = candles.size

        var lo = candles[0].close
        var hi = candles[0].close
        for (candle in candles) {
            if (candle.close < lo) lo = candle.close
            if (candle.close > hi) hi = candle.close
        }
        val span = (hi - lo).takeIf { it > 0f } ?: 1f
        fun xAt(i: Int) = plotLeft + plotW * i / (n - 1).toFloat()
        fun yAt(price: Float) = topPad + plotH - ((price - lo) / span) * plotH

        val labelPaint = Paint().apply {
            color = labelColor.toArgb()
            textSize = 11.sp.toPx()
            isAntiAlias = true
        }
        val leftPaint = Paint(labelPaint).apply { textAlign = Paint.Align.LEFT }
        val centerPaint = Paint(labelPaint).apply { textAlign = Paint.Align.CENTER }
        val rightPaint = Paint(labelPaint).apply { textAlign = Paint.Align.RIGHT }

        listOf(hi, (hi + lo) / 2f, lo).forEach { price ->
            val y = yAt(price)
            drawLine(gridColor, Offset(plotLeft, y), Offset(plotLeft + plotW, y), 1f)
        }

        val baseY = size.height - 6f
        drawContext.canvas.nativeCanvas.drawText(
            timeFmt.format(Date(candles[0].timestamp * 1000L)), plotLeft, baseY, leftPaint,
        )
        drawContext.canvas.nativeCanvas.drawText(
            timeFmt.format(Date(candles[n / 2].timestamp * 1000L)),
            plotLeft + plotW / 2f, baseY, centerPaint,
        )
        drawContext.canvas.nativeCanvas.drawText(
            timeFmt.format(Date(candles[n - 1].timestamp * 1000L)),
            plotLeft + plotW, baseY, rightPaint,
        )

        val path = Path().apply {
            moveTo(xAt(0), yAt(candles[0].close))
            for (i in 1 until n) lineTo(xAt(i), yAt(candles[i].close))
        }
        drawPath(
            path = path,
            color = lineColor,
            style = Stroke(width = strokePx, cap = StrokeCap.Round, join = StrokeJoin.Round),
        )

        val selected = selectedIndex?.coerceIn(0, n - 1)
        if (selected != null) {
            val sx = xAt(selected)
            drawLine(crosshairColor, Offset(sx, topPad), Offset(sx, topPad + plotH), 2f)
            drawCircle(lineColor, radius = strokePx * 1.6f, center = Offset(sx, yAt(candles[selected].close)))
        }

        // Draw price labels last so they are above the graph
        listOf(hi, (hi + lo) / 2f, lo).forEach { price ->
            val y = yAt(price)
            drawContext.canvas.nativeCanvas.drawText(
                formatPrice(price), labelX, y + labelPaint.textSize / 3f, leftPaint,
            )
        }
    }
}
