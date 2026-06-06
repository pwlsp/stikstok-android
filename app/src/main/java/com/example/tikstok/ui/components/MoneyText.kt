package com.example.tikstok.ui.components

import androidx.compose.foundation.text.TextAutoSize
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.isUnspecified
import androidx.compose.ui.unit.sp

/**
 * A single-line [Text] for money values (prices, balances, "you own" amounts) that keeps its
 * design font size when the text fits and shrinks to fit on one line when it doesn't, instead of
 * clipping or ellipsizing. Use it anywhere a value can grow large enough to overflow its slot.
 *
 * The style's font size is the ceiling — the text never grows past it, only down to [minFontSize].
 */
@Composable
fun MoneyText(
    text: String,
    modifier: Modifier = Modifier,
    style: TextStyle = LocalTextStyle.current,
    color: Color = Color.Unspecified,
    fontWeight: FontWeight? = null,
    textAlign: TextAlign? = null,
    minFontSize: TextUnit = 8.sp,
) {
    // Fall back to a sensible ceiling if the style has no explicit size (shouldn't happen for the
    // typography styles we pass, but keeps StepBased from failing on an unspecified max).
    val maxFontSize = if (style.fontSize.isUnspecified) 48.sp else style.fontSize
    Text(
        text = text,
        modifier = modifier,
        style = style,
        color = color,
        fontWeight = fontWeight,
        textAlign = textAlign,
        maxLines = 1,
        softWrap = false,
        autoSize = TextAutoSize.StepBased(
            minFontSize = minFontSize,
            maxFontSize = maxFontSize,
            stepSize = 0.5.sp,
        ),
    )
}
