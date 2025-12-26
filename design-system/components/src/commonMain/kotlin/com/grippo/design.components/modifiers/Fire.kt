package com.grippo.design.components.modifiers

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.unit.dp
import kotlin.math.PI
import kotlin.math.ceil
import kotlin.math.exp
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.sin
import kotlin.random.Random

public fun Modifier.fire(): Modifier = composed {
    val seed = remember { Random.nextInt() }
    val seeds = remember {
        intArrayOf(
            seed,
            seed xor 0x6D2B79F5,
            seed xor 0x9E3779B9.toInt(),
            seed xor 0x85EBCA6B.toInt(),
            seed xor 0xC2B2AE35.toInt()
        )
    }

    val infinite = rememberInfiniteTransition(label = "fire")

    val phase01 = infinite.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "phase"
    ).value

    val flicker = infinite.animateFloat(
        initialValue = 0.96f,
        targetValue = 1.04f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 900, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "flicker"
    ).value

    this.drawWithContent {
        drawFire40dp(
            phase01 = phase01,
            flicker = flicker,
            seedA = seeds[0],
            seedB = seeds[1],
            seedC = seeds[2],
            seedD = seeds[3],
            seedE = seeds[4]
        )
        drawContent()
    }
}

private fun DrawScope.drawFire40dp(
    phase01: Float,
    flicker: Float,
    seedA: Int,
    seedB: Int,
    seedC: Int,
    seedD: Int,
    seedE: Int
) {
    val w = size.width
    val h = size.height
    if (w <= 2f || h <= 2f) return

    val flameH = min(h, 40.dp.toPx().coerceAtLeast(1f))
    val top = h - flameH

    val alpha = 0.24f

    val theta = phase01 * (2f * PI.toFloat())
    val t = phase01

    val core = Color(0xFFFFF2B0)
    val mid = Color(0xFFFFB300)
    val outer = Color(0xFFFF3D00)
    val glow = Color(0x66FF6D00)

    val glowHeight = min(flameH * 1.10f, 72.dp.toPx())
    val glowBrush = Brush.radialGradient(
        colors = listOf(glow.copy(alpha = 0.55f * alpha), Color.Transparent),
        center = Offset(w * 0.5f, flameH),
        radius = max(w, glowHeight) * 0.95f
    )

    val samples = max(180, ceil(w / 6f).toInt())
    val inv = 1f / samples.toFloat()

    val tongueCount = 8
    val centers = FloatArray(tongueCount) { k ->
        hash01(seedA xor (k * 374761393), 11).coerceIn(
            0.06f,
            0.94f
        )
    }
    val widths =
        FloatArray(tongueCount) { k -> lerp(0.045f, 0.090f, hash01(seedB xor (k * 668265263), 19)) }
    val amps =
        FloatArray(tongueCount) { k -> lerp(0.95f, 1.35f, hash01(seedC xor (k * 1442695041), 29)) }
    val phases = FloatArray(tongueCount) { k ->
        hash01(
            seedD xor (k * 1103515245),
            37
        ) * (2f * PI.toFloat())
    }

    val outerY = FloatArray(samples + 1)
    val innerY = FloatArray(samples + 1)

    fun warp(u: Float): Float {
        val w1 = multiSine(seedD, u * 1.7f, theta, 4)
        val w2 = multiSine(seedE, u * 4.3f, theta, 3)
        val du = 0.035f * w1 + 0.018f * w2
        return (u + du).coerceIn(0f, 1f)
    }

    fun tongues(u: Float): Float {
        var m = 0f
        for (k in 0 until tongueCount) {
            val drift = 0.065f * sin(theta * (0.75f + 0.08f * k) + phases[k] + k * 1.6f)
            val c = (centers[k] + drift).coerceIn(0f, 1f)
            val g = gaussian(u, c, widths[k])
            val a = amps[k] * (0.86f + 0.14f * sin(theta * (1.25f + 0.06f * k) + phases[k] * 0.7f))
            m = max(m, g * a)
        }
        return m
    }

    for (i in 0..samples) {
        val u0 = i * inv
        val u = warp(u0)

        val tongue = (tongues(u) / 1.22f).coerceIn(0f, 1.25f)
        val tongueSharp = tongue.toDouble().pow(2.55).toFloat()

        val tipNoise =
            (0.5f + 0.5f * multiSine(seedB, u * 10.5f - t * 2.2f, theta, 5)).coerceIn(0f, 1f)
        val tipSharp = tipNoise.toDouble().pow(2.8).toFloat()

        val tipMask = smoothstep(0.32f, 0.92f, tongue)
        val micro = 0.22f * tipSharp * tipMask

        val base = 0.18f
        val heightFrac = (base + 0.98f * tongueSharp + micro).coerceIn(0.08f, 1.25f)

        val ho = (flameH * 0.98f * heightFrac * flicker).coerceIn(0f, flameH)
        val hi = (ho * (0.62f + 0.10f * tongue)).coerceIn(0f, flameH)

        outerY[i] = flameH - ho
        innerY[i] = flameH - hi
    }

    // Keep tongues sharp: minimal smoothing only to avoid jittery edge.
    smoothInPlace(outerY, passes = 1)
    smoothInPlace(innerY, passes = 1)

    val outerPath = buildFlamePath(w, flameH, outerY)
    val innerPath = buildFlamePath(w, flameH, innerY)

    val outerBrush = Brush.verticalGradient(
        colorStops = arrayOf(
            0.00f to outer.copy(alpha = 0.00f),
            0.20f to outer.copy(alpha = 0.44f * alpha),
            0.58f to mid.copy(alpha = 0.56f * alpha),
            0.92f to core.copy(alpha = 0.56f * alpha),
            1.00f to core.copy(alpha = 0.62f * alpha)
        ),
        startY = 0f,
        endY = flameH
    )

    val innerBrush = Brush.verticalGradient(
        colorStops = arrayOf(
            0.00f to core.copy(alpha = 0.00f),
            0.62f to core.copy(alpha = 0.40f * alpha),
            0.90f to core.copy(alpha = 0.66f * alpha),
            1.00f to core.copy(alpha = 0.72f * alpha)
        ),
        startY = 0f,
        endY = flameH
    )

    clipRect(left = 0f, top = top, right = w, bottom = h) {
        withTransform({ translate(0f, top) }) {
            drawRect(
                brush = glowBrush,
                topLeft = Offset(0f, flameH - glowHeight),
                size = androidx.compose.ui.geometry.Size(w, glowHeight),
                alpha = 1f
            )
            drawPath(path = outerPath, brush = outerBrush, alpha = 1f)
            drawPath(path = innerPath, brush = innerBrush, alpha = 1f)
        }
    }
}

