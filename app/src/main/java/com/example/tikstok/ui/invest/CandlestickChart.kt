package com.example.tikstok.ui.invest

import android.graphics.Paint
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.tikstok.model.Candle
import com.example.tikstok.model.Timeframe
import java.util.Date

internal val UpColor = Color(0xFF22C55E)
internal val DownColor = Color(0xFFE76B4E)

@Composable
fun CandlestickChart(
    candles: List<Candle>,
    selectedIndex: Int?,
    onSelect: (Int) -> Unit,
    timeframe: Timeframe,
    modifier: Modifier = Modifier,
) {
    val gridColor = MaterialTheme.colorScheme.outline
    val labelColor = MaterialTheme.colorScheme.onSurface
    val labelBgColor = MaterialTheme.colorScheme.surfaceVariant
    val crosshairColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
    val trackColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)
    val thumbColor = Color(0xFF5A6470)
    val timeFmt = remember(timeframe) { timeFormatter(timeframe) }
    val density = LocalDensity.current

    BoxWithConstraints(modifier) {
        val viewportPx = constraints.maxWidth.toFloat()
        val plotLeft = with(density) { 40.dp.toPx() }
        val plotRight = with(density) { 8.dp.toPx() }
        val topPad = with(density) { 8.dp.toPx() }
        val bottomPad = with(density) { 30.dp.toPx() }
        val labelX = with(density) { 2.dp.toPx() }
        val minSlot = if (timeframe.isScrollable) with(density) { 8.dp.toPx() } else 0f

        val plotWidth = (viewportPx - plotLeft - plotRight).coerceAtLeast(1f)
        val count = candles.size.coerceAtLeast(1)
        val slot = maxOf(minSlot, plotWidth / count)
        val contentWidth = slot * count
        val maxOffset = (contentWidth - plotWidth).coerceAtLeast(0f)

        var offset by remember(candles, viewportPx) { mutableStateOf(maxOffset) }

        fun indexAtScreenX(x: Float): Int =
            ((x - plotLeft + offset) / slot).toInt().coerceIn(0, candles.size - 1)

        Column(Modifier.fillMaxSize()) {
            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .pointerInput(candles, slot) {
                        awaitEachGesture {
                            val down = awaitFirstDown()
                            onSelect(indexAtScreenX(down.position.x))
                            down.consume()
                            while (true) {
                                val event = awaitPointerEvent()
                                val active = event.changes.firstOrNull { it.pressed } ?: break
                                onSelect(indexAtScreenX(active.position.x))
                                active.consume()
                            }
                        }
                    },
            ) {
                if (candles.isEmpty()) return@Canvas
                val plotH = size.height - topPad - bottomPad

                val firstVisible = indexAtScreenX(plotLeft)
                val lastVisible = indexAtScreenX(plotLeft + plotWidth)
                val window = candles.subList(firstVisible, lastVisible + 1)
                val min = window.minOf { it.low }
                val max = window.maxOf { it.high }
                val span = (max - min).takeIf { it > 0f } ?: 1f
                fun yAt(price: Float) = topPad + plotH - ((price - min) / span) * plotH
                fun screenXAt(i: Int) = plotLeft + (slot * (i + 0.5f) - offset)

                val labelPaint = Paint().apply {
                    color = labelColor.toArgb()
                    textSize = 11.sp.toPx()
                    isAntiAlias = true
                }
                val leftPaint = Paint(labelPaint).apply { textAlign = Paint.Align.LEFT }
                val centerPaint = Paint(labelPaint).apply { textAlign = Paint.Align.CENTER }
                val rightPaint = Paint(labelPaint).apply { textAlign = Paint.Align.RIGHT }

                listOf(max, (max + min) / 2f, min).forEach { price ->
                    val y = yAt(price)
                    drawLine(gridColor, Offset(plotLeft, y), Offset(plotLeft + plotWidth, y), 1f)
                }

                val baseY = size.height - 6f
                drawContext.canvas.nativeCanvas.drawText(
                    timeFmt.format(Date(candles[indexAtScreenX(plotLeft)].timestamp * 1000L)),
                    plotLeft, baseY, leftPaint,
                )
                drawContext.canvas.nativeCanvas.drawText(
                    timeFmt.format(Date(candles[indexAtScreenX(plotLeft + plotWidth / 2f)].timestamp * 1000L)),
                    plotLeft + plotWidth / 2f, baseY, centerPaint,
                )
                drawContext.canvas.nativeCanvas.drawText(
                    timeFmt.format(Date(candles[indexAtScreenX(plotLeft + plotWidth)].timestamp * 1000L)),
                    plotLeft + plotWidth, baseY, rightPaint,
                )

                val candleW = (slot * 0.7f).coerceAtLeast(2f)
                val leftBound = plotLeft - slot
                val rightBound = plotLeft + plotWidth + slot

                clipRect(left = plotLeft, top = topPad, right = plotLeft + plotWidth, bottom = topPad + plotH) {
                    val selected = selectedIndex?.coerceIn(0, candles.size - 1)
                    if (selected != null) {
                        val sx = screenXAt(selected)
                        if (sx in leftBound..rightBound) {
                            drawLine(crosshairColor, Offset(sx, topPad), Offset(sx, topPad + plotH), 2f)
                        }
                    }

                    candles.forEachIndexed { i, candle ->
                        val cx = screenXAt(i)
                        if (cx < leftBound || cx > rightBound) return@forEachIndexed
                        val color = if (candle.close >= candle.open) UpColor else DownColor
                        drawLine(color, Offset(cx, yAt(candle.high)), Offset(cx, yAt(candle.low)), 2f)
                        val bodyTop = yAt(maxOf(candle.open, candle.close))
                        val bodyBot = yAt(minOf(candle.open, candle.close))
                        drawRect(
                            color = color,
                            topLeft = Offset(cx - candleW / 2f, bodyTop),
                            size = Size(candleW, (bodyBot - bodyTop).coerceAtLeast(1.5f)),
                        )
                    }

                    if (selected != null) {
                        val sx = screenXAt(selected)
                        if (sx in leftBound..rightBound) {
                            val candle = candles[selected]
                            val color = if (candle.close >= candle.open) UpColor else DownColor
                            val bodyTop = yAt(maxOf(candle.open, candle.close))
                            val bodyBot = yAt(minOf(candle.open, candle.close))
                            drawRect(
                                color = color,
                                topLeft = Offset(sx - candleW / 2f - 3f, bodyTop - 3f),
                                size = Size(candleW + 6f, (bodyBot - bodyTop).coerceAtLeast(1.5f) + 6f),
                                style = Stroke(width = 2f),
                            )
                        }
                    }
                }

                listOf(max, (max + min) / 2f, min).forEach { price ->
                    val text = formatPrice(price)
                    val baselineY = yAt(price) + labelPaint.textSize / 3f
                    val padX = 4.dp.toPx()
                    val padY = 2.dp.toPx()
                    val fm = leftPaint.fontMetrics
                    val textW = leftPaint.measureText(text)
                    val left = (labelX - padX).coerceAtLeast(0f)
                    drawRoundRect(
                        color = labelBgColor,
                        topLeft = Offset(left, baselineY + fm.ascent - padY),
                        size = Size(textW + (labelX - left) + padX, (fm.descent - fm.ascent) + padY * 2),
                        cornerRadius = CornerRadius(4.dp.toPx(), 4.dp.toPx()),
                    )
                    drawContext.canvas.nativeCanvas.drawText(text, labelX, baselineY, leftPaint)
                }
            }

            Spacer(Modifier.height(6.dp))
            if (maxOffset > 0f) {
                ChartScrollbar(
                    offset = offset,
                    maxOffset = maxOffset,
                    thumbFraction = (plotWidth / contentWidth).coerceIn(0.08f, 1f),
                    trackColor = trackColor,
                    thumbColor = thumbColor,
                    onOffsetChange = { offset = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 6.dp),
                )
            } else {
                Spacer(Modifier.height(8.dp))
            }
        }
    }
}

