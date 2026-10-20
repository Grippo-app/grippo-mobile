package com.grippo.chart.sparkline

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

@Composable
public fun Sparkline(
    modifier: Modifier = Modifier,
    data: SparklineData,
    style: SparklineStyle,
) {
    val density = LocalDensity.current
    val textMeasurer = rememberTextMeasurer()
    val sortedCurrentPoints = remember(data.points) { data.points.sortedBy { it.x } }

    var sizePx by remember { mutableStateOf(IntSize.Zero) }
    var selectedIndex by remember(data.points, style.peek is SparklineStyle.Peek.Visible) { mutableStateOf<Int?>(null) }
    var settledPoints by remember { mutableStateOf<List<SparklinePoint>>(emptyList()) }
    val previousPoints = settledPoints
    val drawProgress = remember(sortedCurrentPoints) { Animatable(0f) }

    LaunchedEffect(sortedCurrentPoints) {
        if (previousPoints.isEmpty()) {
            drawProgress.snapTo(1f)
        } else {
            drawProgress.animateTo(
                targetValue = 1f,
                animationSpec = tween(
                    durationMillis = 520,
                    easing = FastOutSlowInEasing,
                ),
            )
        }
        settledPoints = sortedCurrentPoints
    }

    val layout = remember(sizePx, data.points, style, density) {
        buildLayout(
            sizePx = sizePx,
            data = SparklineData(points = sortedCurrentPoints),
            style = style,
            density = density,
        )
    }
    val previousLayout = remember(sizePx, previousPoints, style, density) {
        buildLayout(
            sizePx = sizePx,
            data = SparklineData(points = previousPoints),
            style = style,
            density = density
        )
    }
    val transitionFromOffsets = remember(layout, previousLayout, previousPoints, data.points) {
        val currentLayout = layout ?: return@remember emptyList()
        buildTransitionFromOffsets(
            currentLayout = currentLayout,
            previousLayout = previousLayout,
            previousPoints = previousPoints,
            currentPoints = sortedCurrentPoints
        )
    }

    val linePath = remember { Path() }
    val areaPath = remember { Path() }
    val midlinePath = remember { Path() }
    val guidePath = remember { Path() }

    val peek = style.peek
    val currentOffsets = remember(layout) { layout?.currentPoints?.map { it.offset }.orEmpty() }

    val visibleOffset: (Int) -> Offset = { index ->
        val target = currentOffsets[index]
        val source = transitionFromOffsets.getOrNull(index)
        if (source == null) target else lerpOffset(source, target, drawProgress.value)
    }

    val pointerModifier = if (peek is SparklineStyle.Peek.Visible) {
        Modifier.pointerInput(layout, peek, transitionFromOffsets) {
            detectTapGestures { position ->
                val currentLayout = layout ?: return@detectTapGestures
                val hitSlop = with(density) { peek.hitSlop.toPx() }
                if (!currentLayout.chart.inflate(hitSlop).contains(position)) {
                    selectedIndex = null
                    return@detectTapGestures
                }
                val nearest = currentOffsets.indices.minByOrNull { index ->
                    abs(visibleOffset(index).x - position.x)
                }
                selectedIndex = if (selectedIndex == nearest) null else nearest
            }
        }.pointerInput(layout, peek, transitionFromOffsets) {
            var isScrubbing = false
            detectHorizontalDragGestures(
                onDragStart = { position ->
                    isScrubbing = layout?.chart?.contains(position) == true
                    if (isScrubbing) {
                        selectedIndex = currentOffsets.indices.minByOrNull { index ->
                            abs(visibleOffset(index).x - position.x)
                        }
                    }
                },
                onDragEnd = { isScrubbing = false },
                onDragCancel = { isScrubbing = false },
                onHorizontalDrag = { change, _ ->
                    if (isScrubbing) {
                        selectedIndex = currentOffsets.indices.minByOrNull { index ->
                            abs(visibleOffset(index).x - change.position.x)
                        }
                    }
                },
            )
        }
    } else {
        Modifier
    }

    Box(
        modifier = modifier
            .then(pointerModifier)
            .onSizeChanged { sizePx = it }
    ) {
        Canvas(Modifier.matchParentSize()) {
            val l = layout ?: return@Canvas
            val chart = l.chart

            when (val base = style.baseline) {
                is SparklineStyle.Baseline.None -> Unit
                is SparklineStyle.Baseline.Visible -> {
                    val y = l.my((base.value ?: l.minY).coerceIn(l.minY, l.maxY))
                    drawLine(
                        color = base.color,
                        start = Offset(chart.left, y),
                        end = Offset(chart.right, y),
                        strokeWidth = base.width.toPx(),
                        cap = StrokeCap.Round
                    )
                }
            }

            when (val mid = style.midline) {
                is SparklineStyle.Midline.None -> Unit
                is SparklineStyle.Midline.Visible -> {
                    val y = chart.top + chart.height * mid.position.coerceIn(0f, 1f)
                    val dash = mid.dash.toPx().coerceAtLeast(1f)
                    val gap = mid.gap.toPx().coerceAtLeast(1f)

                    midlinePath.reset()
                    midlinePath.moveTo(chart.left, y)
                    midlinePath.lineTo(chart.right, y)

                    drawPath(
                        path = midlinePath,
                        color = mid.color,
                        style = Stroke(
                            width = mid.width.toPx(),
                            cap = StrokeCap.Round,
                            pathEffect = PathEffect.dashPathEffect(
                                intervals = floatArrayOf(dash, gap),
                                phase = 0f
                            )
                        )
                    )
                }
            }

            val current = l.currentPoints
            if (current.isEmpty()) return@Canvas

            val progress = drawProgress.value
            val animatedOffsets = interpolateOffsets(
                from = transitionFromOffsets,
                to = currentOffsets,
                progress = progress
            )

            if (current.size == 1) {
                val c = animatedOffsets.first()

                val radius = when (val dots = style.dots) {
                    is SparklineStyle.Dots.Visible -> dots.radius.toPx()
                    is SparklineStyle.Dots.None -> style.line.stroke.toPx()
                }
                drawCircle(
                    color = style.line.color,
                    radius = radius,
                    center = c,
                )

                if (peek is SparklineStyle.Peek.Visible && selectedIndex == 0) {
                    drawPeekOverlay(
                        guidePath = guidePath,
                        point = c,
                        focusColor = peek.focusColor ?: style.line.color,
                        guideColor = peek.guideColor,
                        guideWidthPx = peek.guideWidth.toPx(),
                        guideDashPx = peek.guideDash.toPx(),
                        guideGapPx = peek.guideGap.toPx(),
                        focusRadiusPx = peek.focusRadius.toPx(),
                        focusRingWidthPx = peek.focusRingWidth.toPx(),
                        focusHaloRadiusPx = peek.focusHaloRadius.toPx(),
                        chart = chart
                    )
                }

                return@Canvas
            }

            buildLinePath(
                path = linePath,
                points = animatedOffsets,
                curved = style.line.curved,
                smoothness = style.line.curveSmoothness,
                clampOvershoot = style.line.clampOvershoot
            )

            style.fill?.provider?.let { provider ->
                areaPath.reset()
                areaPath.addPath(linePath)
                areaPath.lineTo(animatedOffsets.last().x, chart.bottom)
                areaPath.lineTo(animatedOffsets.first().x, chart.bottom)
                areaPath.close()
                drawPath(areaPath, brush = provider(chart))
            }

            val sw = style.line.stroke.toPx()
            style.line.brush?.let { provider ->
                drawPath(
                    path = linePath,
                    brush = provider(chart),
                    style = Stroke(width = sw, cap = StrokeCap.Round)
                )
            } ?: run {
                drawPath(
                    path = linePath,
                    color = style.line.color,
                    style = Stroke(width = sw, cap = StrokeCap.Round)
                )
            }

            when (val d = style.dots) {
                is SparklineStyle.Dots.None -> Unit
                is SparklineStyle.Dots.Visible -> {
                    val r = d.radius.toPx()
                    val dc = d.color ?: style.line.color
                    animatedOffsets.forEach {
                        drawCircle(
                            color = dc,
                            radius = r,
                            center = it
                        )
                    }
                }
            }

            when (val ex = style.extremes) {
                is SparklineStyle.Extremes.None -> Unit
                is SparklineStyle.Extremes.Visible -> {
                    if (current.size >= 2) {
                        val rr = ex.radius.toPx()
                        val minIdx = current.indices.minByOrNull { current[it].raw.y } ?: 0
                        val maxIdx = current.indices.maxByOrNull { current[it].raw.y } ?: 0

                        if (minIdx == maxIdx) {
                            drawCircle(
                                color = ex.maxColor,
                                radius = rr,
                                center = animatedOffsets[maxIdx]
                            )
                        } else {
                            drawCircle(
                                color = ex.minColor,
                                radius = rr,
                                center = animatedOffsets[minIdx]
                            )
                            drawCircle(
                                color = ex.maxColor,
                                radius = rr,
                                center = animatedOffsets[maxIdx]
                            )
                        }
                    }
                }
            }

            if (peek is SparklineStyle.Peek.Visible) {
                val idx = selectedIndex
                if (idx != null && idx in current.indices) {
                    val p = animatedOffsets[idx]
                    drawPeekOverlay(
                        guidePath = guidePath,
                        point = p,
                        focusColor = peek.focusColor ?: style.line.color,
                        guideColor = peek.guideColor,
                        guideWidthPx = peek.guideWidth.toPx(),
                        guideDashPx = peek.guideDash.toPx(),
                        guideGapPx = peek.guideGap.toPx(),
                        focusRadiusPx = peek.focusRadius.toPx(),
                        focusRingWidthPx = peek.focusRingWidth.toPx(),
                        focusHaloRadiusPx = peek.focusHaloRadius.toPx(),
                        chart = chart
                    )
                }
            }
        }

        if (peek is SparklineStyle.Peek.Visible) {
            val l = layout
            val idx = selectedIndex
            if (l != null && idx != null && idx in l.currentPoints.indices) {
                val mp = l.currentPoints[idx]
                val valueText = peek.valueFormatter?.invoke(mp.raw.y) ?: formatValue(mp.raw.y, peek.decimals)
                val labelText = if (peek.showLabel) mp.raw.label else null

                val tooltipSize = remember(valueText, labelText, peek, density, sizePx.width, textMeasurer) {
                    measureTooltipSize(
                        textMeasurer = textMeasurer,
                        density = density,
                        valueText = valueText,
                        labelText = labelText,
                        style = peek,
                        canvasWidth = sizePx.width,
                    )
                }

                SparklineTooltip(
                    anchor = { visibleOffset(idx) },
                    canvasSize = sizePx,
                    tooltipSize = tooltipSize,
                    style = peek,
                    valueText = valueText,
                    labelText = labelText,
                )
            }
        }
    }
}

