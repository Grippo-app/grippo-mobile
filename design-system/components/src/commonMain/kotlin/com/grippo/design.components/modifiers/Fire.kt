package com.grippo.design.components.modifiers

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.unit.dp
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.sin
import kotlin.random.Random

public fun Modifier.fire(): Modifier = composed {
    val seeds = remember {
        intArrayOf(
            Random.nextInt(),
            Random.nextInt(),
            Random.nextInt(),
            Random.nextInt(),
            Random.nextInt()
        )
    }

    val infinite = rememberInfiniteTransition(label = "fire")
    val phase = infinite.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = 2600,
                easing = LinearEasing
            )
        ),
        label = "phase"
    ).value

    this.drawWithContent {
        drawPixelFireBehind(
            phase = phase,
            seedA = seeds[0],
            seedB = seeds[1],
            seedC = seeds[2],
            seedD = seeds[3],
            seedE = seeds[4],
        )
        drawContent()
    }
}

private fun DrawScope.drawPixelFireBehind(
    phase: Float,
    seedA: Int,
    seedB: Int,
    seedC: Int,
    seedD: Int,
    seedE: Int,
) {
    val w = size.width
    val h = size.height
    if (w <= 2f || h <= 2f) return

    val alpha = 0.28f
    val coverage = 0.48f

    val cell = 3.dp.toPx().coerceIn(2f, 6f)

    val top = h * (1f - coverage)
    val covH = max(1f, h * coverage)

    val cols = max(1, kotlin.math.ceil(w / cell).toInt())
    val rows = max(1, kotlin.math.ceil(covH / cell).toInt())

    val theta = phase * (2f * PI.toFloat())
    val tSin = sin(theta)
    val tCos = sin(theta + (PI.toFloat() * 0.5f))

    val flicker = 0.92f + 0.16f * (0.5f + 0.5f * sin(theta * 2.0f + 1.3f))

    val cYellow = Color(0xFFF7D44A)
    val cOrange = Color(0xFFF0A23A)
    val cRed = Color(0xFFE66A2E)

    val baseSolidFrac = 0.26f
    val baseSolidRows = max(1, (rows * baseSolidFrac).toInt())

    val tongueCount = 9

    clipRect(left = 0f, top = top, right = w, bottom = h) {
        withTransform({ translate(0f, top) }) {
            for (ry in 0 until rows) {
                val yBottomUp = if (rows <= 1) 1f else ry.toFloat() / (rows - 1).toFloat()
                val y = covH - (ry + 1) * cell

                val inBase = ry >= (rows - baseSolidRows)

                val heatBase = yBottomUp.toDouble().pow(0.80).toFloat()
                val heightFalloff = (1f - yBottomUp).coerceIn(0f, 1f)

                for (cx in 0 until cols) {
                    val u = if (cols <= 1) 0.5f else cx.toFloat() / (cols - 1).toFloat()
                    val x = cx * cell

                    if (x > w) continue

                    var tonguesSum = 0f
                    var tonguesNorm = 0f

                    for (k in 0 until tongueCount) {
                        val center0 = hash01(seedA xor (k * 374761393), 11)
                        val width0 = lerp(0.045f, 0.11f, hash01(seedB xor (k * 668265263), 19))
                        val amp0 = lerp(0.65f, 1.25f, hash01(seedC xor (k * 1442695041), 29))

                        val drift =
                            (0.040f + 0.015f * (k % 3)) * sin(theta * (0.55f + 0.10f * k) + (k * 1.7f))
                        val center = (center0 + drift).coerceIn(0f, 1f)

                        val amp =
                            amp0 * (0.75f + 0.25f * (0.5f + 0.5f * sin(theta * (1.15f + 0.06f * k) + k * 2.1f)))
                        val g = gaussian(u, center, width0)

                        tonguesSum += amp * g
                        tonguesNorm += g
                    }

                    val tonguesBase = if (tonguesNorm > 1e-5f) (tonguesSum / tonguesNorm) else 0f
                    val tongues = (tonguesBase / 1.35f).coerceIn(0f, 1.25f)

                    val nLow = periodicFbm2D(seedD, u * 1.5f, yBottomUp * 0.9f, tSin, tCos, 3)
                    val nMid = periodicFbm2D(
                        seedD xor 0x5F3759DF.toInt(),
                        u * 4.6f,
                        yBottomUp * 1.6f,
                        tSin,
                        tCos,
                        4
                    )
                    val nHi = periodicFbm2D(
                        seedD xor 0x27D4EB2D,
                        u * 12.0f,
                        yBottomUp * 3.2f,
                        tSin,
                        tCos,
                        3
                    )

                    val ridge = ridged01(nMid * 0.8f + nHi * 1.05f)
                    val tipMask =
                        smoothstep(0.22f, 0.88f, tongues) * smoothstep(0.12f, 0.80f, heightFalloff)
                    val tip = (max(0f, (0.55f * nMid + 0.45f * nHi)) * tipMask).toDouble().pow(2.2)
                        .toFloat()

                    val turbulence = (0.18f * nLow + 0.32f * nMid + 0.50f * nHi)

                    val intensity = if (inBase) {
                        1.08f + 0.10f * turbulence
                    } else {
                        (
                                0.25f +
                                        0.85f * heatBase +
                                        0.95f * tongues +
                                        0.22f * ridge +
                                        0.18f * tip +
                                        0.10f * turbulence
                                ) * flicker
                    }

                    val baseCut = lerp(0.92f, 0.38f, yBottomUp)
                    val cutJitter =
                        0.08f * periodicFbm2D(seedE, u * 3.0f, yBottomUp * 1.2f, tSin, tCos, 2)
                    val cut = (baseCut + cutJitter).coerceIn(0.20f, 0.98f)

                    if (!inBase && intensity <= cut) continue

                    val band = if (inBase) {
                        0
                    } else {
                        when {
                            intensity > 1.20f -> 2
                            intensity > 0.92f -> 1
                            else -> 0
                        }
                    }

                    val color = when (band) {
                        2 -> cRed
                        1 -> cOrange
                        else -> cYellow
                    }

                    val bandAlpha = when (band) {
                        2 -> alpha * 0.70f
                        1 -> alpha * 0.78f
                        else -> alpha * 0.92f
                    }

                    val wCell = min(cell, w - x).coerceAtLeast(0f)
                    val hCell = min(cell, covH - y).coerceAtLeast(0f)
                    if (wCell <= 0f || hCell <= 0f) continue

                    drawRect(
                        color = color,
                        topLeft = Offset(x, y),
                        size = Size(wCell + 0.35f, hCell + 0.35f),
                        alpha = bandAlpha
                    )
                }
            }

            drawEmbers(
                phase = phase,
                seed = seedE xor 0x6D2B79F5,
                w = w,
                h = covH,
                cell = cell,
                alpha = alpha,
                yellow = cYellow,
                orange = cOrange,
                red = cRed
            )
        }
    }
}

