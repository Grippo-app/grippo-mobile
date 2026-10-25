package com.grippo.design.components.modifiers

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Shape
import com.grippo.design.core.AppTokens
import com.grippo.design.preview.AppPreview
import com.grippo.design.preview.PreviewContainer

@Composable
public fun Modifier.illustrationBackground(shape: Shape): Modifier {
    val colors = AppTokens.colors.illustration
    return background(
        brush = Brush.linearGradient(listOf(colors.backgroundStart, colors.backgroundEnd)),
        shape = shape,
    )
}

@AppPreview
@Composable
private fun IllustrationBackgroundPreview() {
    PreviewContainer {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(AppTokens.dp.loader.container)
                .illustrationBackground(RoundedCornerShape(AppTokens.dp.brandHeader.radius)),
        )
        Box(
            modifier = Modifier
                .size(AppTokens.dp.loader.container)
                .illustrationBackground(CircleShape),
        )
    }
}
