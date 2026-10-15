package com.grippo.design.components.chart.internal

import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import com.grippo.chart.radar.RadarAxis
import com.grippo.chart.radar.RadarChart
import com.grippo.chart.radar.RadarData
import com.grippo.chart.radar.RadarSeries
import com.grippo.chart.radar.RadarStyle
import com.grippo.chart.radar.RadarValues
import com.grippo.design.core.AppTokens
import com.grippo.design.preview.AppPreview
import com.grippo.design.preview.PreviewContainer
import kotlin.math.PI
import kotlin.math.ceil
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin

@Composable
internal fun RadarChart(
    modifier: Modifier = Modifier,
    data: RadarData,
    showLabels: Boolean,
    clickable: Boolean
) {
    val density = LocalDensity.current
    val measurer = rememberTextMeasurer()
    val charts = AppTokens.colors.charts
    val palette = AppTokens.colors.palette

    val scaleStopsOrangeRed: List<Pair<Float, Color>> = remember(palette.palette5OrangeRedGrowth) {
        val colors = palette.palette5OrangeRedGrowth
        val last = (colors.size - 1).coerceAtLeast(1)
        colors.mapIndexed { index, color ->
            index.toFloat() / last.toFloat() to color
        }
    }

    val labelTextStyle = AppTokens.typography.b11Med().copy(color = AppTokens.colors.text.primary)
    val axisLabelLayouts: List<TextLayoutResult> = remember(
        measurer,
        showLabels,
        data.axes,
        labelTextStyle,
        density.density,
        density.fontScale
    ) {
        if (!showLabels) {
            emptyList()
        } else {
            data.axes.map { axis ->
                measurer.measure(AnnotatedString(axis.label), labelTextStyle)
            }
        }
    }

    val labels = if (showLabels) {
        RadarStyle.Labels.Visible(textStyle = labelTextStyle)
    } else {
        RadarStyle.Labels.None
    }

    val style = RadarStyle(
        layout = RadarStyle.Layout(
            labelPadding = AppTokens.dp.metrics.charts.radar.labelPadding,
            startAngleDeg = -90f,
            clockwise = true,
            reserveAxisLabelSpace = showLabels,
            centered = false,
        ),
        grid = RadarStyle.Grid(
            levels = 5,
            asPolygon = true,
            color = AppTokens.colors.charts.radar.grid,
            strokeWidth = AppTokens.dp.metrics.charts.axis.strokeWidth,
            levelLabels = RadarStyle.LevelLabels.None,
        ),
        spokes = RadarStyle.Spokes.Visible(
            color = AppTokens.colors.charts.radar.grid,
            strokeWidth = AppTokens.dp.metrics.charts.axis.strokeWidth,
        ),
        peek = if (clickable) RadarStyle.Peek.Visible(
            hitSlop = AppTokens.dp.metrics.charts.peek.hitSlop,

            guideColor = charts.tooltip.guide,
            guideWidth = AppTokens.dp.metrics.charts.peek.guideWidth,
            guideDash = AppTokens.dp.metrics.charts.peek.guideDash,
            guideGap = AppTokens.dp.metrics.charts.peek.guideGap,

            focusColor = charts.tooltip.focus,
            focusRadius = AppTokens.dp.metrics.charts.peek.focusRadius,
            focusRingWidth = AppTokens.dp.metrics.charts.radar.focusRingWidth,
            focusHaloRadius = AppTokens.dp.metrics.charts.radar.focusHaloRadius,

            tooltipBackground = charts.tooltip.background,
            tooltipBorder = charts.tooltip.border,
            tooltipText = charts.tooltip.text,
            tooltipCornerRadius = AppTokens.dp.metrics.charts.peek.tooltipRadius,
            tooltipPaddingH = AppTokens.dp.metrics.charts.peek.tooltipHorizontalPadding,
            tooltipPaddingV = AppTokens.dp.metrics.charts.peek.tooltipVerticalPadding,
            tooltipMargin = AppTokens.dp.metrics.charts.peek.tooltipMargin,

            decimals = 0,
            showAxisLabel = true,
            showSeriesName = false,
        ) else RadarStyle.Peek.None,
        labels = labels,
        polygon = RadarStyle.Polygon(
            strokeWidth = AppTokens.dp.metrics.charts.radar.seriesWidth,
            fillAlpha = charts.radar.fillAlpha,
        ),
        vertices = RadarStyle.Vertices.Visible(
            radius = AppTokens.dp.metrics.charts.radar.dotRadius,
            colorOverride = null,
        ),
        values = RadarStyle.Values.None,
        dataPolicy = RadarStyle.DataPolicy(
            requireCompleteSeries = true,
            missingAsZero = true,
        ),
        colorStops = scaleStopsOrangeRed,
    )

    val autoHeightModifier = Modifier.layout { measurable, constraints ->
        if (!constraints.hasBoundedWidth) {
            val placeable = measurable.measure(constraints)
            layout(placeable.width, placeable.height) {
                placeable.place(0, 0)
            }
        } else {
            val heightPx = computeHeightForWidthPx(
                widthPx = constraints.maxWidth,
                data = data,
                style = style,
                density = density,
                axisLabelLayouts = axisLabelLayouts,
            )

            val coercedHeight = if (constraints.hasBoundedHeight) {
                heightPx.coerceIn(constraints.minHeight, constraints.maxHeight)
            } else {
                max(heightPx, constraints.minHeight)
            }

            val placeable = measurable.measure(
                constraints.copy(
                    minHeight = coercedHeight,
                    maxHeight = coercedHeight
                )
            )

            layout(placeable.width, placeable.height) {
                placeable.place(0, 0)
            }
        }
    }

    RadarChart(
        modifier = modifier.then(autoHeightModifier),
        data = data,
        style = style
    )
}

