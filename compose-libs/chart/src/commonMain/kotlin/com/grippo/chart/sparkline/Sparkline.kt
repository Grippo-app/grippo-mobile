package com.grippo.chart.sparkline

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import kotlin.math.max
import kotlin.math.min

@Composable
public fun Sparkline(
    modifier: Modifier = Modifier,
    data: SparklineData,
    style: SparklineStyle,
) {
    androidx.compose.foundation.Canvas(modifier) {
        val ptsIn = data.points
        if (ptsIn.isEmpty()) return@Canvas

        val dotsPadding = when (val d = style.dots) {
            is SparklineStyle.Dots.None -> 0f
            is SparklineStyle.Dots.Visible -> d.radius.toPx()
        }
        val extremesPadding = when (val ex = style.extremes) {
            is SparklineStyle.Extremes.None -> 0f
            is SparklineStyle.Extremes.Visible -> ex.radius.toPx()
        }
        val padding = max(dotsPadding, extremesPadding)

        val chart = Rect(
            left = padding,
            top = padding,
            right = size.width - padding,
            bottom = size.height - padding
        )
        if (chart.width <= 0f || chart.height <= 0f) return@Canvas

        val minX = ptsIn.minOf { it.x }
        val maxX = ptsIn.maxOf { it.x }
        val minY = ptsIn.minOf { it.y }
        val maxY = ptsIn.maxOf { it.y }

        val spanXRaw = maxX - minX
        val spanYRaw = maxY - minY
        val flatX = spanXRaw <= 1e-6f
        val flatY = spanYRaw <= 1e-6f
        val spanX = if (flatX) 1f else spanXRaw
        val spanY = if (flatY) 1f else spanYRaw

        fun mx(x: Float): Float {
            return if (flatX) {
                chart.left + chart.width / 2f
            } else {
                chart.left + (x - minX) / spanX * chart.width
            }
        }

        fun my(y: Float): Float {
            return if (flatY) {
                chart.top + chart.height / 2f
            } else {
                chart.bottom - (y - minY) / spanY * chart.height
            }
        }

        val sortedIn = ptsIn.sortedBy { it.x }
        val pts = sortedIn.map { Offset(mx(it.x), my(it.y)) }

        when (val base = style.baseline) {
            is SparklineStyle.Baseline.None -> Unit
            is SparklineStyle.Baseline.Visible -> {
                val baseVal = base.value ?: minY
                val y = my(baseVal.coerceIn(minY, maxY))
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
                val pos = mid.position.coerceIn(0f, 1f)
                val y = chart.top + chart.height * pos
                val dash = mid.dash.toPx().coerceAtLeast(1f)
                val gap = mid.gap.toPx().coerceAtLeast(1f)
                val effect = androidx.compose.ui.graphics.PathEffect.dashPathEffect(
                    intervals = floatArrayOf(dash, gap),
                    phase = 0f
                )

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
                        pathEffect = effect
                    )
                )
            }
        }

        val sw = style.line.stroke.toPx()

        if (pts.size == 1) {
            val center = pts.first()
            val minSize = min(chart.width, chart.height)

            val pointColor = when (val ex = style.extremes) {
                is SparklineStyle.Extremes.Visible -> ex.maxColor
                is SparklineStyle.Extremes.None -> when (val d = style.dots) {
                    is SparklineStyle.Dots.Visible -> d.color ?: style.line.color
                    is SparklineStyle.Dots.None -> style.line.color
                }
            }

            val baseR = when (val d = style.dots) {
                is SparklineStyle.Dots.Visible -> d.radius.toPx()
                is SparklineStyle.Dots.None -> sw * 1.3f
            }
            val r = max(baseR, minSize * 0.04f)
            val ringW = max(sw * 0.8f, 1f)

            val inset = chart.width * 0.18f
            drawLine(
                color = pointColor.copy(alpha = 0.42f),
                start = Offset(chart.left + inset, center.y),
                end = Offset(chart.right - inset, center.y),
                strokeWidth = sw,
                cap = StrokeCap.Round
            )

            drawLine(
                color = pointColor.copy(alpha = 0.18f),
                start = Offset(center.x, center.y),
                end = Offset(center.x, chart.bottom),
                strokeWidth = ringW,
                cap = StrokeCap.Round
            )

            drawCircle(
                color = pointColor.copy(alpha = 0.16f),
                radius = r * 2.6f,
                center = center
            )

            drawCircle(
                color = pointColor.copy(alpha = 0.35f),
                radius = r * 1.8f,
                center = center,
                style = Stroke(width = ringW, cap = StrokeCap.Round)
            )

            drawCircle(
                color = pointColor,
                radius = r,
                center = center
            )

            return@Canvas
        }

        fun buildLinePath(points: List<Offset>): Path {
            val path = Path()
            if (points.isEmpty()) return path
            if (!style.line.curved || points.size < 3) {
                path.moveTo(points.first().x, points.first().y)
                for (i in 1 until points.size) path.lineTo(points[i].x, points[i].y)
                return path
            }

            val s = style.line.curveSmoothness.coerceIn(0f, 0.5f)
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

                if (style.line.clampOvershoot) {
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

        val linePath = buildLinePath(pts)

        style.fill?.provider?.let { provider ->
            val area = Path().apply {
                addPath(linePath)
                lineTo(pts.last().x, chart.bottom)
                lineTo(pts.first().x, chart.bottom)
                close()
            }
            drawPath(area, brush = provider(chart))
        }

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
                pts.forEach { drawCircle(color = dc, radius = r, center = it) }
            }
        }

        when (val ex = style.extremes) {
            is SparklineStyle.Extremes.None -> Unit
            is SparklineStyle.Extremes.Visible -> {
                val rr = ex.radius.toPx()

                val minIdx = sortedIn.indices.minByOrNull { sortedIn[it].y } ?: 0
                val maxIdx = sortedIn.indices.maxByOrNull { sortedIn[it].y } ?: 0

                if (minIdx == maxIdx) {
                    drawCircle(color = ex.maxColor, radius = rr, center = pts[maxIdx])
                } else {
                    drawCircle(color = ex.minColor, radius = rr, center = pts[minIdx])
                    drawCircle(color = ex.maxColor, radius = rr, center = pts[maxIdx])
                }
            }
        }
    }
}
