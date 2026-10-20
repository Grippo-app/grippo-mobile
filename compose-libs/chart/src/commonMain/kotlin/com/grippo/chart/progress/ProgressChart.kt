package com.grippo.chart.progress

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import kotlin.math.ln
import kotlin.math.min
import kotlin.math.pow

@Composable
public fun ProgressChart(
    modifier: Modifier = Modifier,
    data: ProgressData,
    style: ProgressStyle,
) {
    val maximum = when (val domain = style.domain) {
        is ProgressStyle.Domain.Normalized -> 1f
        is ProgressStyle.Domain.Absolute -> domain.maxValue?.takeIf { it.isFinite() && it > 0f }
            ?: data.items.maxOfOrNull { it.value.takeIf(Float::isFinite) ?: 0f }?.coerceAtLeast(1f)
            ?: 1f
    }

    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(style.layout.spacing.coerceAtLeast(0.dp)),
    ) {
        data.items.forEach { entry ->
            ProgressRow(entry = entry, data = data, style = style, maximum = maximum)
        }
    }
}

@Composable
private fun ProgressRow(
    entry: ProgressChartData,
    data: ProgressData,
    style: ProgressStyle,
    maximum: Float,
) {
    val density = LocalDensity.current
    val measurer = rememberTextMeasurer()
    val fraction = scaledFraction(value = entry.value, maximum = maximum, progression = style.progression)
    val normalized = style.domain is ProgressStyle.Domain.Normalized
    val valueText = if (!entry.value.isFinite() && style.values !is ProgressStyle.Values.None) {
        "—"
    } else when (val values = style.values) {
        is ProgressStyle.Values.None -> null
        is ProgressStyle.Values.Inside -> values.formatter(
            if (normalized && values.preferNormalizedLabels) entry.value.coerceIn(0f, 1f) else entry.value,
            data,
        )
        is ProgressStyle.Values.Outside -> values.formatter(
            if (normalized && values.preferNormalizedLabels) entry.value.coerceIn(0f, 1f) else entry.value,
            data,
        )
    }
    val valueStyle = when (val values = style.values) {
        is ProgressStyle.Values.None -> style.labels.textStyle
        is ProgressStyle.Values.Inside -> values.textStyle
        is ProgressStyle.Values.Outside -> values.textStyle
    }
    val valueLayout = remember(measurer, valueText, valueStyle) {
        valueText?.let { measurer.measure(text = AnnotatedString(it), style = valueStyle) }
    }

    BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
        val inside = style.values as? ProgressStyle.Values.Inside
        val innerPadding = with(density) { inside?.minInnerPadding?.toPx() ?: 0f }
        val barWidth = with(density) { maxWidth.toPx() } * fraction
        val showInside = inside != null && valueLayout != null &&
            valueLayout.size.width + innerPadding * 2f <= barWidth &&
            valueLayout.size.height <= with(density) { style.layout.barHeight.toPx() }

        Column(verticalArrangement = Arrangement.spacedBy(style.layout.labelPadding.coerceAtLeast(0.dp))) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(style.layout.labelPadding.coerceAtLeast(0.dp)),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                BasicText(
                    modifier = Modifier.weight(1f),
                    text = entry.label,
                    style = style.labels.textStyle,
                )
                if (valueText != null && !showInside) {
                    BasicText(text = valueText, style = valueStyle)
                }
            }
            ProgressBar(
                modifier = Modifier.fillMaxWidth().height(style.layout.barHeight.coerceAtLeast(0.dp)),
                entry = entry,
                style = style,
                maximum = maximum,
                fraction = fraction,
                valueLayout = valueLayout.takeIf { showInside },
            )
        }
    }
}

@Composable
private fun ProgressBar(
    modifier: Modifier,
    entry: ProgressChartData,
    style: ProgressStyle,
    maximum: Float,
    fraction: Float,
    valueLayout: TextLayoutResult?,
) {
    Canvas(modifier = modifier) {
        val width = size.width * fraction
        val bar = Rect(left = 0f, top = 0f, right = width, bottom = size.height)
        val corner = style.layout.corner.toPx().coerceAtLeast(0f)
        style.bars.trackColor?.let { track ->
            drawRoundRect(
                color = track,
                cornerRadius = CornerRadius(min(corner, size.minDimension / 2f)),
            )
        }
        if (width > 0f && size.height > 0f) {
            val radius = CornerRadius(min(corner, min(width, size.height) / 2f))
            val brush = style.bars.brushProvider?.invoke(entry, size, bar)
            if (brush != null) {
                drawRoundRect(brush = brush, size = Size(width, size.height), cornerRadius = radius)
            } else {
                drawRoundRect(color = entry.color, size = Size(width, size.height), cornerRadius = radius)
            }
            val stroke = style.bars.strokeWidth.toPx().coerceIn(0f, min(width, size.height))
            if (stroke > 0f) {
                drawRoundRect(
                    color = style.bars.strokeColor,
                    topLeft = Offset(stroke / 2f, stroke / 2f),
                    size = Size(width - stroke, size.height - stroke),
                    cornerRadius = CornerRadius((radius.x - stroke / 2f).coerceAtLeast(0f)),
                    style = Stroke(width = stroke),
                )
            }
        }
        style.target?.let { target ->
            val position = size.width * scaledFraction(
                value = target.value,
                maximum = maximum,
                progression = style.progression,
            )
            drawLine(
                color = target.color,
                start = Offset(position, 0f),
                end = Offset(position, size.height),
                strokeWidth = target.width.toPx(),
            )
        }
        val inside = style.values as? ProgressStyle.Values.Inside
        if (valueLayout != null && inside != null) {
            drawText(
                textLayoutResult = valueLayout,
                color = inside.insideColor ?: inside.textStyle.color,
                topLeft = Offset(
                    x = width - inside.minInnerPadding.toPx() - valueLayout.size.width,
                    y = (size.height - valueLayout.size.height) / 2f,
                ),
            )
        }
    }
}

private fun scaledFraction(
    value: Float,
    maximum: Float,
    progression: ProgressStyle.Progression,
): Float {
    if (!value.isFinite() || value <= 0f) return 0f
    val bounded = value.coerceAtMost(maximum)
    val ratio = (bounded / maximum).coerceIn(0f, 1f)
    return when (progression) {
        is ProgressStyle.Progression.Linear -> ratio
        is ProgressStyle.Progression.Power -> ratio.pow(progression.alpha.takeIf { it.isFinite() && it > 0f } ?: 1f)
        is ProgressStyle.Progression.Logarithmic -> (ln(bounded.toDouble() + 1.0) / ln(maximum.toDouble() + 1.0)).toFloat()
    }
}
