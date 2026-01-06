package com.grippo.design.components.empty

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.grippo.design.core.AppTokens
import com.grippo.design.preview.AppPreview
import com.grippo.design.preview.PreviewContainer
import com.grippo.design.resources.provider.Res
import com.grippo.design.resources.provider.no_data_yet

@Composable
public fun EmptyState(modifier: Modifier = Modifier) {

    val text = AppTokens.strings.res(Res.string.no_data_yet)

    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(AppTokens.dp.contentPadding.content),
            modifier = Modifier.padding(horizontal = AppTokens.dp.screen.horizontalPadding)
        ) {
            Spacer(Modifier.weight(0.7f))

            EmptyIllustration(
                modifier = Modifier.size(AppTokens.dp.empty.image * 0.55f)
            )

            Text(
                text = text,
                style = AppTokens.typography.h6(),
                color = AppTokens.colors.text.secondary,
                textAlign = TextAlign.Center
            )

            Spacer(Modifier.weight(1f))
        }
    }
}

@Composable
private fun EmptyIllustration(modifier: Modifier = Modifier) {

    val borderColor = AppTokens.colors.border.default.copy(alpha = 0.4f)
    val backgroundColor = AppTokens.colors.background.card.copy(alpha = 0.4f)
    val highlight = AppTokens.colors.brand.color2
    val shape = RoundedCornerShape(AppTokens.dp.menu.radius)

    Box(
        modifier = modifier
            .background(
                color = backgroundColor,
                shape = shape
            )
            .border(
                width = 1.dp,
                color = borderColor,
                shape = shape
            )
            .padding(AppTokens.dp.contentPadding.subContent)
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val canvasSize = size
            val baselineY = canvasSize.height * 0.78f
            val barWidth = canvasSize.width * 0.11f
            val gap = barWidth * 0.6f
            val startX = canvasSize.width * 0.16f

            val barHeights = listOf(0.35f, 0.55f, 0.45f, 0.25f)

            barHeights.forEachIndexed { index, heightFraction ->
                val left = startX + index * (barWidth + gap)
                val top = baselineY - (canvasSize.height * heightFraction)
                drawRoundRect(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            highlight.copy(alpha = 0.85f),
                            highlight.copy(alpha = 0.55f)
                        )
                    ),
                    topLeft = Offset(x = left, y = top),
                    size = Size(width = barWidth, height = baselineY - top),
                    cornerRadius = CornerRadius(
                        x = barWidth * 0.4f,
                        y = barWidth * 0.4f
                    )
                )
            }
        }
    }
}

@AppPreview
@Composable
private fun EmptyStatePreview() {
    PreviewContainer {
        EmptyState(
            modifier = Modifier.fillMaxSize()
        )
    }
}
