package com.grippo.chart.pie

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import kotlin.math.PI
import kotlin.math.asin
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.sin

@Composable
public fun PieChart(
    modifier: Modifier = Modifier,
    data: PieData,
    style: PieStyle,
) {
    val measurer = rememberTextMeasurer()
    val slices = remember(data.slices) {
        data.slices.filter { it.value.isFinite() && it.value > 0f }
    }
    val total = slices.sumOf { it.value.toDouble() }
    val sweeps = remember(slices, total) {
        slices.map { (it.value / total * 360.0).toFloat() }
    }

    val density = LocalDensity.current

    BoxWithConstraints(modifier = modifier) {
        val hasBoundedHeight = constraints.hasBoundedHeight
        val useLegend = remember(slices, sweeps, style, measurer, density, maxWidth, maxHeight) {
            needsLegend(
                slices = slices,
                sweeps = sweeps,
                style = style,
                measurer = measurer,
                density = density,
                width = with(density) { maxWidth.toPx() },
                height = with(density) { maxHeight.toPx() },
            )
        }
        if (useLegend) {
            val labels = style.labels
            val textStyle = when (labels) {
                is PieStyle.Labels.Adaptive -> labels.textStyle
                is PieStyle.Labels.Inside -> labels.textStyle
                is PieStyle.Labels.Outside -> labels.textStyle
            }
            val formatter = when (labels) {
                is PieStyle.Labels.Adaptive -> labels.formatter
                is PieStyle.Labels.Inside -> labels.formatter
                is PieStyle.Labels.Outside -> labels.formatter
            }
            val gap = when (labels) {
                is PieStyle.Labels.Adaptive -> labels.labelPadding
                is PieStyle.Labels.Inside -> labels.labelPadding
                is PieStyle.Labels.Outside -> labels.labelPadding
            }
            val requestedPlotHeight = style.arc.width * 4 + style.layout.padding * 2
            val first = slices.first()
            val firstLabel = formatter(first, (first.value / total * 100.0).roundToInt())
            val legendMinimumHeight = with(density) {
                val textWidth = (maxWidth.toPx() - (style.arc.width / 2 + gap).toPx()).toInt().coerceAtLeast(1)
                measurer.measure(
                    text = AnnotatedString(firstLabel),
                    style = textStyle,
                    constraints = Constraints(maxWidth = textWidth),
                ).size.height.toDp().coerceAtLeast(style.arc.width / 2)
            }
            val plotHeight = if (hasBoundedHeight) {
                minOf(requestedPlotHeight, (maxHeight - legendMinimumHeight).coerceAtLeast(0.dp))
            } else {
                requestedPlotHeight
            }
            Column(modifier = Modifier.fillMaxSize()) {
                PieDrawing(
                    modifier = Modifier.fillMaxWidth().height(plotHeight),
                    slices = slices,
                    sweeps = sweeps,
                    total = total,
                    style = style,
                    showLabels = false,
                )
                val legendModifier = if (hasBoundedHeight) {
                    Modifier.weight(1f).verticalScroll(rememberScrollState())
                } else {
                    Modifier
                }
                Column(
                    modifier = legendModifier,
                    verticalArrangement = Arrangement.spacedBy(gap),
                ) {
                    slices.forEach { slice ->
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(gap),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Spacer(
                                modifier = Modifier
                                    .size(style.arc.width / 2)
                                    .background(color = slice.color, shape = CircleShape),
                            )
                            BasicText(
                                text = formatter(slice, (slice.value / total * 100.0).roundToInt()),
                                style = textStyle,
                            )
                        }
                    }
                }
            }
        } else {
            PieDrawing(
                modifier = Modifier.matchParentSize(),
                slices = slices,
                sweeps = sweeps,
                total = total,
                style = style,
                showLabels = true,
            )
        }
    }
}

