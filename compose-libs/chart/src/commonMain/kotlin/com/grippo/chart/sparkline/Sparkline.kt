package com.grippo.chart.sparkline

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
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
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.abs
import kotlin.math.ceil
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
    var selectedIndex by remember(data.points) { mutableStateOf<Int?>(null) }
    var settledPoints by remember { mutableStateOf<List<SparklinePoint>>(emptyList()) }
    val previousPoints = settledPoints
    val drawProgress = remember(sortedCurrentPoints) { Animatable(0f) }

    LaunchedEffect(sortedCurrentPoints) {
        drawProgress.animateTo(
            targetValue = 1f,
            animationSpec = tween(
                durationMillis = 520,
                easing = FastOutSlowInEasing
            )
        )
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

    val peek = style.peek

    val pointerModifier = if (peek is SparklineStyle.Peek.Visible) {
        Modifier.pointerInput(layout, peek, style.dots, data.points, selectedIndex) {
            detectTapGestures { pos ->
                val l = layout ?: return@detectTapGestures
                if (l.currentPoints.isEmpty()) return@detectTapGestures

                val baseHit = with(density) { peek.hitSlop.toPx() }
                val extraBase = with(density) { 6.dp.toPx() }

                val dotR = when (val d = style.dots) {
                    is SparklineStyle.Dots.None -> 0f
                    is SparklineStyle.Dots.Visible -> with(density) { d.radius.toPx() }
                }

                val focusR = with(density) { peek.focusRadius.toPx() }

                val threshold = baseHit + extraBase + max(dotR, focusR)

                val nearest = l.currentPoints
                    .withIndex()
                    .minByOrNull { (it.value.offset - pos).getDistance() }

                if (nearest == null) {
                    selectedIndex = null
                    return@detectTapGestures
                }

                val dist = (nearest.value.offset - pos).getDistance()
                if (dist > threshold) {
                    selectedIndex = null
                    return@detectTapGestures
                }

                selectedIndex = if (selectedIndex == nearest.index) null else nearest.index
            }
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

                    val p = Path().apply {
                        moveTo(chart.left, y)
                        lineTo(chart.right, y)
                    }

                    drawPath(
                        path = p,
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

            val currentOffsets = current.map { it.offset }
            val progress = drawProgress.value
            val revealAlpha = if (previousPoints.isEmpty()) progress else 1f
            val animatedOffsets = interpolateOffsets(
                from = transitionFromOffsets,
                to = currentOffsets,
                progress = progress
            )

            if (current.size == 1) {
                val c = animatedOffsets.first()

                drawSinglePointHero(
                    center = c,
                    color = style.line.color.copy(alpha = revealAlpha),
                    strokePx = style.line.stroke.toPx(),
                    chart = chart
                )

                if (peek is SparklineStyle.Peek.Visible && selectedIndex == 0) {
                    drawPeekOverlay(
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

            val currentPath = buildLinePath(
                points = animatedOffsets,
                curved = style.line.curved,
                smoothness = style.line.curveSmoothness,
                clampOvershoot = style.line.clampOvershoot
            )

            style.fill?.provider?.let { provider ->
                val area = Path().apply {
                    addPath(currentPath)
                    lineTo(animatedOffsets.last().x, chart.bottom)
                    lineTo(animatedOffsets.first().x, chart.bottom)
                    close()
                }
                drawPath(area, brush = provider(chart), alpha = revealAlpha)
            }

            val sw = style.line.stroke.toPx()
            style.line.brush?.let { provider ->
                drawPath(
                    path = currentPath,
                    brush = provider(chart),
                    alpha = revealAlpha,
                    style = Stroke(width = sw, cap = StrokeCap.Round)
                )
            } ?: run {
                drawPath(
                    path = currentPath,
                    color = style.line.color.copy(alpha = revealAlpha),
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
                            color = dc.copy(alpha = revealAlpha),
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
                                color = ex.maxColor.copy(alpha = revealAlpha),
                                radius = rr,
                                center = animatedOffsets[maxIdx]
                            )
                        } else {
                            drawCircle(
                                color = ex.minColor.copy(alpha = revealAlpha),
                                radius = rr,
                                center = animatedOffsets[minIdx]
                            )
                            drawCircle(
                                color = ex.maxColor.copy(alpha = revealAlpha),
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
                val valueText = formatValue(mp.raw.y, peek.decimals)
                val labelText = if (peek.showLabel) mp.raw.label else null

                val tooltipSize = remember(mp.raw.y, mp.raw.label, peek, density) {
                    measureTooltipSize(
                        textMeasurer = textMeasurer,
                        density = density,
                        valueText = valueText,
                        labelText = labelText,
                        padH = peek.tooltipPaddingH,
                        padV = peek.tooltipPaddingV
                    )
                }

                SparklineTooltip(
                    anchor = mp.offset,
                    canvasSize = sizePx,
                    tooltipSize = tooltipSize,
                    margin = peek.tooltipMargin,
                    background = peek.tooltipBackground,
                    border = peek.tooltipBorder,
                    textColor = peek.tooltipText,
                    corner = peek.tooltipCornerRadius,
                    padH = peek.tooltipPaddingH,
                    padV = peek.tooltipPaddingV,
                    valueText = valueText,
                    labelText = labelText
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
    val minX: Float,
    val maxX: Float,
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
    if (data.points.isEmpty()) return null

    val dotsPad = when (val d = style.dots) {
        is SparklineStyle.Dots.None -> 0f
        is SparklineStyle.Dots.Visible -> with(density) { d.radius.toPx() }
    }

    val extremesPad = when (val ex = style.extremes) {
        is SparklineStyle.Extremes.None -> 0f
        is SparklineStyle.Extremes.Visible -> with(density) { ex.radius.toPx() }
    }

    val padding = max(dotsPad, extremesPad)

    val chart = Rect(
        left = padding,
        top = padding,
        right = sizePx.width.toFloat() - padding,
        bottom = sizePx.height.toFloat() - padding
    )

    if (chart.width <= 0f || chart.height <= 0f) return null

    val all = data.points

    val minX = all.minOf { it.x }
    val maxX = all.maxOf { it.x }
    val minY = all.minOf { it.y }
    val maxY = all.maxOf { it.y }

    val spanXRaw = maxX - minX
    val spanYRaw = maxY - minY
    val flatX = spanXRaw <= 1e-6f
    val flatY = spanYRaw <= 1e-6f
    val spanX = if (flatX) 1f else spanXRaw
    val spanY = if (flatY) 1f else spanYRaw

    fun mx(x: Float): Float {
        return if (flatX) chart.left + chart.width / 2f
        else chart.left + (x - minX) / spanX * chart.width
    }

    fun my(y: Float): Float {
        return if (flatY) chart.top + chart.height / 2f
        else chart.bottom - (y - minY) / spanY * chart.height
    }

    val currentSorted = data.points.sortedBy { it.x }
    val currentMapped = currentSorted.map { MappedPoint(it, Offset(mx(it.x), my(it.y))) }

    return SparklineLayout(
        chart = chart,
        currentPoints = currentMapped,
        minX = minX,
        maxX = maxX,
        minY = minY,
        maxY = maxY,
        mx = ::mx,
        my = ::my,
    )
}

private fun buildLinePath(
    points: List<Offset>,
    curved: Boolean,
    smoothness: Float,
    clampOvershoot: Boolean,
): Path {
    val path = Path()
    if (points.isEmpty()) return path

    if (!curved || points.size < 3) {
        path.moveTo(points.first().x, points.first().y)
        for (i in 1 until points.size) path.lineTo(points[i].x, points[i].y)
        return path
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
            val minYp = min(p0.y, min(p1.y, p2.y))
            val maxYp = max(p0.y, max(p1.y, p2.y))
            c1 = c1.copy(y = c1.y.coerceIn(minYp, maxYp))

            val minYn = min(p1.y, min(p2.y, p3.y))
            val maxYn = max(p1.y, max(p2.y, p3.y))
            c2 = c2.copy(y = c2.y.coerceIn(minYn, maxYn))
        }

        path.cubicTo(c1.x, c1.y, c2.x, c2.y, p2.x, p2.y)
    }

    return path
}

private fun DrawScope.drawPeekOverlay(
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
    val guide = Path().apply {
        moveTo(point.x, chart.top)
        lineTo(point.x, chart.bottom)
    }

    drawPath(
        path = guide,
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

    val halo = Brush.radialGradient(
        colors = listOf(
            focusColor.copy(alpha = 0.20f),
            focusColor.copy(alpha = 0f)
        ),
        center = point,
        radius = focusHaloRadiusPx.coerceAtLeast(focusRadiusPx * 2f)
    )

    drawCircle(brush = halo, radius = focusHaloRadiusPx, center = point)
    drawCircle(
        color = focusColor.copy(alpha = 0.48f),
        radius = (focusRadiusPx * 1.9f).coerceAtLeast(focusRadiusPx + 1f),
        center = point,
        style = Stroke(width = focusRingWidthPx.coerceAtLeast(1f), cap = StrokeCap.Round)
    )
    drawCircle(color = focusColor, radius = focusRadiusPx.coerceAtLeast(1f), center = point)
}

private fun DrawScope.drawSinglePointHero(
    center: Offset,
    color: androidx.compose.ui.graphics.Color,
    strokePx: Float,
    chart: Rect,
) {
    val sw = strokePx.coerceAtLeast(1f)
    val inset = chart.width * 0.18f

    drawLine(
        color = color.copy(alpha = 0.42f),
        start = Offset(chart.left + inset, center.y),
        end = Offset(chart.right - inset, center.y),
        strokeWidth = sw,
        cap = StrokeCap.Round
    )

    drawLine(
        color = color.copy(alpha = 0.18f),
        start = Offset(center.x, center.y),
        end = Offset(center.x, chart.bottom),
        strokeWidth = max(sw * 0.9f, 1f),
        cap = StrokeCap.Round
    )

    drawCircle(
        color = color.copy(alpha = 0.16f),
        radius = max(sw * 3.3f, min(chart.width, chart.height) * 0.10f),
        center = center
    )

    drawCircle(
        color = color.copy(alpha = 0.35f),
        radius = max(sw * 2.2f, min(chart.width, chart.height) * 0.07f),
        center = center,
        style = Stroke(width = max(sw * 0.85f, 1f), cap = StrokeCap.Round)
    )

    drawCircle(
        color = color,
        radius = max(sw * 1.2f, min(chart.width, chart.height) * 0.03f),
        center = center
    )
}

@Composable
private fun SparklineTooltip(
    anchor: Offset,
    canvasSize: IntSize,
    tooltipSize: IntSize,
    margin: Dp,
    background: androidx.compose.ui.graphics.Color,
    border: androidx.compose.ui.graphics.Color,
    textColor: androidx.compose.ui.graphics.Color,
    corner: Dp,
    padH: Dp,
    padV: Dp,
    valueText: String,
    labelText: String?,
) {
    val density = LocalDensity.current
    val marginPx = with(density) { margin.toPx() }.roundToInt()

    val w = tooltipSize.width
    val h = tooltipSize.height
    if (w <= 0 || h <= 0 || canvasSize.width <= 0 || canvasSize.height <= 0) return

    val ax = anchor.x.roundToInt()
    val ay = anchor.y.roundToInt()

    fun clampX(x: Int): Int = x.coerceIn(0, max(0, canvasSize.width - w))
    fun clampY(y: Int): Int = y.coerceIn(0, max(0, canvasSize.height - h))

    fun fits(x: Int, y: Int): Boolean {
        return x >= 0 && y >= 0 && x + w <= canvasSize.width && y + h <= canvasSize.height
    }

    data class Candidate(val x: Int, val y: Int)

    val top = Candidate(
        x = ax - w / 2,
        y = ay - h - marginPx
    )
    val bottom = Candidate(
        x = ax - w / 2,
        y = ay + marginPx
    )
    val right = Candidate(
        x = ax + marginPx,
        y = ay - h / 2
    )
    val left = Candidate(
        x = ax - w - marginPx,
        y = ay - h / 2
    )

    val candidates = listOf(top, bottom, right, left)

    val chosen = candidates.firstOrNull { fits(it.x, it.y) } ?: run {
        candidates.minBy { c ->
            val cx = clampX(c.x)
            val cy = clampY(c.y)
            abs(cx - c.x) + abs(cy - c.y)
        }
    }

    val x = clampX(chosen.x)
    val y = clampY(chosen.y)

    Box(
        modifier = Modifier
            .offset { IntOffset(x, y) }
            .background(
                background,
                shape = androidx.compose.foundation.shape.RoundedCornerShape(corner)
            )
            .border(
                1.dp,
                border,
                shape = androidx.compose.foundation.shape.RoundedCornerShape(corner)
            )
            .padding(horizontal = padH, vertical = padV)
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            BasicText(
                text = valueText,
                style = TextStyle(
                    color = textColor,
                    fontSize = 12.sp
                )
            )
            if (!labelText.isNullOrBlank()) {
                BasicText(
                    text = labelText,
                    style = TextStyle(
                        color = textColor.copy(alpha = 0.80f),
                        fontSize = 10.sp
                    )
                )
            }
        }
    }
}

private fun measureTooltipSize(
    textMeasurer: androidx.compose.ui.text.TextMeasurer,
    density: androidx.compose.ui.unit.Density,
    valueText: String,
    labelText: String?,
    padH: Dp,
    padV: Dp,
): IntSize {
    val value = textMeasurer.measure(
        text = valueText,
        style = TextStyle(fontSize = 12.sp)
    ).size

    val label = if (!labelText.isNullOrBlank()) {
        textMeasurer.measure(
            text = labelText,
            style = TextStyle(fontSize = 10.sp)
        ).size
    } else null

    val padHPx = with(density) { padH.toPx() }
    val padVPx = with(density) { padV.toPx() }
    val gapPx = if (label != null) with(density) { 2.dp.toPx() } else 0f

    val contentW = max(value.width, label?.width ?: 0)
    val contentH = value.height + (label?.height ?: 0) + gapPx.roundToInt()

    val w = ceil(contentW + padHPx * 2f + 2f).toInt()
    val h = ceil(contentH + padVPx * 2f + 2f).toInt()

    return IntSize(w, h)
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
    if (previousPoints.isEmpty() || currentPoints.isEmpty()) return emptyList()

    val previousSorted = previousPoints.sortedBy { it.x }
    val currentSorted = currentPoints.sortedBy { it.x }

    if (isAppendTransition(previousSorted, currentSorted)) {
        val previousOffsets = previousLayout?.currentPoints?.map { it.offset }.orEmpty()
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
            Offset(x = currentLayout.mx(point.x), y = currentLayout.my(y))
        }
    }

    return currentSorted.map { point ->
        val py = samplePreviousY(previousSorted, point.x)
        Offset(x = currentLayout.mx(point.x), y = currentLayout.my(py))
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
