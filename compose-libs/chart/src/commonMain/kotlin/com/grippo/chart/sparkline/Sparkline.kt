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
    values: List<Float>,
    style: SparklineStyle = SparklineStyle(),
) {
    androidx.compose.foundation.Canvas(modifier) {
        if (values.isEmpty()) return@Canvas

        // bounds
        val pad = style.padding.toPx()
        val chart = Rect(pad, pad, size.width - pad, size.height - pad)

        // domain
        val minV = values.minOrNull() ?: 0f
        val maxV = values.maxOrNull() ?: 1f
        val span = (maxV - minV).takeIf { it > 1e-6f } ?: 1f

        fun mx(i: Int): Float = chart.left + i * chart.width / (values.size - 1).coerceAtLeast(1)
        fun my(v: Float): Float = chart.bottom - (v - minV) / span * chart.height

        // mapped points
        val pts = values.mapIndexed { i, v -> Offset(mx(i), my(v)) }

        // optional baseline
        if (style.showBaseline) {
            val baseVal = style.baselineValue ?: minV
            val y = my(baseVal.coerceIn(minV, maxV))
            drawLine(
                color = style.baselineColor,
                start = Offset(chart.left, y),
                end = Offset(chart.right, y),
                strokeWidth = style.baselineWidth.toPx()
            )
        }

        // build line path (optional smoothing)
        fun buildLinePath(points: List<Offset>): Path {
            val path = Path()
            if (points.isEmpty()) return path
            if (!style.curved || points.size < 3) {
                path.moveTo(points.first().x, points.first().y)
                for (i in 1 until points.size) path.lineTo(points[i].x, points[i].y)
                return path
            }
            val s = style.curveSmoothness.coerceIn(0f, 0.5f)
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
                if (style.clampOvershoot) {
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

        // fill under line
        style.fill?.let { provider ->
            val area = Path().apply {
                addPath(linePath)
                lineTo(pts.last().x, chart.bottom)
                lineTo(pts.first().x, chart.bottom)
                close()
            }
            drawPath(area, brush = provider(chart))
        }

        // stroke
        val sw = style.stroke.toPx()
        style.lineBrush?.let { brushProvider ->
            drawPath(
                linePath, brush = brushProvider(chart), style = Stroke(
                    width = sw,
                    cap = StrokeCap.Round
                )
            )
        } ?: run {
            drawPath(
                linePath,
                color = style.color,
                style = Stroke(width = sw, cap = StrokeCap.Round)
            )
        }

        // dots
        if (style.showDots) {
            val r = style.dotRadius.toPx()
            val dc = style.dotColor ?: style.color
            pts.forEach { drawCircle(color = dc, radius = r, center = it) }
        }

        // min/max markers
        if (style.showMinMax && values.size >= 2) {
            val minIdx = values.indices.minBy { values[it] }
            val maxIdx = values.indices.maxBy { values[it] }
            val rr = style.minMaxRadius.toPx()
            drawCircle(color = style.minColor, radius = rr, center = pts[minIdx])
            drawCircle(color = style.maxColor, radius = rr, center = pts[maxIdx])
        }
    }
}