@Composable
private fun PieDrawing(
    modifier: Modifier,
    slices: List<PieSlice>,
    sweeps: List<Float>,
    total: Double,
    style: PieStyle,
    showLabels: Boolean,
) {
    val measurer = rememberTextMeasurer()
    val description = if (showLabels) {
        val formatter = when (val labels = style.labels) {
            is PieStyle.Labels.Adaptive -> labels.formatter
            is PieStyle.Labels.Inside -> labels.formatter
            is PieStyle.Labels.Outside -> labels.formatter
        }
        slices.joinToString(separator = "; ") { slice ->
            val formatted = formatter(slice, (slice.value / total * 100.0).roundToInt())
            if (formatted.contains(slice.label)) formatted else "${slice.label}: $formatted"
        }
    } else {
        null
    }
    val drawingModifier = if (description != null) {
        modifier.semantics { contentDescription = description }
    } else {
        modifier
    }
    Spacer(
        modifier = drawingModifier.drawWithCache {
            val padding = style.layout.padding.toPx().coerceAtLeast(0f)
            val labelPadding = when (val labels = style.labels) {
                is PieStyle.Labels.Adaptive -> labels.labelPadding.toPx()
                is PieStyle.Labels.Inside -> labels.labelPadding.toPx()
                is PieStyle.Labels.Outside -> labels.labelPadding.toPx()
            }.coerceAtLeast(0f)
            val labelStyle = when (val labels = style.labels) {
                is PieStyle.Labels.Adaptive -> labels.textStyle
                is PieStyle.Labels.Inside -> labels.textStyle
                is PieStyle.Labels.Outside -> labels.textStyle
            }
            val formatter = when (val labels = style.labels) {
                is PieStyle.Labels.Adaptive -> labels.formatter
                is PieStyle.Labels.Inside -> labels.formatter
                is PieStyle.Labels.Outside -> labels.formatter
            }
            val labelWidth = (size.width * 0.24f).roundToInt().coerceAtLeast(1)
            var angle = style.layout.startAngleDeg
            val labels = if (!showLabels) emptyList() else slices.mapIndexed { index, slice ->
                val sweep = sweeps[index]
                val middle = angle + sweep / 2f
                angle += sweep
                val percent = (slice.value / total * 100.0).roundToInt()
                val text = formatter(slice, percent)
                val textLayout = measurer.measure(
                    text = AnnotatedString(text),
                    style = labelStyle,
                    constraints = Constraints(maxWidth = labelWidth),
                )
                val outside = when (val configuration = style.labels) {
                    is PieStyle.Labels.Inside -> false
                    is PieStyle.Labels.Outside -> true
                    is PieStyle.Labels.Adaptive -> sweep < configuration.insideMinAngleDeg &&
                        sweep >= configuration.outsideMinAngleDeg
                }
                PieLabel(
                    layout = textLayout,
                    angle = middle,
                    outside = outside,
                    visible = when (val configuration = style.labels) {
                        is PieStyle.Labels.Adaptive -> sweep >= min(configuration.insideMinAngleDeg, configuration.outsideMinAngleDeg)
                        else -> true
                    },
                    right = cos(middle * PI.toFloat() / 180f) >= 0f,
                )
            }
            val outside = labels.filter { it.outside }
            val leaderOffset = style.leaders.offset.toPx().coerceAtLeast(0f)
            val leftGutter = outside.filter { !it.right }.maxOfOrNull { it.layout.size.width }?.toFloat() ?: 0f
            val rightGutter = outside.filter { it.right }.maxOfOrNull { it.layout.size.width }?.toFloat() ?: 0f
            val left = padding + if (leftGutter > 0f) leftGutter + labelPadding + leaderOffset else 0f
            val right = size.width - padding - if (rightGutter > 0f) rightGutter + labelPadding + leaderOffset else 0f
            val radius = min((right - left) / 2f, (size.height - padding * 2f) / 2f).coerceAtLeast(0f)
            val center = Offset(x = (left + right) / 2f, y = size.height / 2f)
            val arcWidth = style.arc.width.toPx().coerceIn(0f, (radius - 1f).coerceAtLeast(0f))
            val labelRadius = radius - arcWidth / 2f
            val leftLabels = outside.filter { !it.right }
            val rightLabels = outside.filter { it.right }
            val leftPositions = outsideLabelPositions(
                labels = leftLabels,
                center = center,
                radius = radius + leaderOffset,
                top = padding,
                bottom = size.height - padding,
                gap = labelPadding,
            )
            val rightPositions = outsideLabelPositions(
                labels = rightLabels,
                center = center,
                radius = radius + leaderOffset,
                top = padding,
                bottom = size.height - padding,
                gap = labelPadding,
            )
            var sliceStart = style.layout.startAngleDeg
            val paths = slices.mapIndexed { index, _ ->
                val start = sliceStart
                val sweep = sweeps[index]
                sliceStart += sweep
                val gap = if (slices.size == 1) 0f else min(style.arc.paddingAngleDeg.coerceAtLeast(0f), sweep * 0.2f)
                roundedSlicePath(
                    center = center,
                    radius = radius,
                    width = arcWidth,
                    startAngle = start + gap / 2f,
                    sweepAngle = sweep - gap,
                    corner = style.arc.cornerRadius.toPx(),
                )
            }

            onDrawBehind {
                if (radius <= 0f || arcWidth <= 0f) return@onDrawBehind
                slices.forEachIndexed { index, slice ->
                    if (slices.size == 1) {
                        drawCircle(
                            color = slice.color,
                            center = center,
                            radius = radius - arcWidth / 2f,
                            style = Stroke(width = arcWidth),
                        )
                    } else {
                        drawPath(path = paths[index], color = slice.color)
                    }
                    val label = labels.getOrNull(index)
                    if (label != null && label.visible && !label.outside) {
                        val position = polarPoint(center = center, radius = labelRadius, angle = label.angle)
                        drawText(
                            textLayoutResult = label.layout,
                            topLeft = Offset(
                                x = position.x - label.layout.size.width / 2f,
                                y = position.y - label.layout.size.height / 2f,
                            ),
                        )
                    }
                }
                outside.forEach { label ->
                    val positions = if (label.right) rightPositions else leftPositions
                    val y = positions.getValue(label)
                    val x = if (label.right) size.width - padding - label.layout.size.width else padding
                    if (style.leaders.show) {
                        val anchor = polarPoint(center = center, radius = radius, angle = label.angle)
                        val elbow = polarPoint(center = center, radius = radius + leaderOffset, angle = label.angle)
                        drawLine(
                            color = labelStyle.color,
                            start = anchor,
                            end = elbow,
                            strokeWidth = style.leaders.lineWidth.toPx(),
                        )
                        drawLine(
                            color = labelStyle.color,
                            start = elbow,
                            end = Offset(
                                x = if (label.right) x - labelPadding else x + label.layout.size.width + labelPadding,
                                y = y + label.layout.size.height / 2f,
                            ),
                            strokeWidth = style.leaders.lineWidth.toPx(),
                        )
                    }
                    drawText(textLayoutResult = label.layout, topLeft = Offset(x = x, y = y))
                }
            }
        },
    )
}