private fun DrawScope.drawEmbers(
    phase: Float,
    seed: Int,
    w: Float,
    h: Float,
    cell: Float,
    alpha: Float,
    yellow: Color,
    orange: Color,
    red: Color
) {
    val emberCount = 22
    for (i in 0 until emberCount) {
        val s = seed xor (i * 0x9E3779B9.toInt())

        val x01 = hash01(s, 11)
        val offset = hash01(s, 19)
        val speed = lerp(0.55f, 1.15f, hash01(s, 29))

        val pRaw = (phase * speed + offset)
        val p = pRaw - floor(pRaw)

        val fadeIn = smoothstep(0.02f, 0.14f, p)
        val fadeOut = 1f - smoothstep(0.86f, 0.98f, p)
        val a = alpha * 0.55f * fadeIn * fadeOut
        if (a <= 0.001f) continue

        val x = (x01 * w)
        val y = h - (p * h * 0.95f)

        val jitter = (hashSigned(
            s,
            71
        ) * 0.35f + sin((phase * 2f * PI.toFloat()) + offset * 6.2831f) * 0.65f) * cell
        val xx = (x + jitter).coerceIn(0f, w - 1f)

        val sizeCells = if (hash01(s, 37) > 0.82f) 2 else 1
        val sz = cell * sizeCells

        val band = when {
            p > 0.70f -> 2
            p > 0.40f -> 1
            else -> 0
        }
        val c = when (band) {
            2 -> red
            1 -> orange
            else -> yellow
        }

        drawRect(
            color = c,
            topLeft = Offset(xx, y),
            size = Size(sz, sz),
            alpha = a
        )
    }
}