private data class MappedPoint(
    val raw: SparklinePoint,
    val offset: Offset,
)

private data class SparklineLayout(
    val chart: Rect,
    val currentPoints: List<MappedPoint>,
    val minY: Float,
    val maxY: Float,
    val mx: (Float) -> Float,
    val my: (Float) -> Float,
)

private fun buildLayout(
    sizePx: IntSize,
    data: SparklineData,
    style: SparklineStyle,
    density: androidx.compose.ui.unit.Density,
): SparklineLayout? {
    if (sizePx.width <= 0 || sizePx.height <= 0) return null
    if (data.points.isEmpty() || data.points.any { !it.x.isFinite() || !it.y.isFinite() }) return null

    val dotsPad = when (val d = style.dots) {
        is SparklineStyle.Dots.None -> 0f
        is SparklineStyle.Dots.Visible -> with(density) { d.radius.toPx() }
    }

    val extremesPad = when (val ex = style.extremes) {
        is SparklineStyle.Extremes.None -> 0f
        is SparklineStyle.Extremes.Visible -> with(density) { ex.radius.toPx() }
    }

    val focusPadding = when (val peek = style.peek) {
        is SparklineStyle.Peek.None -> 0f
        is SparklineStyle.Peek.Visible -> with(density) {
            max(peek.focusRadius.toPx() + peek.focusRingWidth.toPx() / 2f, peek.focusHaloRadius.toPx())
        }
    }
    val padding = maxOf(dotsPad, extremesPad, focusPadding, with(density) { style.line.stroke.toPx() / 2f })
    val verticalPadding = max(padding, with(density) { style.verticalPadding.toPx() })

    val chart = Rect(
        left = padding,
        top = verticalPadding,
        right = sizePx.width.toFloat() - padding,
        bottom = sizePx.height.toFloat() - verticalPadding
    )

    if (chart.width <= 0f || chart.height <= 0f) return null

    val all = data.points

    val minX = all.minOf { it.x }
    val maxX = all.maxOf { it.x }
    val minY = all.minOf { it.y }
    val maxY = all.maxOf { it.y }

    val spanXRaw = maxX.toDouble() - minX
    val spanYRaw = maxY.toDouble() - minY
    val flatX = spanXRaw <= 1e-6f
    val flatY = spanYRaw <= 1e-6f
    val spanX = if (flatX) 1.0 else spanXRaw
    val spanY = if (flatY) 1.0 else spanYRaw

    val mapX: (Float) -> Float = { x ->
        if (flatX) chart.left + chart.width / 2f
        else chart.left + ((x.toDouble() - minX) / spanX * chart.width).toFloat()
    }

    val mapY: (Float) -> Float = { y ->
        if (flatY) chart.top + chart.height / 2f
        else chart.bottom - ((y.toDouble() - minY) / spanY * chart.height).toFloat()
    }

    val currentMapped = data.points.map { MappedPoint(it, Offset(x = mapX(it.x), y = mapY(it.y))) }

    return SparklineLayout(
        chart = chart,
        currentPoints = currentMapped,
        minY = minY,
        maxY = maxY,
        mx = mapX,
        my = mapY,
    )
}