private fun needsLegend(
    slices: List<PieSlice>,
    sweeps: List<Float>,
    style: PieStyle,
    measurer: TextMeasurer,
    density: Density,
    width: Float,
    height: Float,
): Boolean = with(density) {
    if (style.labels is PieStyle.Labels.Inside || slices.isEmpty()) return false
    val labelStyle = when (val labels = style.labels) {
        is PieStyle.Labels.Adaptive -> labels.textStyle
        is PieStyle.Labels.Outside -> labels.textStyle
        is PieStyle.Labels.Inside -> labels.textStyle
    }
    val formatter = when (val labels = style.labels) {
        is PieStyle.Labels.Adaptive -> labels.formatter
        is PieStyle.Labels.Outside -> labels.formatter
        is PieStyle.Labels.Inside -> labels.formatter
    }
    val gap = when (val labels = style.labels) {
        is PieStyle.Labels.Adaptive -> labels.labelPadding.toPx()
        is PieStyle.Labels.Outside -> labels.labelPadding.toPx()
        is PieStyle.Labels.Inside -> labels.labelPadding.toPx()
    }.coerceAtLeast(0f)
    val total = slices.sumOf { it.value.toDouble() }
    val padding = style.layout.padding.toPx().coerceAtLeast(0f)
    val maxLabelWidth = (width * 0.24f).roundToInt().coerceAtLeast(1)
    var angle = style.layout.startAngleDeg
    var leftHeight = 0f
    var rightHeight = 0f
    var leftWidth = 0
    var rightWidth = 0
    slices.forEachIndexed { index, slice ->
        val sweep = sweeps[index]
        val middle = angle + sweep / 2f
        angle += sweep
        val outside = when (val labels = style.labels) {
            is PieStyle.Labels.Outside -> true
            is PieStyle.Labels.Adaptive -> sweep < labels.insideMinAngleDeg && sweep >= labels.outsideMinAngleDeg
            is PieStyle.Labels.Inside -> false
        }
        if (outside) {
            val layout = measurer.measure(
                text = AnnotatedString(formatter(slice, (slice.value / total * 100.0).roundToInt())),
                style = labelStyle,
                constraints = Constraints(maxWidth = maxLabelWidth),
            )
            if (cos(middle * PI.toFloat() / 180f) >= 0f) {
                rightHeight += layout.size.height + gap
                rightWidth = maxOf(rightWidth, layout.size.width)
            } else {
                leftHeight += layout.size.height + gap
                leftWidth = maxOf(leftWidth, layout.size.width)
            }
        }
    }
    val availableHeight = (height - padding * 2f).coerceAtLeast(0f)
    val availableWidth = width - padding * 2f - leftWidth - rightWidth -
        (if (leftWidth > 0) gap + style.leaders.offset.toPx() else 0f) -
        (if (rightWidth > 0) gap + style.leaders.offset.toPx() else 0f)
    maxOf(leftHeight, rightHeight) - gap > availableHeight || availableWidth <= style.arc.width.toPx() * 2f
}

