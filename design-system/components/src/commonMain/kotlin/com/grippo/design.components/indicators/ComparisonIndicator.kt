package com.grippo.design.components.indicators

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.clipRect
import com.grippo.design.core.AppTokens
import com.grippo.design.preview.AppPreview
import com.grippo.design.preview.PreviewContainer
import com.grippo.design.resources.provider.AppColor

@Composable
public fun ComparisonIndicator(
    modifier: Modifier = Modifier,
    value: () -> Float,
    reference: () -> Float,
    colors: AppColor.Charts.IndicatorColors.IndicatorColors,
) {
    val barHeight = AppTokens.dp.metrics.charts.comparison.height
    val markerWidth = AppTokens.dp.metrics.charts.comparison.marker.width
    val markerHeight = AppTokens.dp.metrics.charts.comparison.marker.height
    val comparisonColors = AppTokens.colors.charts.comparison
    val trackColor = comparisonColors.track
    val sharedColor = comparisonColors.shared
    val markerColor = comparisonColors.marker
    val gainColor = colors.dominant
    val lossColor = colors.dominant.copy(alpha = colors.dominant.alpha * comparisonColors.lossAlpha)

    Spacer(
        modifier = modifier
            .fillMaxWidth()
            .height(maxOf(barHeight, markerHeight))
            .drawBehind {
                val width = size.width
                if (width <= 0f) return@drawBehind

                val barPx = barHeight.toPx().coerceAtMost(size.height)
                val barTopLeft = Offset(0f, (size.height - barPx) / 2f)
                val barCorner = CornerRadius(barPx / 2f, barPx / 2f)
                val referenceX = width * sanitizedFraction(reference())
                val valueX = width * sanitizedFraction(value())
                val isGain = valueX > referenceX
                val valueSize = Size(maxOf(valueX, barPx).coerceAtMost(width), barPx)

                drawRoundRect(
                    color = trackColor,
                    topLeft = barTopLeft,
                    size = Size(width, barPx),
                    cornerRadius = barCorner,
                )

                if (valueX < referenceX) {
                    clipRect(right = referenceX) {
                        drawRoundRect(
                            color = lossColor,
                            topLeft = barTopLeft,
                            size = Size(width, barPx),
                            cornerRadius = barCorner,
                        )
                    }
                }

                if (valueX > 0f) {
                    clipRect(right = if (isGain) referenceX else width) {
                        drawRoundRect(
                            color = sharedColor,
                            topLeft = barTopLeft,
                            size = valueSize,
                            cornerRadius = barCorner,
                        )
                    }
                }

                if (isGain) {
                    clipRect(left = referenceX) {
                        drawRoundRect(
                            color = gainColor,
                            topLeft = barTopLeft,
                            size = valueSize,
                            cornerRadius = barCorner,
                        )
                    }
                }

                val markerWidthPx = markerWidth.toPx().coerceAtMost(width)
                val markerHeightPx = markerHeight.toPx().coerceAtMost(size.height)
                val markerLeft = (referenceX - markerWidthPx / 2f).coerceIn(0f, width - markerWidthPx)
                drawRoundRect(
                    color = markerColor,
                    topLeft = Offset(markerLeft, (size.height - markerHeightPx) / 2f),
                    size = Size(markerWidthPx, markerHeightPx),
                    cornerRadius = CornerRadius(markerWidthPx / 2f, markerWidthPx / 2f),
                )
            },
    )
}

private fun sanitizedFraction(fraction: Float): Float = if (fraction.isNaN()) 0f else fraction.coerceIn(0f, 1f)

@AppPreview
@Composable
private fun ComparisonIndicatorPreview() {
    PreviewContainer {
        ComparisonIndicator(
            value = { 0.82f },
            reference = { 0.6f },
            colors = AppTokens.colors.charts.indicator.success,
        )

        ComparisonIndicator(
            value = { 0.45f },
            reference = { 0.7f },
            colors = AppTokens.colors.charts.indicator.decline,
        )

        ComparisonIndicator(
            value = { 0.54f },
            reference = { 0.54f },
            colors = AppTokens.colors.charts.indicator.muted,
        )

        ComparisonIndicator(
            value = { 0.6f },
            reference = { 1f },
            colors = AppTokens.colors.charts.indicator.muted,
        )

        ComparisonIndicator(
            value = { 1f },
            reference = { 0f },
            colors = AppTokens.colors.charts.indicator.success,
        )

        ComparisonIndicator(
            value = { 0.01f },
            reference = { 0.3f },
            colors = AppTokens.colors.charts.indicator.decline,
        )

        ComparisonIndicator(
            value = { 0f },
            reference = { 0.4f },
            colors = AppTokens.colors.charts.indicator.decline,
        )
    }
}