private fun buildLinePath(
    path: Path,
    points: List<Offset>,
    curved: Boolean,
    smoothness: Float,
    clampOvershoot: Boolean,
) {
    path.reset()
    if (points.isEmpty()) return

    if (!curved || points.size < 3) {
        path.moveTo(points.first().x, points.first().y)
        for (i in 1 until points.size) path.lineTo(points[i].x, points[i].y)
        return
    }

    val s = smoothness.coerceIn(0f, 0.5f)
    val p = points

    path.moveTo(p.first().x, p.first().y)

    for (i in 0 until p.lastIndex) {
        val p0 = if (i == 0) p[i] else p[i - 1]
        val p1 = p[i]
        val p2 = p[i + 1]
        val p3 = if (i + 2 <= p.lastIndex) p[i + 2] else p[i + 1]

        var c1 = Offset(
            x = p1.x + (p2.x - p0.x) * s,
            y = p1.y + (p2.y - p0.y) * s
        )
        var c2 = Offset(
            x = p2.x - (p3.x - p1.x) * s,
            y = p2.y - (p3.y - p1.y) * s
        )

        if (clampOvershoot) {
            val minY = min(p1.y, p2.y)
            val maxY = max(p1.y, p2.y)
            c1 = Offset(x = c1.x.coerceIn(p1.x, p2.x), y = c1.y.coerceIn(minY, maxY))
            c2 = Offset(x = c2.x.coerceIn(p1.x, p2.x), y = c2.y.coerceIn(minY, maxY))
        }

        path.cubicTo(c1.x, c1.y, c2.x, c2.y, p2.x, p2.y)
    }
}

