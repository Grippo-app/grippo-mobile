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
    style: SparklineStyle = SparklineStyle(),
) {
    androidx.compose.foundation.Canvas(modifier) {
        val ptsIn = data.points
        if (ptsIn.isEmpty()) return@Canvas

        // Bounds
        val pad = style.layout.padding.toPx()
        val chart = Rect(pad, pad, size.width - pad, size.height - pad)
        if (chart.width <= 0f || chart.height <= 0f) return@Canvas

        // Domain
        val minX = ptsIn.minOf { it.x }
        val maxX = ptsIn.maxOf { it.x }
        val minY = ptsIn.minOf { it.y }
        val maxY = ptsIn.maxOf { it.y }
        val spanX = (maxX - minX).takeIf { it > 1e-6f } ?: 1f
        val spanY = (maxY - minY).takeIf { it > 1e-6f } ?: 1f

        fun mx(x: Float): Float = chart.left + (x - minX) / spanX * chart.width
        fun my(y: Float): Float = chart.bottom - (y - minY) / spanY * chart.height

        // Sort by x to ensure monotonic path
        val pts = ptsIn.sortedBy { it.x }.map { Offset(mx(it.x), my(it.y)) }

        // Baseline
        if (style.baseline.show) {
            val baseVal = style.baseline.value ?: minY
            val y = my(baseVal.coerceIn(minY, maxY))
            drawLine(
                color = style.baseline.color,
                start = Offset(chart.left, y),
                end = Offset(chart.right, y),
                strokeWidth = style.baseline.width.toPx()
            )
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
                    p1.x + (p2.x - p0.x) * s,
                    p1.y + (p2.y - p0.y) * s
                )
                var c2 = Offset(
                    p2.x - (p3.x - p1.x) * s,
                    p2.y - (p3.y - p1.y) * s
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

        // Fill under line
        style.fill.provider?.let { provider ->
            val area = Path().apply {
                addPath(linePath)
                lineTo(pts.last().x, chart.bottom)
                lineTo(pts.first().x, chart.bottom)
                close()
            }
            drawPath(area, brush = provider(chart))
        }

        // Stroke
        val sw = style.line.stroke.toPx()
        style.line.brush?.let { provider ->
            drawPath(
                linePath,
                brush = provider(chart),
                style = Stroke(width = sw, cap = StrokeCap.Round)
            )
        } ?: run {
            drawPath(
                linePath,
                color = style.line.color,
                style = Stroke(width = sw, cap = StrokeCap.Round)
            )
        }

        // Dots
        if (style.dots.show) {
            val r = style.dots.radius.toPx()
            val dc = style.dots.color ?: style.line.color
            pts.forEach { drawCircle(color = dc, radius = r, center = it) }
        }

        // Min/Max markers
        if (style.extremes.show && ptsIn.size >= 2) {
            val minIdx = ptsIn.indices.minBy { ptsIn[it].y }
            val maxIdx = ptsIn.indices.maxBy { ptsIn[it].y }
            val rr = style.extremes.radius.toPx()
            drawCircle(color = style.extremes.minColor, radius = rr, center = pts[minIdx])
            drawCircle(color = style.extremes.maxColor, radius = rr, center = pts[maxIdx])
        }
    }
}