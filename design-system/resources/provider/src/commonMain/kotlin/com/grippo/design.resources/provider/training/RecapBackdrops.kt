package com.grippo.design.resources.provider.training

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.PathBuilder
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

private const val BACKDROP_SIZE = 200f
private const val PATH_DOT_COUNT = 6

public fun recapRecordBackdrop(accent: Color): ImageVector = backdrop(name = "RecapRecordBackdrop") {
    glow(center = Offset(110f, 96f), radius = 80f, accent = accent, alpha = 0.34f)
    path(
        fill = Brush.linearGradient(
            colors = listOf(accent.copy(alpha = 0.95f), accent.copy(alpha = 0.7f)),
            start = Offset(130f, 18f),
            end = Offset(90f, 180f),
        ),
        stroke = SolidColor(accent.copy(alpha = 0.82f)),
        strokeLineWidth = 6f,
        strokeLineJoin = StrokeJoin.Round,
    ) {
        moveTo(122f, 18f)
        lineTo(72f, 106f)
        lineTo(104f, 106f)
        lineTo(88f, 180f)
        lineTo(150f, 86f)
        lineTo(116f, 86f)
        lineTo(138f, 18f)
        close()
    }
    star(center = Offset(168f, 46f), radius = 15f, accent = accent, alpha = 0.85f)
    star(center = Offset(46f, 60f), radius = 10f, accent = accent, alpha = 0.6f)
    star(center = Offset(60f, 152f), radius = 6f, accent = accent, alpha = 0.5f)
}

public fun recapImprovedBackdrop(accent: Color): ImageVector = backdrop(name = "RecapImprovedBackdrop") {
    glow(center = Offset(120f, 88f), radius = 72f, accent = accent, alpha = 0.3f)
    path(
        stroke = Brush.linearGradient(
            colors = listOf(accent.copy(alpha = 0.22f), accent.copy(alpha = 0.9f)),
            start = Offset(30f, 160f),
            end = Offset(150f, 62f),
        ),
        strokeLineWidth = 16f,
        strokeLineCap = StrokeCap.Round,
    ) {
        moveTo(30f, 160f)
        curveTo(70f, 150f, 110f, 120f, 150f, 62f)
    }
    path(
        fill = SolidColor(accent.copy(alpha = 0.92f)),
        stroke = SolidColor(accent.copy(alpha = 0.92f)),
        strokeLineWidth = 4f,
        strokeLineJoin = StrokeJoin.Round,
    ) {
        moveTo(165f, 40f)
        lineTo(162f, 76f)
        lineTo(133f, 55f)
        close()
    }
    orb(center = Offset(52f, 92f), radius = 9f, accent = accent, alpha = 0.7f)
    orb(center = Offset(160f, 140f), radius = 6f, accent = accent, alpha = 0.5f)
    orb(center = Offset(100f, 40f), radius = 4f, accent = accent, alpha = 0.45f)
}

public fun recapStableBackdrop(accent: Color): ImageVector = backdrop(name = "RecapStableBackdrop") {
    glow(center = Offset(100f, 104f), radius = 80f, accent = accent, alpha = 0.24f)
    path(
        stroke = Brush.linearGradient(
            colors = listOf(accent.copy(alpha = 0.55f), accent.copy(alpha = 0.9f), accent.copy(alpha = 0.55f)),
            start = Offset(40f, 106f),
            end = Offset(160f, 106f),
        ),
        strokeLineWidth = 12f,
        strokeLineCap = StrokeCap.Round,
    ) {
        moveTo(40f, 106f)
        lineTo(160f, 106f)
    }
    path(
        fill = SolidColor(accent.copy(alpha = 0.8f)),
        stroke = SolidColor(accent.copy(alpha = 0.8f)),
        strokeLineWidth = 6f,
        strokeLineJoin = StrokeJoin.Round,
    ) {
        moveTo(100f, 116f)
        lineTo(118f, 148f)
        lineTo(82f, 148f)
        close()
    }
    path(
        stroke = SolidColor(accent.copy(alpha = 0.6f)),
        strokeLineWidth = 8f,
        strokeLineCap = StrokeCap.Round,
    ) {
        moveTo(74f, 158f)
        lineTo(126f, 158f)
    }
    orb(center = Offset(56f, 84f), radius = 14f, accent = accent, alpha = 0.85f)
    orb(center = Offset(144f, 84f), radius = 14f, accent = accent, alpha = 0.85f)
    orb(center = Offset(100f, 56f), radius = 5f, accent = accent, alpha = 0.45f)
}