private fun computeHeightForWidthPx(
    widthPx: Int,
    data: RadarData,
    style: RadarStyle,
    density: Density,
    axisLabelLayouts: List<TextLayoutResult>,
): Int {
    if (widthPx <= 0 || data.axes.isEmpty()) return 0

    val w = widthPx.toFloat()
    val n = data.axes.size
    val dir = if (style.layout.clockwise) 1f else -1f
    val angleStep = (2f * PI.toFloat() / n) * dir
    val startRad = style.layout.startAngleDeg * PI.toFloat() / 180f
    val horizontalDirections = FloatArray(n) { index -> cos(startRad + index * angleStep) }
    val verticalDirections = FloatArray(n) { index -> sin(startRad + index * angleStep) }

    val gridInset = with(density) { style.grid.strokeWidth.toPx() } * 0.5f
    val polygonInset = with(density) { style.polygon.strokeWidth.toPx() } * 0.5f
    val spokesInset = when (val sp = style.spokes) {
        is RadarStyle.Spokes.Visible -> with(density) { sp.strokeWidth.toPx() } * 0.5f
        is RadarStyle.Spokes.None -> 0f
    }

    val inset = max(gridInset, max(polygonInset, spokesInset)).coerceAtLeast(0f)
    val labelPadPx = with(density) { style.layout.labelPadding.toPx() }
    val reserveLabels = axisLabelLayouts.isNotEmpty() && style.layout.reserveAxisLabelSpace

    var upperRadius = max(0f, w)
    repeat(8) {
        val requiredWidth = requiredWidthForRadius(
            radius = upperRadius,
            horizontalDirections = horizontalDirections,
            labelLayouts = axisLabelLayouts,
            labelPadding = labelPadPx,
            inset = inset,
            reserveLabels = reserveLabels,
        )
        if (requiredWidth < w && upperRadius > 0f) {
            upperRadius = (upperRadius * 1.5f).coerceAtMost(w * 4f)
        }
    }

    var lowerRadius = 0f
    repeat(26) {
        val candidateRadius = (lowerRadius + upperRadius) * 0.5f
        val requiredWidth = requiredWidthForRadius(
            radius = candidateRadius,
            horizontalDirections = horizontalDirections,
            labelLayouts = axisLabelLayouts,
            labelPadding = labelPadPx,
            inset = inset,
            reserveLabels = reserveLabels,
        )
        if (requiredWidth <= w) {
            lowerRadius = candidateRadius
        } else {
            upperRadius = candidateRadius
        }
    }
    val radius = lowerRadius

    var minY = Float.POSITIVE_INFINITY
    var maxY = Float.NEGATIVE_INFINITY
    for (i in 0 until n) {
        val y = radius * verticalDirections[i]
        minY = min(minY, y)
        maxY = max(maxY, y)
    }
    if (reserveLabels) {
        val rr = radius + labelPadPx
        for (i in 0 until n) {
            val halfH = axisLabelLayouts[i].size.height * 0.5f
            val y = rr * verticalDirections[i]
            minY = min(minY, y - halfH)
            maxY = max(maxY, y + halfH)
        }
    }

    val contentHeight = (maxY - minY).coerceAtLeast(0f)
    return ceil(contentHeight + inset * 2f).toInt()
}