private fun DrawScope.drawPeekOverlay(
    guidePath: Path,
    point: Offset,
    focusColor: androidx.compose.ui.graphics.Color,
    guideColor: androidx.compose.ui.graphics.Color,
    guideWidthPx: Float,
    guideDashPx: Float,
    guideGapPx: Float,
    focusRadiusPx: Float,
    focusRingWidthPx: Float,
    focusHaloRadiusPx: Float,
    chart: Rect,
) {
    guidePath.reset()
    guidePath.moveTo(point.x, chart.top)
    guidePath.lineTo(point.x, chart.bottom)

    drawPath(
        path = guidePath,
        color = guideColor,
        style = Stroke(
            width = guideWidthPx.coerceAtLeast(1f),
            cap = StrokeCap.Round,
            pathEffect = PathEffect.dashPathEffect(
                intervals = floatArrayOf(
                    guideDashPx.coerceAtLeast(1f),
                    guideGapPx.coerceAtLeast(1f)
                ),
                phase = 0f
            )
        )
    )

    if (focusHaloRadiusPx > 0f) {
        val halo = Brush.radialGradient(
            colors = listOf(focusColor.copy(alpha = 0.20f), focusColor.copy(alpha = 0f)),
            center = point,
            radius = focusHaloRadiusPx,
        )
        drawCircle(brush = halo, radius = focusHaloRadiusPx, center = point)
    }
    if (focusRadiusPx > 0f && focusRingWidthPx > 0f) {
        drawCircle(
            color = focusColor,
            radius = focusRadiusPx,
            center = point,
            style = Stroke(width = focusRingWidthPx),
        )
    }
    if (focusRadiusPx > 0f) {
        drawCircle(color = focusColor, radius = focusRadiusPx, center = point)
    }
}