public fun recapDeclinedBackdrop(accent: Color): ImageVector = backdrop(name = "RecapDeclinedBackdrop") {
    glow(center = Offset(104f, 100f), radius = 76f, accent = accent, alpha = 0.2f)
    path(
        fill = Brush.linearGradient(
            colors = listOf(accent.copy(alpha = 0.72f), accent.copy(alpha = 0.32f)),
            start = Offset(150f, 44f),
            end = Offset(54f, 158f),
        ),
    ) {
        moveTo(54f, 158f)
        curveTo(70f, 110f, 110f, 60f, 150f, 44f)
        curveTo(152f, 82f, 120f, 132f, 54f, 158f)
        close()
    }
    path(
        stroke = SolidColor(accent.copy(alpha = 0.85f)),
        strokeLineWidth = 4f,
        strokeLineCap = StrokeCap.Round,
    ) {
        moveTo(46f, 168f)
        curveTo(78f, 128f, 112f, 84f, 148f, 48f)
    }
    orb(center = Offset(164f, 120f), radius = 6f, accent = accent, alpha = 0.4f)
    orb(center = Offset(58f, 86f), radius = 4f, accent = accent, alpha = 0.35f)
    orb(center = Offset(132f, 150f), radius = 3f, accent = accent, alpha = 0.3f)
}

public fun recapFirstWorkoutBackdrop(accent: Color): ImageVector = backdrop(name = "RecapFirstWorkoutBackdrop") {
    glow(center = Offset(116f, 94f), radius = 74f, accent = accent, alpha = 0.26f)
    path(
        stroke = SolidColor(accent.copy(alpha = 0.85f)),
        strokeLineWidth = 6f,
    ) {
        circle(center = Offset(52f, 148f), radius = 14f)
    }
    orb(center = Offset(52f, 148f), radius = 5f, accent = accent, alpha = 0.85f)
    repeat(PATH_DOT_COUNT) { index ->
        val t = (index + 1f) / (PATH_DOT_COUNT + 1f)
        orb(
            center = cubicPoint(
                t = t,
                start = Offset(70f, 136f),
                first = Offset(100f, 120f),
                second = Offset(120f, 96f),
                end = Offset(146f, 70f),
            ),
            radius = 4f,
            accent = accent,
            alpha = 0.3f + 0.5f * t,
        )
    }
    star(center = Offset(160f, 52f), radius = 22f, accent = accent, alpha = 0.95f)
    star(center = Offset(184f, 94f), radius = 8f, accent = accent, alpha = 0.6f)
    star(center = Offset(118f, 34f), radius = 6f, accent = accent, alpha = 0.5f)
}

private fun backdrop(name: String, content: ImageVector.Builder.() -> Unit): ImageVector {
    return ImageVector.Builder(
        name = name,
        defaultWidth = BACKDROP_SIZE.dp,
        defaultHeight = BACKDROP_SIZE.dp,
        viewportWidth = BACKDROP_SIZE,
        viewportHeight = BACKDROP_SIZE,
    ).apply(content).build()
}

private fun ImageVector.Builder.glow(center: Offset, radius: Float, accent: Color, alpha: Float) {
    path(
        fill = Brush.radialGradient(
            colors = listOf(accent.copy(alpha = alpha), accent.copy(alpha = 0f)),
            center = center,
            radius = radius,
        ),
    ) {
        circle(center = center, radius = radius)
    }
}

private fun ImageVector.Builder.orb(center: Offset, radius: Float, accent: Color, alpha: Float) {
    path(
        fill = Brush.linearGradient(
            colors = listOf(accent.copy(alpha = alpha), accent.copy(alpha = alpha * 0.6f)),
            start = Offset(center.x - radius, center.y - radius),
            end = Offset(center.x + radius, center.y + radius),
        ),
    ) {
        circle(center = center, radius = radius)
    }
}

private fun ImageVector.Builder.star(center: Offset, radius: Float, accent: Color, alpha: Float) {
    val waist = radius * 0.22f

    path(fill = SolidColor(accent.copy(alpha = alpha))) {
        moveTo(center.x, center.y - radius)
        curveTo(center.x + waist, center.y - waist, center.x + waist, center.y - waist, center.x + radius, center.y)
        curveTo(center.x + waist, center.y + waist, center.x + waist, center.y + waist, center.x, center.y + radius)
        curveTo(center.x - waist, center.y + waist, center.x - waist, center.y + waist, center.x - radius, center.y)
        curveTo(center.x - waist, center.y - waist, center.x - waist, center.y - waist, center.x, center.y - radius)
        close()
    }
}

private fun PathBuilder.circle(center: Offset, radius: Float) {
    moveTo(center.x - radius, center.y)
    arcToRelative(radius, radius, 0f, true, false, radius * 2f, 0f)
    arcToRelative(radius, radius, 0f, true, false, -radius * 2f, 0f)
    close()
}

private fun cubicPoint(t: Float, start: Offset, first: Offset, second: Offset, end: Offset): Offset {
    val inverse = 1f - t
    val a = inverse * inverse * inverse
    val b = 3f * inverse * inverse * t
    val c = 3f * inverse * t * t
    val d = t * t * t

    return Offset(
        x = a * start.x + b * first.x + c * second.x + d * end.x,
        y = a * start.y + b * first.y + c * second.y + d * end.y,
    )
}