@Composable
private fun ChartScrollbar(
    offset: Float,
    maxOffset: Float,
    thumbFraction: Float,
    trackColor: Color,
    thumbColor: Color,
    onOffsetChange: (Float) -> Unit,
    modifier: Modifier = Modifier,
) {
    Canvas(
        modifier = modifier
            .height(8.dp)
            .pointerInput(maxOffset, thumbFraction) {
                fun moveTo(x: Float) {
                    val thumbW = size.width * thumbFraction
                    val travel = size.width - thumbW
                    if (travel > 0f) {
                        val desired = (x - thumbW / 2f).coerceIn(0f, travel)
                        onOffsetChange(desired / travel * maxOffset)
                    }
                }
                awaitEachGesture {
                    val down = awaitFirstDown()
                    moveTo(down.position.x)
                    down.consume()
                    while (true) {
                        val event = awaitPointerEvent()
                        val active = event.changes.firstOrNull { it.pressed } ?: break
                        moveTo(active.position.x)
                        active.consume()
                    }
                }
            },
    ) {
        val radius = CornerRadius(size.height / 2f, size.height / 2f)
        drawRoundRect(color = trackColor, cornerRadius = radius)
        val thumbW = size.width * thumbFraction
        val travel = size.width - thumbW
        val thumbX = if (maxOffset > 0f) offset / maxOffset * travel else 0f
        drawRoundRect(
            color = thumbColor,
            topLeft = Offset(thumbX, 0f),
            size = Size(thumbW, size.height),
            cornerRadius = radius,
        )
    }
}