@Composable
private fun SparklineTooltip(
    anchor: () -> Offset,
    canvasSize: IntSize,
    tooltipSize: IntSize,
    style: SparklineStyle.Peek.Visible,
    valueText: String,
    labelText: String?,
) {
    val density = LocalDensity.current
    val marginPx = with(density) { style.tooltipMargin.roundToPx() }
    if (tooltipSize.width <= 0 || tooltipSize.height <= 0 ||
        canvasSize.width <= 0 || tooltipSize.height > canvasSize.height
    ) {
        return
    }
    val shape = RoundedCornerShape(style.tooltipCornerRadius)

    Box(
        modifier = Modifier
            .offset {
                tooltipPosition(
                    anchor = anchor(),
                    canvasSize = canvasSize,
                    tooltipSize = tooltipSize,
                    marginPx = marginPx,
                )
            }
            .width(with(density) { tooltipSize.width.toDp() })
            .background(color = style.tooltipBackground, shape = shape)
            .then(
                if (style.tooltipBorderWidth > Dp.Hairline) {
                    Modifier.border(style.tooltipBorderWidth, style.tooltipBorder, shape)
                } else {
                    Modifier
                }
            )
            .padding(horizontal = style.tooltipPaddingH, vertical = style.tooltipPaddingV)
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(style.tooltipLabelSpacing),
        ) {
            BasicText(
                text = valueText,
                style = style.valueTextStyle.copy(color = style.tooltipText),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            if (!labelText.isNullOrBlank()) {
                BasicText(
                    text = labelText,
                    style = style.labelTextStyle.copy(color = style.tooltipText),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

private fun measureTooltipSize(
    textMeasurer: TextMeasurer,
    density: Density,
    valueText: String,
    labelText: String?,
    style: SparklineStyle.Peek.Visible,
    canvasWidth: Int,
): IntSize {
    val padH = with(density) { style.tooltipPaddingH.roundToPx() }
    val padV = with(density) { style.tooltipPaddingV.roundToPx() }
    val constraints = Constraints(maxWidth = (canvasWidth - padH * 2).coerceAtLeast(1))
    val value = textMeasurer.measure(
        text = AnnotatedString(valueText),
        style = style.valueTextStyle,
        overflow = TextOverflow.Ellipsis,
        maxLines = 1,
        constraints = constraints,
    ).size
    val label = if (!labelText.isNullOrBlank()) {
        textMeasurer.measure(
            text = AnnotatedString(labelText),
            style = style.labelTextStyle,
            overflow = TextOverflow.Ellipsis,
            maxLines = 1,
            constraints = constraints,
        ).size
    } else null
    val gap = if (label != null) with(density) { style.tooltipLabelSpacing.roundToPx() } else 0

    return IntSize(
        width = (max(value.width, label?.width ?: 0) + padH * 2).coerceAtMost(canvasWidth),
        height = value.height + (label?.height ?: 0) + gap + padV * 2,
    )
}

private fun tooltipPosition(
    anchor: Offset,
    canvasSize: IntSize,
    tooltipSize: IntSize,
    marginPx: Int,
): IntOffset {
    val width = tooltipSize.width
    val height = tooltipSize.height
    val anchorX = anchor.x.roundToInt()
    val anchorY = anchor.y.roundToInt()
    val candidates = listOf(
        IntOffset(x = anchorX - width / 2, y = anchorY - height - marginPx),
        IntOffset(x = anchorX - width / 2, y = anchorY + marginPx),
        IntOffset(x = anchorX + marginPx, y = anchorY - height / 2),
        IntOffset(x = anchorX - width - marginPx, y = anchorY - height / 2),
    )
    val maxX = max(0, canvasSize.width - width)
    val maxY = max(0, canvasSize.height - height)
    val chosen = candidates.firstOrNull { it.x in 0..maxX && it.y in 0..maxY }
        ?: candidates.minBy {
            abs(it.x.coerceIn(0, maxX) - it.x) + abs(it.y.coerceIn(0, maxY) - it.y)
        }
    return IntOffset(x = chosen.x.coerceIn(0, maxX), y = chosen.y.coerceIn(0, maxY))
}

private fun formatValue(value: Float, decimals: Int): String {
    val d = decimals.coerceIn(0, 6)
    if (d == 0) return value.roundToInt().toString()

    val p = pow10(d)
    val rounded = (value * p).roundToInt().toFloat() / p

    val s = rounded.toString()
    val dot = s.indexOf('.')
    if (dot < 0) return s

    var end = s.length
    while (end > dot + 1 && s[end - 1] == '0') end--
    if (end == dot + 1) end--
    return s.take(end)
}

private fun pow10(n: Int): Float {
    var r = 1f
    repeat(n) { r *= 10f }
    return r
}

private fun interpolateOffsets(
    from: List<Offset>,
    to: List<Offset>,
    progress: Float,
): List<Offset> {
    if (to.isEmpty()) return emptyList()
    if (from.isEmpty()) return to

    val t = progress.coerceIn(0f, 1f)
    return List(to.size) { i ->
        val start = if (i < from.size) from[i] else from.last()
        lerpOffset(start, to[i], t)
    }
}

private fun lerpOffset(from: Offset, to: Offset, fraction: Float): Offset {
    val t = fraction.coerceIn(0f, 1f)
    return Offset(
        x = from.x + (to.x - from.x) * t,
        y = from.y + (to.y - from.y) * t
    )
}

private fun buildTransitionFromOffsets(
    currentLayout: SparklineLayout,
    previousLayout: SparklineLayout?,
    previousPoints: List<SparklinePoint>,
    currentPoints: List<SparklinePoint>,
): List<Offset> {
    if (previousLayout == null || previousPoints.isEmpty() || currentPoints.isEmpty()) {
        return emptyList()
    }

    val previousSorted = previousPoints.sortedBy { it.x }
    val currentSorted = currentPoints.sortedBy { it.x }

    if (isAppendTransition(previousSorted, currentSorted)) {
        val previousOffsets = previousLayout.currentPoints.map { it.offset }
        if (previousOffsets.isNotEmpty()) {
            return List(currentSorted.size) { index ->
                if (index < previousOffsets.size) {
                    previousOffsets[index]
                } else {
                    previousOffsets.last()
                }
            }
        }
    }

    if (previousSorted.size == 1) {
        val y = previousSorted.first().y
        return currentSorted.map { point ->
            Offset(
                x = currentLayout.mx(point.x),
                y = currentLayout.my(y).coerceIn(currentLayout.chart.top, currentLayout.chart.bottom),
            )
        }
    }

    return currentSorted.map { point ->
        val py = samplePreviousY(previousSorted, point.x)
        Offset(
            x = currentLayout.mx(point.x),
            y = currentLayout.my(py).coerceIn(currentLayout.chart.top, currentLayout.chart.bottom),
        )
    }
}

private fun isAppendTransition(
    previousSorted: List<SparklinePoint>,
    currentSorted: List<SparklinePoint>,
): Boolean {
    if (previousSorted.isEmpty()) return false
    if (currentSorted.size != previousSorted.size + 1) return false

    return previousSorted.indices.all { index ->
        val prev = previousSorted[index]
        val curr = currentSorted[index]
        prev.x == curr.x && prev.y == curr.y && prev.label == curr.label
    }
}

private fun samplePreviousY(points: List<SparklinePoint>, x: Float): Float {
    val first = points.first()
    val last = points.last()

    if (x <= first.x) return first.y
    if (x >= last.x) return last.y

    val rightIndex = points.indexOfFirst { it.x >= x }
    if (rightIndex <= 0) return first.y

    val left = points[rightIndex - 1]
    val right = points[rightIndex]
    val span = right.x - left.x
    if (span <= 1e-6f) return right.y

    val t = ((x - left.x) / span).coerceIn(0f, 1f)
    return left.y + (right.y - left.y) * t
}