private fun periodicFbm2D(
    seed: Int,
    x: Float,
    y: Float,
    tSin: Float,
    tCos: Float,
    octaves: Int
): Float {
    var sum = 0f
    var amp = 0.55f
    var freq = 1f
    var norm = 0f

    for (i in 0 until octaves) {
        val s = seed + i * 1013
        val xx = x * freq
        val yy = y * freq

        val a = valueNoise2D(s, xx + 13.7f, yy + 9.1f + tSin * (0.75f + 0.10f * i) * freq)
        val b = valueNoise2D(
            s xor 0x5F3759DF.toInt(),
            xx - 6.4f + tCos * (0.78f + 0.10f * i) * freq,
            yy - 3.3f
        )

        val n = (a + b) * 0.5f
        sum += n * amp
        norm += amp

        amp *= 0.5f
        freq *= 2f
    }

    return if (norm > 0f) (sum / norm) else 0f
}

private fun valueNoise2D(seed: Int, x: Float, y: Float): Float {
    val x0 = floor(x).toInt()
    val y0 = floor(y).toInt()
    val x1 = x0 + 1
    val y1 = y0 + 1

    val tx = x - x0.toFloat()
    val ty = y - y0.toFloat()

    val v00 = hashSigned(seed, x0, y0)
    val v10 = hashSigned(seed, x1, y0)
    val v01 = hashSigned(seed, x0, y1)
    val v11 = hashSigned(seed, x1, y1)

    val ux = fade(tx)
    val uy = fade(ty)

    val a = lerp(v00, v10, ux)
    val b = lerp(v01, v11, ux)
    return lerp(a, b, uy)
}

private fun ridged01(n: Float): Float {
    val r = 1f - abs(n.coerceIn(-1f, 1f))
    return (r * r).coerceIn(0f, 1f)
}

private fun gaussian(x: Float, mean: Float, sigma: Float): Float {
    val s = sigma.coerceAtLeast(1e-4f)
    val d = (x - mean) / s
    val v = -0.5f * d * d
    return kotlin.math.exp(v)
}

private fun hashSigned(seed: Int, salt: Int): Float {
    var h = seed xor (salt * 0x27D4EB2D)
    h = (h xor (h ushr 15)) * 0x85EBCA6B.toInt()
    h = (h xor (h ushr 13)) * 0xC2B2AE35.toInt()
    h = h xor (h ushr 16)
    val v = (h ushr 1) % 10_000
    return (v / 10_000f) * 2f - 1f
}

private fun hashSigned(seed: Int, x: Int, y: Int): Float {
    var h = seed
    h = h xor (x * 0x27D4EB2D)
    h = h xor (y * 0x165667B1)
    h = (h xor (h ushr 15)) * 0x85EBCA6B.toInt()
    h = (h xor (h ushr 13)) * 0xC2B2AE35.toInt()
    h = h xor (h ushr 16)
    val v = (h ushr 1) % 10_000
    return (v / 10_000f) * 2f - 1f
}

private fun hash01(seed: Int, salt: Int): Float {
    var x = seed xor (salt * 0x9E3779B9.toInt())
    x = (x xor (x ushr 16)) * 0x7FEB352D
    x = (x xor (x ushr 15)) * 0x846CA68B.toInt()
    x = x xor (x ushr 16)
    val u = x ushr 1
    return (u % 10_000) / 10_000f
}

private fun fade(t: Float): Float = t * t * (3f - 2f * t)

private fun smoothstep(edge0: Float, edge1: Float, x: Float): Float {
    val t = ((x - edge0) / (edge1 - edge0)).coerceIn(0f, 1f)
    return t * t * (3f - 2f * t)
}

private fun lerp(a: Float, b: Float, t: Float): Float = a + (b - a) * t