private data class PieLabel(
    val layout: TextLayoutResult,
    val angle: Float,
    val outside: Boolean,
    val visible: Boolean,
    val right: Boolean,
)

private fun outsideLabelPositions(
    labels: List<PieLabel>,
    center: Offset,
    radius: Float,
    top: Float,
    bottom: Float,
    gap: Float,
): Map<PieLabel, Float> {
    val ordered = labels.sortedBy { sin(it.angle * PI.toFloat() / 180f) }
    val positions = FloatArray(ordered.size)
    var previousBottom = top - gap
    ordered.forEachIndexed { index, label ->
        val desired = polarPoint(center = center, radius = radius, angle = label.angle).y - label.layout.size.height / 2f
        val position = maxOf(desired, previousBottom + gap)
        positions[index] = position
        previousBottom = position + label.layout.size.height
    }
    var nextTop = bottom + gap
    for (index in ordered.indices.reversed()) {
        positions[index] = min(positions[index], nextTop - gap - ordered[index].layout.size.height)
        nextTop = positions[index]
    }
    return ordered.indices.associate { ordered[it] to positions[it] }
}

private fun roundedSlicePath(
    center: Offset,
    radius: Float,
    width: Float,
    startAngle: Float,
    sweepAngle: Float,
    corner: Float,
): Path {
    val innerRadius = radius - width
    if (radius <= 0f || innerRadius <= 0f || sweepAngle <= 0f) return Path()
    val outerCorner = sliceCorner(radius = radius, width = width, sweep = sweepAngle, corner = corner)
    val innerCorner = sliceCorner(radius = innerRadius, width = width, sweep = sweepAngle, corner = corner)
    val endAngle = startAngle + sweepAngle
    val topLeft = polarPoint(center = center, radius = radius, angle = startAngle)
    val topLeftStart = polarPoint(center = center, radius = radius - outerCorner.first, angle = startAngle)
    val topLeftEnd = polarPoint(center = center, radius = radius, angle = startAngle + outerCorner.second)
    val topRight = polarPoint(center = center, radius = radius, angle = endAngle)
    val topRightEnd = polarPoint(center = center, radius = radius - outerCorner.first, angle = endAngle)
    val bottomRight = polarPoint(center = center, radius = innerRadius, angle = endAngle)
    val bottomRightStart = polarPoint(center = center, radius = innerRadius + innerCorner.first, angle = endAngle)
    val bottomRightEnd = polarPoint(center = center, radius = innerRadius, angle = endAngle - innerCorner.second)
    val bottomLeft = polarPoint(center = center, radius = innerRadius, angle = startAngle)
    val bottomLeftEnd = polarPoint(center = center, radius = innerRadius + innerCorner.first, angle = startAngle)
    return Path().apply {
        moveTo(topLeftStart.x, topLeftStart.y)
        quadraticTo(topLeft.x, topLeft.y, topLeftEnd.x, topLeftEnd.y)
        arcTo(
            rect = Rect(center.x - radius, center.y - radius, center.x + radius, center.y + radius),
            startAngleDegrees = startAngle + outerCorner.second,
            sweepAngleDegrees = sweepAngle - outerCorner.second * 2f,
            forceMoveTo = false,
        )
        quadraticTo(topRight.x, topRight.y, topRightEnd.x, topRightEnd.y)
        lineTo(bottomRightStart.x, bottomRightStart.y)
        quadraticTo(bottomRight.x, bottomRight.y, bottomRightEnd.x, bottomRightEnd.y)
        arcTo(
            rect = Rect(center.x - innerRadius, center.y - innerRadius, center.x + innerRadius, center.y + innerRadius),
            startAngleDegrees = endAngle - innerCorner.second,
            sweepAngleDegrees = -(sweepAngle - innerCorner.second * 2f),
            forceMoveTo = false,
        )
        quadraticTo(bottomLeft.x, bottomLeft.y, bottomLeftEnd.x, bottomLeftEnd.y)
        close()
    }
}

private fun sliceCorner(radius: Float, width: Float, sweep: Float, corner: Float): Pair<Float, Float> {
    val safeCorner = corner.coerceIn(0f, min(width / 2f, radius))
    val travel = (2.0 * asin((safeCorner / (2f * radius)).toDouble()) * 180.0 / PI).toFloat()
    return if (travel > sweep / 2f) {
        radius * sin(sweep * PI.toFloat() / 360f) to sweep / 2f
    } else {
        safeCorner to travel
    }
}

private fun polarPoint(center: Offset, radius: Float, angle: Float): Offset {
    val radians = angle * PI.toFloat() / 180f
    return Offset(x = center.x + radius * cos(radians), y = center.y + radius * sin(radians))
}
