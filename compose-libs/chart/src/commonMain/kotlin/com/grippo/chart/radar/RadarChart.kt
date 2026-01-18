package com.grippo.chart.radar

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.round
import kotlin.math.roundToInt
import kotlin.math.sin

@Composable
public fun RadarChart(
    modifier: Modifier = Modifier,
    data: RadarData,
    style: RadarStyle,
) {
    val measurer = rememberTextMeasurer()
    val density = LocalDensity.current

    var canvasSize by remember { mutableStateOf(IntSize.Zero) }
    var selected by remember { mutableStateOf<RadarHit?>(null) }

    val n = data.axes.size

    LaunchedEffect(data.axes.size, data.series.size) {
        val sel = selected ?: return@LaunchedEffect
        if (sel.seriesIdx !in data.series.indices || sel.axisIdx !in 0 until data.axes.size) {
            selected = null
        }
    }

    val axisLabelLayouts: List<TextLayoutResult> = when (val lbl = style.labels) {
        is RadarStyle.Labels.None -> emptyList()
        is RadarStyle.Labels.Visible -> data.axes.map { axis ->
            measurer.measure(AnnotatedString(axis.label), lbl.textStyle)
        }
    }

    val levels = max(1, style.grid.levels)
    val levelLabelLayouts: List<TextLayoutResult> = when (val ll = style.grid.levelLabels) {
        is RadarStyle.LevelLabels.None -> emptyList()
        is RadarStyle.LevelLabels.Visible -> (1..levels).map { idx ->
            val t = idx / levels.toFloat()
            val text = ll.levelFormatter(t)
            measurer.measure(AnnotatedString(text), ll.levelLabelStyle)
        }
    }

    val layout by remember(
        canvasSize,
        data,
        style,
        density.density,
        density.fontScale,
        axisLabelLayouts,
        levelLabelLayouts
    ) {
        derivedStateOf {
            if (canvasSize.width <= 0 || canvasSize.height <= 0 || n <= 0) {
                null
            } else {
                computeRadarLayout(
                    canvasSize = canvasSize,
                    data = data,
                    style = style,
                    density = density,
                    axisLabelLayouts = axisLabelLayouts
                )
            }
        }
    }

    val peek = style.peek
    val hitSlopPx = remember(peek, density) {
        if (peek is RadarStyle.Peek.Visible) with(density) { peek.hitSlop.toPx() } else 0f
    }

    Box(
        modifier = modifier
            .onSizeChanged { canvasSize = it }
            .then(
                if (peek is RadarStyle.Peek.Visible) {
                    Modifier.pointerInput(data, style, canvasSize, hitSlopPx) {
                        detectTapGestures { pos ->
                            val l = layout ?: return@detectTapGestures
                            val hit = findHitVertex(
                                series = l.series,
                                tap = pos,
                                hitSlopPx = hitSlopPx
                            )

                            selected = when {
                                hit == null -> null
                                selected == hit -> null
                                else -> hit
                            }
                        }
                    }
                } else {
                    Modifier
                }
            )
    ) {
        Canvas(Modifier.matchParentSize()) {
            val l = layout ?: return@Canvas
            drawRadar(
                layout = l,
                data = data,
                style = style,
                measurer = measurer,
                axisLabelLayouts = axisLabelLayouts,
                levelLabelLayouts = levelLabelLayouts,
                selected = selected
            )
        }

        if (peek is RadarStyle.Peek.Visible) {
            val l = layout
            val sel = selected
            if (l != null && sel != null) {
                val sl = l.series.getOrNull(sel.seriesIdx)
                val v = sl?.vertices?.firstOrNull { it.axisIdx == sel.axisIdx }
                if (sl != null && v != null) {
                    val series = data.series[sel.seriesIdx]
                    val axis = data.axes[sel.axisIdx]
                    val rawValue = sl.rawValues.getOrNull(sel.axisIdx)
                    if (rawValue != null && !rawValue.isNaN()) {
                        val valueText = peek.valueFormatter?.invoke(rawValue, data)
                            ?: when (val vCfg = style.values) {
                                is RadarStyle.Values.Visible -> vCfg.formatter(rawValue, data)
                                is RadarStyle.Values.None -> format01(rawValue, peek.decimals)
                            }

                        val labelText = buildString {
                            val parts = mutableListOf<String>()
                            if (peek.showAxisLabel) parts.add(axis.label)
                            if (peek.showSeriesName) parts.add(series.name)
                            append(parts.joinToString(" • "))
                        }.ifBlank { null }

                        val tooltipSize = remember(
                            valueText,
                            labelText,
                            peek,
                            density
                        ) {
                            measureTooltipSize(
                                textMeasurer = measurer,
                                density = density,
                                valueText = valueText,
                                labelText = labelText,
                                padH = peek.tooltipPaddingH,
                                padV = peek.tooltipPaddingV
                            )
                        }

                        RadarChartTooltip(
                            anchor = v.point,
                            canvasSize = canvasSize,
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
    }
}

private data class RadarHit(
    val seriesIdx: Int,
    val axisIdx: Int,
)

private data class RadarVertexLayout(
    val axisIdx: Int,
    val value: Float,
    val point: Offset,
)

private data class RadarSeriesLayout(
    val path: Path,
    val vertices: List<RadarVertexLayout>,
    val rawValues: FloatArray,
)

private data class RadarChartLayoutResult(
    val chart: Rect,
    val center: Offset,
    val radius: Float,
    val angles: FloatArray,
    val series: List<RadarSeriesLayout?>,
)

private fun computeRadarLayout(
    canvasSize: IntSize,
    data: RadarData,
    style: RadarStyle,
    density: Density,
    axisLabelLayouts: List<TextLayoutResult>,
): RadarChartLayoutResult {
    val w = canvasSize.width.toFloat()
    val h = canvasSize.height.toFloat()
    val chart = Rect(0f, 0f, w, h)
    val center = Offset(w * 0.5f, h * 0.5f)

    val n = data.axes.size
    val dir = if (style.layout.clockwise) 1f else -1f
    val angleStep = (2f * PI.toFloat() / n) * dir
    val startRad = style.layout.startAngleDeg * PI.toFloat() / 180f

    val angles = FloatArray(n) { i -> startRad + i * angleStep }

    fun polar(angle: Float, radius: Float): Offset = Offset(
        x = center.x + radius * cos(angle),
        y = center.y + radius * sin(angle)
    )

    val labelPadPx = with(density) { style.layout.labelPadding.toPx() }

    var r = min(chart.width, chart.height) * 0.5f * 0.95f
    if (axisLabelLayouts.isNotEmpty()) {
        var iter = 0
        while (iter < 10) {
            var maxOverflow = 0f
            for (i in 0 until n) {
                val a = angles[i]
                val p = polar(a, r + labelPadPx)
                val layout = axisLabelLayouts[i]
                val halfW = layout.size.width * 0.5f
                val halfH = layout.size.height * 0.5f

                val left = p.x - halfW
                val right = p.x + halfW
                val top = p.y - halfH
                val bottom = p.y + halfH

                val overflowLeft = max(0f, chart.left - left)
                val overflowRight = max(0f, right - chart.right)
                val overflowTop = max(0f, chart.top - top)
                val overflowBottom = max(0f, bottom - chart.bottom)

                maxOverflow = max(
                    maxOverflow,
                    max(max(overflowLeft, overflowRight), max(overflowTop, overflowBottom))
                )
            }

            if (maxOverflow <= 0f) break
            r -= maxOverflow
            if (r <= 0f) {
                r = 0f
                break
            }
            iter++
        }
    }

    val seriesLayouts: List<RadarSeriesLayout?> = data.series.map { s ->
        val raw = FloatArray(n) { Float.NaN }
        var hasMissing = false

        for (i in 0 until n) {
            val v: Float? = when (val vv = s.values) {
                is RadarValues.ByAxisId -> vv.map[data.axes[i].id]
                is RadarValues.ByIndex -> vv.list.getOrNull(i)
            }
            if (v == null) {
                hasMissing = true
                if (style.dataPolicy.missingAsZero) raw[i] = 0f
            } else {
                raw[i] = v.coerceIn(0f, 1f)
            }
        }

        if (style.dataPolicy.requireCompleteSeries && hasMissing && !style.dataPolicy.missingAsZero) {
            return@map null
        }

        val path = Path()
        for (i in 0 until n) {
            val vv = raw[i]
            val rr = if (vv.isNaN()) 0f else vv
            val p = polar(angles[i], r * rr)
            if (i == 0) path.moveTo(p.x, p.y) else path.lineTo(p.x, p.y)
        }
        path.close()

        val vertices = buildList {
            for (i in 0 until n) {
                val vv = raw[i]
                if (vv.isNaN() && !style.dataPolicy.missingAsZero) continue
                val point = polar(angles[i], r * (if (vv.isNaN()) 0f else vv))
                add(
                    RadarVertexLayout(
                        axisIdx = i,
                        value = if (vv.isNaN()) 0f else vv,
                        point = point
                    )
                )
            }
        }

        RadarSeriesLayout(
            path = path,
            vertices = vertices,
            rawValues = raw
        )
    }

    return RadarChartLayoutResult(
        chart = chart,
        center = center,
        radius = r,
        angles = angles,
        series = seriesLayouts
    )
}

private fun DrawScope.drawRadar(
    layout: RadarChartLayoutResult,
    data: RadarData,
    style: RadarStyle,
    measurer: TextMeasurer,
    axisLabelLayouts: List<TextLayoutResult>,
    levelLabelLayouts: List<TextLayoutResult>,
    selected: RadarHit?,
) {
    val chart = layout.chart
    val c = layout.center
    val r = layout.radius
    val n = data.axes.size
    if (n <= 0) return

    fun polar(angle: Float, radius: Float): Offset = Offset(
        x = c.x + radius * cos(angle),
        y = c.y + radius * sin(angle)
    )

    val levels = max(1, style.grid.levels)
    val gridStroke = Stroke(width = style.grid.strokeWidth.toPx(), cap = StrokeCap.Round)

    for (lvl in 1..levels) {
        val t = lvl / levels.toFloat()
        val rr = r * t

        if (style.grid.asPolygon) {
            val p = Path()
            for (i in 0 until n) {
                val pt = polar(layout.angles[i], rr)
                if (i == 0) p.moveTo(pt.x, pt.y) else p.lineTo(pt.x, pt.y)
            }
            p.close()
            drawPath(p, color = style.grid.color, style = gridStroke)
        } else {
            drawCircle(color = style.grid.color, radius = rr, center = c, style = gridStroke)
        }

        when (val ll = style.grid.levelLabels) {
            is RadarStyle.LevelLabels.None -> Unit
            is RadarStyle.LevelLabels.Visible -> {
                val layoutText = levelLabelLayouts.getOrNull(lvl - 1)
                if (layoutText != null) {
                    val y =
                        (c.y - rr - layoutText.size.height - 4.dp.toPx()).coerceAtLeast(chart.top)
                    val x = (c.x - layoutText.size.width * 0.5f)
                        .coerceIn(chart.left, chart.right - layoutText.size.width.toFloat())
                    drawText(layoutText, topLeft = Offset(x, y))
                }
            }
        }
    }

    when (val sp = style.spokes) {
        is RadarStyle.Spokes.None -> Unit
        is RadarStyle.Spokes.Visible -> {
            val sw = sp.strokeWidth.toPx()
            for (i in 0 until n) {
                val p = polar(layout.angles[i], r)
                drawLine(sp.color, c, p, sw)
            }
        }
    }

    val strokeW = style.polygon.strokeWidth.toPx().coerceAtLeast(0f)
    val gradientStops = style.colorStops.sortedBy { it.first }

    data.series.forEachIndexed { sIdx, s ->
        val sl = layout.series.getOrNull(sIdx) ?: return@forEachIndexed
        val sweepStops = if (gradientStops.isNotEmpty()) {
            buildSweepStops(
                angles = layout.angles,
                rawValues = sl.rawValues,
                stops = gradientStops,
            )
        } else {
            emptyList()
        }

        val fillAlpha = style.polygon.fillAlpha.coerceIn(0f, 1f)
        val fill = s.color.copy(alpha = fillAlpha)
        if (style.polygon.fillAlpha > 0f) {
            if (sweepStops.isNotEmpty()) {
                val fillBrush = Brush.sweepGradient(
                    colorStops = sweepStops.map { (pos, color) ->
                        pos to color.copy(alpha = fillAlpha)
                    }.toTypedArray(),
                    center = c,
                )
                drawPath(sl.path, brush = fillBrush)
            } else {
                drawPath(sl.path, color = fill)
            }
        }

        val stroke = s.color
        if (strokeW > 0f) {
            if (sweepStops.isNotEmpty()) {
                val strokeBrush = Brush.sweepGradient(
                    colorStops = sweepStops.toTypedArray(),
                    center = c,
                )
                drawPath(
                    sl.path,
                    brush = strokeBrush,
                    style = Stroke(
                        width = strokeW.coerceAtLeast(1f),
                        cap = StrokeCap.Round,
                        join = StrokeJoin.Round
                    )
                )
            } else {
                drawPath(
                    sl.path,
                    color = stroke,
                    style = Stroke(
                        width = strokeW.coerceAtLeast(1f),
                        cap = StrokeCap.Round,
                        join = StrokeJoin.Round
                    )
                )
            }
        }

        when (val vc = style.vertices) {
            is RadarStyle.Vertices.None -> Unit
            is RadarStyle.Vertices.Visible -> {
                val vr = vc.radius.toPx().coerceAtLeast(0f)
                sl.vertices.forEach { v ->
                    val col = vc.colorOverride ?: run {
                        if (gradientStops.isNotEmpty()) colorFromStops(gradientStops, v.value)
                        else s.color
                    }
                    drawCircle(color = col, radius = vr, center = v.point)
                }
            }
        }

        when (val vCfg = style.values) {
            is RadarStyle.Values.None -> Unit
            is RadarStyle.Values.Visible -> {
                val off = vCfg.offset.toPx()
                for (i in 0 until n) {
                    val raw = sl.rawValues.getOrNull(i) ?: Float.NaN
                    if (raw.isNaN() && !style.dataPolicy.missingAsZero) continue
                    val vv = if (raw.isNaN()) 0f else raw
                    val p0 = polar(layout.angles[i], r * vv)
                    val label = vCfg.formatter(vv, data)
                    val textLayout = measurer.measure(AnnotatedString(label), vCfg.textStyle)

                    val a = layout.angles[i]
                    val x = (p0.x + cos(a) * off - textLayout.size.width * 0.5f)
                        .coerceIn(chart.left, chart.right - textLayout.size.width.toFloat())
                    val y = (p0.y + sin(a) * off - textLayout.size.height * 0.5f)
                        .coerceIn(chart.top, chart.bottom - textLayout.size.height.toFloat())

                    drawText(textLayout, topLeft = Offset(x, y))
                }
            }
        }
    }

    if (selected != null && style.peek is RadarStyle.Peek.Visible) {
        val selSeries = layout.series.getOrNull(selected.seriesIdx)
        val vertex = selSeries?.vertices?.firstOrNull { it.axisIdx == selected.axisIdx }
        if (selSeries != null && vertex != null) {
            val series = data.series[selected.seriesIdx]
            val peek = style.peek
            val focus = (peek.focusColor ?: series.color)

            val guideW = peek.guideWidth.toPx()
            val dash = peek.guideDash.toPx()
            val gap = peek.guideGap.toPx()

            val axisAngle = layout.angles[selected.axisIdx]
            val axisEnd = polar(axisAngle, r)

            val guidePath = Path().apply {
                moveTo(c.x, c.y)
                lineTo(axisEnd.x, axisEnd.y)
            }

            drawPath(
                path = guidePath,
                color = peek.guideColor,
                style = Stroke(
                    width = guideW.coerceAtLeast(1f),
                    cap = StrokeCap.Round,
                    pathEffect = PathEffect.dashPathEffect(
                        intervals = floatArrayOf(
                            dash.coerceAtLeast(1f),
                            gap.coerceAtLeast(1f)
                        ),
                        phase = 0f
                    )
                )
            )

            val overlayAlpha = peek.selectedPolygonFillOverlayAlpha.coerceIn(0f, 1f)
            if (overlayAlpha > 0f) {
                drawPath(
                    selSeries.path,
                    color = focus.copy(alpha = overlayAlpha)
                )
            }

            val selStrokeW = peek.selectedPolygonStrokeWidth.toPx()
            if (selStrokeW > 0f) {
                drawPath(
                    selSeries.path,
                    color = focus.copy(alpha = 0.95f),
                    style = Stroke(
                        width = selStrokeW.coerceAtLeast(1f),
                        cap = StrokeCap.Round,
                        join = StrokeJoin.Round
                    )
                )
            }

            val focusRadiusPx = peek.focusRadius.toPx()
            val ringW = peek.focusRingWidth.toPx()
            val haloR = peek.focusHaloRadius.toPx()

            val halo = androidx.compose.ui.graphics.Brush.radialGradient(
                colors = listOf(
                    focus.copy(alpha = 0.20f),
                    focus.copy(alpha = 0f)
                ),
                center = vertex.point,
                radius = haloR.coerceAtLeast(focusRadiusPx * 2f)
            )

            drawCircle(brush = halo, radius = haloR, center = vertex.point)
            drawCircle(
                color = focus.copy(alpha = 0.48f),
                radius = (focusRadiusPx * 1.9f).coerceAtLeast(focusRadiusPx + 1f),
                center = vertex.point,
                style = Stroke(width = ringW.coerceAtLeast(1f), cap = StrokeCap.Round)
            )
            drawCircle(
                color = focus,
                radius = focusRadiusPx.coerceAtLeast(1f),
                center = vertex.point
            )
        }
    }

    when (val lbl = style.labels) {
        is RadarStyle.Labels.None -> Unit
        is RadarStyle.Labels.Visible -> {
            val lp = style.layout.labelPadding.toPx()
            for (i in 0 until n) {
                val a = layout.angles[i]
                val p = polar(a, r + lp)
                val textLayout = axisLabelLayouts[i]
                val x = (p.x - textLayout.size.width * 0.5f)
                    .coerceIn(chart.left, chart.right - textLayout.size.width.toFloat())
                val y = (p.y - textLayout.size.height * 0.5f)
                    .coerceIn(chart.top, chart.bottom - textLayout.size.height.toFloat())
                drawText(textLayout, topLeft = Offset(x, y))
            }
        }
    }
}

private fun findHitVertex(
    series: List<RadarSeriesLayout?>,
    tap: Offset,
    hitSlopPx: Float
): RadarHit? {
    if (series.isEmpty()) return null
    if (hitSlopPx <= 0f) return null

    val maxDist2 = hitSlopPx * hitSlopPx
    var best: RadarHit? = null
    var bestD2 = Float.POSITIVE_INFINITY

    for (sIdx in series.indices) {
        val sl = series[sIdx] ?: continue
        for (v in sl.vertices) {
            val dx = tap.x - v.point.x
            val dy = tap.y - v.point.y
            val d2 = dx * dx + dy * dy
            if (d2 <= maxDist2 && d2 < bestD2) {
                bestD2 = d2
                best = RadarHit(seriesIdx = sIdx, axisIdx = v.axisIdx)
            }
        }
    }

    return best
}

private fun lerpColor(a: Color, b: Color, t: Float): Color = Color(
    red = a.red + (b.red - a.red) * t,
    green = a.green + (b.green - a.green) * t,
    blue = a.blue + (b.blue - a.blue) * t,
    alpha = a.alpha + (b.alpha - a.alpha) * t
)

private fun colorFromStops(stops: List<Pair<Float, Color>>, t: Float): Color {
    if (stops.isEmpty()) return Color.Unspecified
    val clamped = t.coerceIn(0f, 1f)

    var prevPos = stops.first().first
    var prevColor = stops.first().second

    for (i in 1 until stops.size) {
        val pos = stops[i].first
        val col = stops[i].second
        if (clamped <= pos) {
            val localT = if (pos == prevPos) 0f else (clamped - prevPos) / (pos - prevPos)
            return lerpColor(prevColor, col, localT)
        }
        prevPos = pos
        prevColor = col
    }

    return stops.last().second
}

private fun buildSweepStops(
    angles: FloatArray,
    rawValues: FloatArray,
    stops: List<Pair<Float, Color>>,
): List<Pair<Float, Color>> {
    if (angles.isEmpty()) return emptyList()
    val twoPi = 2f * PI.toFloat()

    val byAngle = buildList {
        for (i in angles.indices) {
            val angle = angles[i]
            val fraction = ((angle % twoPi) + twoPi) % twoPi / twoPi
            val raw = rawValues.getOrNull(i) ?: Float.NaN
            val value = if (raw.isNaN()) 0f else raw
            add(fraction to colorFromStops(stops, value))
        }
    }.sortedBy { it.first }.toMutableList()

    if (byAngle.isEmpty()) return emptyList()

    val first = byAngle.first()
    val last = byAngle.last()
    if (first.first > 0f) {
        byAngle.add(0, 0f to first.second)
    }
    if (last.first < 1f) {
        byAngle.add(1f to first.second)
    }

    return byAngle
}

private fun format01(value: Float, decimals: Int): String {
    val v = value.coerceIn(0f, 1f)
    if (decimals <= 0) return (v * 100f).roundToInt().toString()

    val scale = 10.0.pow(decimals.toDouble())
    val vv = round(v.toDouble() * scale) / scale
    return vv.toString()
}

private fun measureTooltipSize(
    textMeasurer: TextMeasurer,
    density: Density,
    valueText: String,
    labelText: String?,
    padH: Dp,
    padV: Dp,
): IntSize {
    val value = textMeasurer.measure(
        text = AnnotatedString(valueText),
        style = TextStyle(fontSize = 12.sp, textAlign = TextAlign.Center)
    ).size

    val label = if (!labelText.isNullOrBlank()) {
        textMeasurer.measure(
            text = AnnotatedString(labelText),
            style = TextStyle(fontSize = 10.sp, textAlign = TextAlign.Center)
        ).size
    } else {
        null
    }

    val padHPx = with(density) { padH.toPx() }
    val padVPx = with(density) { padV.toPx() }
    val gapPx = if (label != null) with(density) { 2.dp.toPx() } else 0f

    val contentW = max(value.width, label?.width ?: 0)
    val contentH = value.height + (label?.height ?: 0) + gapPx.roundToInt()

    val w = kotlin.math.ceil(contentW + padHPx * 2f + 2f).toInt()
    val h = kotlin.math.ceil(contentH + padVPx * 2f + 2f).toInt()

    return IntSize(w, h)
}

@Composable
private fun RadarChartTooltip(
    anchor: Offset,
    canvasSize: IntSize,
    tooltipSize: IntSize,
    margin: Dp,
    background: Color,
    border: Color,
    textColor: Color,
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

    val top = Candidate(x = ax - w / 2, y = ay - h - marginPx)
    val bottom = Candidate(x = ax - w / 2, y = ay + marginPx)
    val right = Candidate(x = ax + marginPx, y = ay - h / 2)
    val left = Candidate(x = ax - w - marginPx, y = ay - h / 2)

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
                color = background,
                shape = RoundedCornerShape(corner)
            )
            .border(
                width = 1.dp,
                color = border,
                shape = RoundedCornerShape(corner)
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