private fun buildFlamePath(w: Float, flameH: Float, ys: FloatArray): Path {
    val n = ys.size - 1
    val stepX = w / n.toFloat()

    val p = Path()
    p.moveTo(0f, flameH)
    p.lineTo(0f, ys[0])

    // Polyline keeps tongues more "flame-like" than heavy cubic smoothing.
    for (i in 1..n) {
        p.lineTo(i * stepX, ys[i])
    }

    p.lineTo(w, flameH)
    p.close()
    return p
}

private fun gaussian(x: Float, mean: Float, sigma: Float): Float {
    val s = sigma.coerceAtLeast(1e-4f)
    val d = (x - mean) / s
    return exp(-0.5f * d * d)
}

private fun multiSine(seed: Int, x: Float, theta: Float, harmonics: Int): Float {
    var sum = 0f
    var norm = 0f
    for (i in 0 until harmonics) {
        val f = 1 shl i
        val a = lerp(0.55f, 1.00f, hash01(seed xor (i * 1013), 17)) / f.toFloat()
        val ph = hash01(seed xor (i * 69069), 31) * (2f * PI.toFloat())
        val sp = lerp(0.7f, 1.6f, hash01(seed xor (i * 362437), 43))
        sum += a * sin((2f * PI.toFloat()) * (x * 0.18f * f) + theta * sp + ph)
        norm += a
    }
    return if (norm > 1e-6f) (sum / norm).coerceIn(-1f, 1f) else 0f
}

private fun smoothInPlace(a: FloatArray, passes: Int) {
    if (a.size < 5) return
    repeat(passes.coerceAtLeast(1)) {
        val c = a.copyOf()
        for (i in 2 until a.size - 2) {
            a[i] = c[i - 2] * 0.08f +
                    c[i - 1] * 0.22f +
                    c[i] * 0.40f +
                    c[i + 1] * 0.22f +
                    c[i + 2] * 0.08f
        }
    }
}

private fun smoothstep(edge0: Float, edge1: Float, x: Float): Float {
    val t = ((x - edge0) / (edge1 - edge0)).coerceIn(0f, 1f)
    return t * t * (3f - 2f * t)
}

private fun lerp(a: Float, b: Float, t: Float): Float = a + (b - a) * t

private fun hash01(seed: Int, salt: Int): Float {
    var x = seed xor (salt * 0x9E3779B9.toInt())
    x = (x xor (x ushr 16)) * 0x7FEB352D
    x = (x xor (x ushr 15)) * 0x846CA68B.toInt()
    x = x xor (x ushr 16)
    val u = x ushr 1
    return (u % 10_000) / 10_000f
}