private fun requiredWidthForRadius(
    radius: Float,
    horizontalDirections: FloatArray,
    labelLayouts: List<TextLayoutResult>,
    labelPadding: Float,
    inset: Float,
    reserveLabels: Boolean,
): Float {
    var minX = Float.POSITIVE_INFINITY
    var maxX = Float.NEGATIVE_INFINITY
    horizontalDirections.forEachIndexed { index, direction ->
        val x = radius * direction
        minX = min(minX, x)
        maxX = max(maxX, x)
        if (reserveLabels) {
            val labelX = (radius + labelPadding) * direction
            val halfWidth = labelLayouts[index].size.width * 0.5f
            minX = min(minX, labelX - halfWidth)
            maxX = max(maxX, labelX + halfWidth)
        }
    }
    return (maxX - minX).coerceAtLeast(0f) + inset * 2f
}

@AppPreview
@Composable
private fun RadarChartPreview() {
    PreviewContainer {
        val ds = RadarData(
            axes = listOf(
                RadarAxis("chest", "Chest"),
                RadarAxis("back", "Back"),
                RadarAxis("legs", "Legs"),
                RadarAxis("shoulders", "Shoulders"),
                RadarAxis("arms", "Arms"),
                RadarAxis("core", "Core"),
            ),
            series = listOf(
                RadarSeries(
                    name = "Current",
                    color = AppTokens.colors.context.muscle,
                    values = RadarValues.ByAxisId(
                        mapOf(
                            "chest" to 0.75f,
                            "back" to 0.6f,
                            "legs" to 0.9f,
                            "shoulders" to 0.55f,
                            "arms" to 0.7f,
                            "core" to 0.5f
                        )
                    )
                )
            )
        )

        RadarChart(
            modifier = Modifier.size(300.dp),
            data = ds,
            showLabels = true,
            clickable = true
        )
    }
}

@AppPreview
@Composable
private fun RadarChartWithoutLabelsPreview() {
    PreviewContainer {
        RadarChart(
            modifier = Modifier.size(300.dp),
            data = RadarData(
                axes = listOf(
                    RadarAxis("chest", "Chest"),
                    RadarAxis("back", "Back"),
                    RadarAxis("legs", "Legs"),
                ),
                series = listOf(
                    RadarSeries(
                        name = "Current",
                        color = AppTokens.colors.context.muscle,
                        values = RadarValues.ByIndex(listOf(0f, 0.5f, 1f)),
                    ),
                ),
            ),
            showLabels = false,
            clickable = false,
        )
    }
}

@AppPreview
@Composable
private fun RadarChartEmptyPreview() {
    PreviewContainer {
        RadarChart(
            modifier = Modifier.size(300.dp),
            data = RadarData(axes = emptyList(), series = emptyList()),
            showLabels = true,
            clickable = true,
        )
    }
}
