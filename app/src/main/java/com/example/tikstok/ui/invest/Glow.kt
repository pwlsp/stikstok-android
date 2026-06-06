package com.example.tikstok.ui.invest

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.BlurredEdgeTreatment
import androidx.compose.ui.draw.blur
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@Composable
fun Glowing(
    color: Color,
    shape: Shape,
    modifier: Modifier = Modifier,
    radius: Dp = 20.dp,
    alpha: Float = 0.55f,
    content: @Composable () -> Unit,
) {
    Box(modifier) {
        Box(
            Modifier
                .matchParentSize()
                .padding(horizontal = 8.dp, vertical = 5.dp)
                .blur(radius, BlurredEdgeTreatment.Unbounded)
                .background(color.copy(alpha = alpha), shape)
        )
        content()
    }
}
