package com.grippo.design.components.modifiers

import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipRect
import kotlin.math.PI
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.sin

public fun Modifier.fire(
    intensity: Float = 1f,
    speed: Float = 1f,
    frequency: Float = 1f,
): Modifier = composed {
    val seed = remember { (kotlin.random.Random.nextInt() xor 0x6D2B79F5) }
    var timeNanos by remember { mutableLongStateOf(0L) }

    val intensityState by rememberUpdatedState(intensity.coerceIn(0f, 1f))
    val speedState by rememberUpdatedState(speed.coerceIn(0f, 1f))
    val frequencyState by rememberUpdatedState(frequency.coerceIn(0f, 1f))

    LaunchedEffect(Unit) {
        while (true) {
            withFrameNanos {
                timeNanos = it
            }
        }
    }

    this
        .drawWithContent {
            val i = intensityState
            val s = speedState
            val f = frequencyState
            drawContent()
            if (i > 0.001f) {
                drawProceduralFire(
                    seed = seed,
                    timeNanos = timeNanos,
                    intensity = i,
                    speed = s,
                    frequency = f,
                )
            }
        }
}

private fun DrawScope.drawProceduralFire(
    seed: Int,
    timeNanos: Long,
    intensity: Float,
    speed: Float,
    frequency: Float,
) {
    val w = size.width
    val h = size.height
    if (w <= 2f || h <= 2f) return

    val i = intensity.coerceIn(0f, 1f)
    val s = speed.coerceIn(0f, 1f)
    val f = frequency.coerceIn(0f, 1f)
    val flameH = (h * i).coerceAtLeast(1f)
    if (flameH < 1.5f) return

    val bottom = h
    val top = h - flameH
    // Global time multiplier: at speed=1 we want very fast burn (≈2x faster than previous speed=1).
    val timeScale = ((0.22f + 1.55f * s) * (1f + s) * (1f + s)).coerceIn(0.15f, 7.2f)
    val tSec = (timeNanos / 1_000_000_000f) * timeScale

    // No baseGlow band in cartoon mode (it reads as a flat strip).

    clipRect(left = 0f, top = top, right = w, bottom = bottom) {
        // Cartoon fire doesn't need a flat full-width glow band (it reads as a straight layer).
        // drawRect(brush = baseGlow)

        // Cartoon mode: layered tongues (back big / front small), with non-uniform spacing and smooth height noise.
        val count = max(3, ceil(w / (170f - 70f * f)).toInt())
        val cellW = (w / count.toFloat()).coerceAtLeast(1f)
        val widthScale = (0.52f + 0.55f * smooth01(i)).coerceIn(0.52f, 1.05f)
        val tongueWBase = (cellW * 1.35f * widthScale).coerceIn(12f, 78f)
        val maxTongueH = (flameH * 0.98f).coerceAtLeast(1f)
        val minY = top + 0.75f

        // Base "mass" is created by overlap of tongues; avoid extra bottom bands that look like separate layers.

        val wind01 = valueNoise1D(seed xor 0x12E15E35, x = 0.0f, t = tSec * 0.22f)
        val wind = (wind01 * 2f - 1f)

        // Make tongues overlap more so the bottom reads as one continuous flame.
        val backCount = max(2, (count * 0.65f).toInt())
        val frontCount = count + max(2, (count * 0.45f).toInt())

        // Back layer: fewer, larger, darker (adds mass).
        for (idx in 0 until backCount) {
            val u = (idx + 0.5f) / backCount.toFloat()
            val baseX = u * w
            val r = hash01(seed, idx * 43 + 7)
            val xNoise = valueNoise1D(seed xor 0x6D2B79F5, x = u * 1.7f, t = tSec * 0.10f)
            val x = (baseX + (xNoise - 0.5f) * cellW * 0.85f).coerceIn(0f, w)

            val hNoise = valueNoise1D(
                seed xor 0x85EBCA6B.toInt(),
                x = u * (1.9f + 1.4f * f),
                t = tSec * 0.28f
            )
            val peak = smooth01(((hNoise - 0.70f) / 0.30f).coerceIn(0f, 1f))
            val tongueH =
                (maxTongueH * (0.50f + 0.55f * hNoise + 0.25f * peak)).coerceIn(6f, maxTongueH)
            val tongueW = (tongueWBase * (1.30f + 0.85f * hNoise) * (0.90f + 0.20f * r)).coerceIn(
                14f,
                w * 0.72f
            )

            val phase = r * TAU
            val sway =
                (wind * tongueW * 0.12f + sin(tSec * (0.65f + 1.40f * s) + phase) * tongueW * 0.10f)
                    .coerceIn(-tongueW * 0.26f, tongueW * 0.26f)

            drawCartoonTongue(
                seed = seed,
                idx = 1000 + idx,
                centerX = (x + sway).coerceIn(0f, w),
                bottomY = bottom,
                width = tongueW,
                height = tongueH,
                minY = minY,
                tSec = tSec,
                speed = s,
                intensity = i,
                alphaMul = 0.70f,
                isBackLayer = true,
            )
        }

        // Front layer: more tongues, smaller, brighter (the "icon" tongues).
        for (idx in 0 until frontCount) {
            val u = (idx + 0.5f) / frontCount.toFloat()
            val baseX = u * w
            val r = hash01(seed, idx * 31 + 3)
            val xNoise =
                valueNoise1D(seed xor 0xC2B2AE35.toInt(), x = u * 3.2f + r * 0.9f, t = tSec * 0.22f)
            val x = (baseX + (xNoise - 0.5f) * cellW * (0.55f + 0.35f * f)).coerceIn(0f, w)

            val hNoise = valueNoise1D(
                seed xor 0x27D4EB2F,
                x = u * (3.0f + 3.0f * f),
                t = tSec * (0.42f + 0.35f * s)
            )
            val tongueH =
                (maxTongueH * (0.35f + 0.65f * hNoise.pow(1.15f))).coerceIn(5f, maxTongueH)
            val tongueW = (tongueWBase * (1.00f + 0.70f * hNoise) * (0.88f + 0.24f * r)).coerceIn(
                12f,
                w * 0.55f
            )

            val phase = (r * TAU) + idx * 0.33f
            val wobble = sin(tSec * (1.40f + 3.10f * s) + phase) * tongueW * 0.12f
            val gust = sin(tSec * (0.70f + 1.25f * s) + phase * 0.7f) * tongueW * 0.07f
            val sway =
                (wind * tongueW * 0.14f + wobble + gust).coerceIn(-tongueW * 0.28f, tongueW * 0.28f)

            drawCartoonTongue(
                seed = seed,
                idx = idx,
                centerX = (x + sway).coerceIn(0f, w),
                bottomY = bottom,
                width = tongueW,
                height = tongueH,
                minY = minY,
                tSec = tSec,
                speed = s,
                intensity = i,
                alphaMul = 1.0f,
                isBackLayer = false,
            )
        }

        // Subtle embers/particles: small spots that occasionally float up.
        val particleBlend = ((i - 0.25f) / 0.75f).coerceIn(0f, 1f)
        if (particleBlend > 0.01f) {
            val particleCount = max(4, (w / 140f).toInt() + 2)
            val baseAlpha = (0.03f + 0.10f * particleBlend).coerceIn(0.03f, 0.16f)
            val activeWindow = (0.07f + 0.14f * particleBlend).coerceIn(0.06f, 0.22f)
            val globalWind =
                (valueNoise1D(seed xor 0x22D3A3B1, x = 0.0f, t = tSec * 0.10f) * 2f - 1f)

            for (p in 0 until particleCount) {
                val base = hash01(seed xor 0x5A1F7A13, p * 31 + 7)
                val period = (2.6f - 1.2f * s).coerceIn(1.0f, 2.6f) * (0.85f + 0.60f * hash01(
                    seed,
                    p * 31 + 8
                ))
                val cycle = fract((tSec / period) + base)
                if (cycle > activeWindow) continue

                val u01 = (cycle / activeWindow).coerceIn(0f, 1f)
                val fade = (sin(u01 * PI.toFloat())).coerceIn(0f, 1f)

                val x0 = w * hash01(seed, p * 31 + 10)
                val driftLocal = (hash01(seed, p * 31 + 11) - 0.5f) * (w * 0.05f)
                val x = (x0 + (globalWind * w * 0.03f + driftLocal) * u01).coerceIn(0f, w)

                val rise = flameH * (0.55f + 0.60f * hash01(seed, p * 31 + 9))
                val y = (bottom - (u01.pow(1.35f) * rise)).coerceIn(top + 1f, bottom - 1f)

                val r =
                    (0.55f + 2.20f * hash01(seed, p * 31 + 12)) * (0.70f + 0.60f * particleBlend)
                val warm = hash01(seed, p * 31 + 13)
                val c = if (warm > 0.72f) Color(0xFFFFF8E1) else Color(0xFFFFD180)

                drawCircle(
                    color = c.copy(alpha = baseAlpha * fade),
                    radius = r,
                    center = Offset(x, y)
                )
            }
        }

        // Removed bottom straight ember lines (looked like a highlighted flat strip).
    }
}

private fun DrawScope.drawTongueLayers(
    seed: Int,
    idx: Int,
    centerX: Float,
    bottomY: Float,
    width: Float,
    height: Float,
    clipTopY: Float,
    tSec: Float,
    speed: Float,
    phase: Float,
    intensity: Float,
    baseTaper: Float,
    tipSplitCount: Int,
    tipSplit: Float,
) {
    val h = max(2f, height)
    val w = max(2f, width)

    val outerPath = buildTonguePath(
        seed = seed,
        idx = idx,
        centerX = centerX,
        bottomY = bottomY,
        width = w * 1.18f,
        height = h * 1.05f,
        minY = clipTopY,
        tSec = tSec,
        speed = speed,
        phase = phase,
        wobbleScale = 1.00f,
        baseTaper = baseTaper,
        tipSplitCount = tipSplitCount,
        tipSplit = tipSplit,
    )
    val innerPath = buildTonguePath(
        seed = seed,
        idx = idx + 101,
        centerX = centerX,
        bottomY = bottomY,
        width = w * 0.86f,
        height = h * 0.92f,
        minY = clipTopY,
        tSec = tSec,
        speed = speed * 1.05f,
        phase = phase + 0.7f,
        wobbleScale = 0.85f,
        baseTaper = baseTaper,
        tipSplitCount = tipSplitCount,
        tipSplit = tipSplit,
    )
    val corePath = buildTonguePath(
        seed = seed,
        idx = idx + 202,
        centerX = centerX,
        bottomY = bottomY,
        width = w * 0.55f,
        height = h * 0.78f,
        minY = clipTopY,
        tSec = tSec,
        speed = speed * 1.12f,
        phase = phase - 0.8f,
        wobbleScale = 0.70f,
        baseTaper = baseTaper,
        tipSplitCount = tipSplitCount,
        tipSplit = tipSplit,
    )

    val outerA = (0.10f + 0.22f * intensity).coerceIn(0.10f, 0.40f)
    val innerA = (0.14f + 0.28f * intensity).coerceIn(0.14f, 0.52f)
    val coreA = (0.12f + 0.32f * intensity).coerceIn(0.12f, 0.62f)

    val outerBrush = flameBrush(
        bottomY = bottomY,
        topY = bottomY - h * 1.10f,
        bottomColor = Color(0xFFFF4E00).copy(alpha = outerA),
        midColor = Color(0xFFFF9A00).copy(alpha = outerA * 0.92f),
    )
    val innerBrush = flameBrush(
        bottomY = bottomY,
        topY = bottomY - h * 1.00f,
        bottomColor = Color(0xFFFF7A00).copy(alpha = innerA),
        midColor = Color(0xFFFFD54F).copy(alpha = innerA * 0.95f),
    )
    val coreBrush = flameBrush(
        bottomY = bottomY,
        topY = bottomY - h * 0.90f,
        bottomColor = Color(0xFFFFF3E0).copy(alpha = coreA),
        midColor = Color(0xFFFFF8E1).copy(alpha = coreA * 0.92f),
    )

    drawPath(path = outerPath, brush = outerBrush)
    drawPath(path = innerPath, brush = innerBrush)
    drawPath(path = corePath, brush = coreBrush, blendMode = BlendMode.Plus)

    // Small highlight streak (makes the core feel "liquid hot").
    val strokeA = (0.05f + 0.12f * intensity).coerceIn(0.04f, 0.22f)
    drawPath(
        path = corePath,
        color = Color.White.copy(alpha = strokeA),
        style = Stroke(width = max(0.6f, w * 0.03f)),
    )

    // Rare core "flash" (very subtle): adds life without looking like noise.
    val flashNoise = valueNoise1D(seed xor 0x6A09E667, x = idx * 0.21f, t = tSec * 0.40f)
    val flash = smooth01(((flashNoise - 0.86f) / 0.12f).coerceIn(0f, 1f))
    if (flash > 0.01f) {
        val a = (0.02f + 0.10f * flash) * (0.35f + 0.65f * intensity)
        drawPath(
            path = corePath,
            color = Color(0xFFFFF8E1).copy(alpha = a.coerceIn(0f, 0.16f)),
            blendMode = BlendMode.Plus,
        )
    }
}

private fun DrawScope.drawCartoonTongue(
    seed: Int,
    idx: Int,
    centerX: Float,
    bottomY: Float,
    width: Float,
    height: Float,
    minY: Float,
    tSec: Float,
    speed: Float,
    intensity: Float,
    alphaMul: Float,
    isBackLayer: Boolean,
) {
    intensity.coerceIn(0f, 1f)
    // Tip wiggles more than the base (cartoon flame "waving").
    val tipNoise = valueNoise1D(
        seed xor 0xA341316C.toInt(),
        x = idx * 0.31f,
        t = tSec * (1.05f + 1.45f * speed)
    )
    val tipWobble =
        sin(tSec * (2.0f + 4.4f * speed) + idx * 0.7f) * width * 0.14f +
                (tipNoise - 0.5f) * width * 0.16f
    val tipX = (centerX + tipWobble).coerceIn(0f, Float.MAX_VALUE)
    val asym = (hash01(seed, idx * 29 + 11) - 0.5f) * 0.28f
    // Smaller roundness => less "dome", more flame tip.
    val tipRound = (0.02f + 0.03f * hash01(seed, idx * 29 + 12)).coerceIn(0.015f, 0.06f)

    val outer = buildCartoonFlamePath(
        centerX = centerX,
        tipX = tipX,
        bottomY = bottomY,
        width = width,
        height = height,
        minY = minY,
        asym = asym,
        tipRound = tipRound,
    )
    val inner = buildCartoonFlamePath(
        centerX = centerX,
        tipX = tipX - width * 0.02f,
        bottomY = bottomY,
        width = width * 0.42f,
        height = height * 0.62f,
        minY = minY,
        asym = asym * 0.45f,
        tipRound = tipRound * 0.75f,
    )

    // Cartoon palette (close to the icon): red -> orange -> yellow.
    // User requested minimal transparency: keep it essentially opaque.
    val aMul = alphaMul.coerceIn(0.60f, 1.25f)
    val outerA = (0.92f * aMul).coerceIn(0.70f, 1.0f)
    val innerA = (0.78f * aMul).coerceIn(0.55f, 0.95f)

    val outerTop = if (isBackLayer) Color(0xFFB71C1C) else Color(0xFFFF3B1D)
    val outerMid = if (isBackLayer) Color(0xFFD84315) else Color(0xFFFF5A00)
    val outerBot = if (isBackLayer) Color(0xFFFF6A00) else Color(0xFFFF8F00)

    val innerTop = Color(0xFFFFF3D0)
    val innerMid = Color(0xFFFFE082)
    val innerBot = Color(0xFFFFC400)

    val outerBrush = Brush.verticalGradient(
        colorStops = arrayOf(
            0.00f to outerTop.copy(alpha = outerA),
            0.55f to outerMid.copy(alpha = outerA),
            1.00f to outerBot.copy(alpha = outerA),
        ),
        startY = max(minY, bottomY - height),
        endY = bottomY,
    )
    val innerBrush = Brush.verticalGradient(
        colorStops = arrayOf(
            0.00f to innerTop.copy(alpha = innerA),
            0.65f to innerMid.copy(alpha = innerA),
            1.00f to innerBot.copy(alpha = innerA * 0.90f),
        ),
        startY = max(minY, bottomY - height * 0.62f),
        endY = bottomY,
    )

    // Avoid "yellow snowmen": no additive blending for the core and no outline stroke.
    drawPath(path = outer, brush = outerBrush)
    drawPath(path = inner, brush = innerBrush)
}

private fun buildCartoonFlamePath(
    centerX: Float,
    tipX: Float,
    bottomY: Float,
    width: Float,
    height: Float,
    minY: Float,
    asym: Float,
    tipRound: Float,
): Path {
    val tipY = max(minY, bottomY - height)

    // Less "blob", more flame: strong pinch + sharp tip (no dome).
    val a = asym.coerceIn(-0.35f, 0.35f)
    val pinch = (0.62f + 0.22f * (1f - tipRound)).coerceIn(0.55f, 0.88f)

    val baseHalfL = width * 0.58f * (1f + a * 0.45f)
    val baseHalfR = width * 0.58f * (1f - a * 0.45f)
    val pinchHalfL = width * (0.14f + 0.12f * (1f - pinch)) * (1f + a * 0.20f)
    val pinchHalfR = width * (0.14f + 0.12f * (1f - pinch)) * (1f - a * 0.20f)

    val leftBase = centerX - baseHalfL
    val rightBase = centerX + baseHalfR

    val yBulge = bottomY - height * 0.22f
    val yPinch = bottomY - height * (0.58f + 0.08f * pinch)
    val yNeck = bottomY - height * 0.82f

    val bulgeLx = centerX - baseHalfL * (0.95f + 0.05f * pinch)
    val bulgeRx = centerX + baseHalfR * (0.95f + 0.05f * pinch)

    val pinchLx = tipX - pinchHalfL
    val pinchRx = tipX + pinchHalfR

    return Path().apply {
        moveTo(leftBase, bottomY)
        cubicTo(
            leftBase, yBulge,
            bulgeLx, yBulge,
            pinchLx, yPinch
        )
        quadraticTo(tipX, tipY, pinchRx, yPinch)
        cubicTo(
            bulgeRx, yNeck,
            rightBase, yBulge,
            rightBase, bottomY
        )
        close()
    }
}

private fun flameBrush(
    bottomY: Float,
    topY: Float,
    bottomColor: Color,
    midColor: Color,
): Brush {
    val top = min(topY, bottomY)
    val bot = max(topY, bottomY)
    return Brush.verticalGradient(
        colorStops = arrayOf(
            0.00f to Color.Transparent,
            0.12f to midColor.copy(alpha = midColor.alpha * 0.70f),
            0.45f to midColor,
            1.00f to bottomColor,
        ),
        startY = top,
        endY = bot,
    )
}

private fun buildTonguePath(
    seed: Int,
    idx: Int,
    centerX: Float,
    bottomY: Float,
    width: Float,
    height: Float,
    minY: Float,
    tSec: Float,
    speed: Float,
    phase: Float,
    wobbleScale: Float,
    baseTaper: Float,
    tipSplitCount: Int,
    tipSplit: Float,
): Path {
    val segments = 16
    val ptsL = ArrayList<Offset>(segments + 1)
    val ptsR = ArrayList<Offset>(segments + 1)

    val tipPull = (0.02f + 0.05f * hash01(seed, idx * 19 + 77)).coerceIn(0.02f, 0.07f)
    val edgeFlickerFreq = 7.0f + 4.0f * hash01(seed, idx * 19 + 78)
    val edgeFlickerPhase = hash01(seed, idx * 19 + 79) * TAU
    val noiseOffset = hash01(seed, idx * 19 + 80) * 10f
    val asym = (hash01(seed, idx * 19 + 81) - 0.5f) * 0.22f // left/right asymmetry
    val curl = (hash01(seed, idx * 19 + 82) - 0.5f) * 0.55f // tip curl strength
    val bulgeAmt = (0.16f + 0.16f * hash01(seed, idx * 19 + 83)).coerceIn(0.12f, 0.32f)
    val windSeed = seed xor 0x1B56C4E9

    for (s in 0..segments) {
        val u = s / segments.toFloat() // 0..1 (bottom -> top)
        val y = max(minY, bottomY - u * height)

        val rise = (1f - u).coerceIn(0f, 1f)
        // Wider, more "flame-like" profile vs thin "cola" spikes.
        val decay = rise.pow(0.55f)
        val bulge = 1f + bulgeAmt * sin(u * PI.toFloat()) * (0.90f - 0.35f * u)
        val profile = (decay * bulge).coerceIn(0f, 1.35f)

        val n1 = valueNoise1D(
            seed = seed,
            x = (centerX * 0.035f) + (u * 2.8f) + noiseOffset,
            t = (tSec * (0.75f + 0.55f * speed)),
        )
        val n2 = valueNoise1D(
            seed = seed xor 0xB5297A4D.toInt(),
            x = (centerX * 0.055f) + (u * 5.1f) + (noiseOffset * 1.7f),
            t = (tSec * (0.95f + 0.65f * speed)),
        )

        val sway = sin(phase + (tSec * (0.85f + 0.75f * speed)) + (u * 6.2f)) * (width * 0.12f)
        val flutter =
            sin(edgeFlickerPhase + tSec * (1.2f + speed) + u * edgeFlickerFreq) * (width * 0.05f)
        val tipCurl =
            sin(phase * 0.7f + tSec * (1.05f + 0.35f * speed) + u * (8.5f + 2.0f * speed)) * (width * 0.08f)
        val curlWeight = (u * u).coerceIn(0f, 1f)

        // Random wind: slow gusts + per-tongue jitter, stronger at the tip.
        val wind01 = valueNoise1D(
            seed = windSeed,
            x = (idx * 0.13f) + noiseOffset,
            t = tSec * 0.14f,
        )
        val wind = (wind01 * 2f - 1f)
        val gust01 = valueNoise1D(
            seed = windSeed xor 0x7F4A7C15,
            x = (idx * 0.31f) + (u * 0.85f) + noiseOffset,
            t = tSec * (0.55f + 0.25f * speed),
        )
        val gust = (gust01 * 2f - 1f)
        val windWeight = u.pow(1.65f)
        val windShift = (wind * width * 0.18f + gust * width * 0.06f) * windWeight * wobbleScale

        val x = centerX + wobbleScale * decay * (
                sway + flutter +
                        (n1 - 0.5f) * width * 0.20f +
                        (n2 - 0.5f) * width * 0.10f +
                        tipCurl * curl * curlWeight
                ) + windShift

        val edgePulse =
            0.84f + 0.24f * sin(edgeFlickerPhase + tSec * (1.0f + 0.6f * speed) + u * (8.0f + 2.0f * speed))
        val baseRamp = 0.18f
        val baseFactor =
            if (u < baseRamp) lerp(
                baseTaper.coerceIn(0.02f, 1f),
                1f,
                smooth01(u / baseRamp)
            ) else 1f

        val rBase = (width * 0.5f) * profile * edgePulse * baseFactor
        val rL = max(0.40f, rBase * (1f + asym * 0.65f))
        val rR = max(0.40f, rBase * (1f - asym * 0.65f))

        ptsL.add(Offset(x - rL, y))
        ptsR.add(Offset(x + rR, y))
    }

    val leftTop = ptsL.last()
    val rightTop = ptsR.last()
    val tipBaseY = max(minY, min(leftTop.y, rightTop.y) - (height * tipPull))
    val tipCenterX = (leftTop.x + rightTop.x) * 0.5f
    val split = tipSplit.coerceIn(0f, 1f)
    val splitCount = if (split > 0.12f) tipSplitCount.coerceIn(1, 3) else 1

    return Path().apply {
        val first = ptsL.first()
        moveTo(first.x, first.y)
        quadraticPolyline(ptsL)
        if (splitCount == 1) {
            val tip = Offset(x = tipCenterX, y = tipBaseY)
            quadraticTo(tip.x, tip.y, rightTop.x, rightTop.y)
        } else {
            val spread = (width * 0.22f) * (0.70f + 0.30f * split)
            val notchY = max(minY, tipBaseY + height * (0.05f + 0.03f * (1f - split)))
            if (splitCount == 2) {
                val mid = Offset(tipCenterX, notchY)
                val tipL = Offset(tipCenterX - spread, tipBaseY)
                val tipR = Offset(tipCenterX + spread, tipBaseY)
                quadraticTo(tipL.x, tipL.y, mid.x, mid.y)
                quadraticTo(tipR.x, tipR.y, rightTop.x, rightTop.y)
            } else {
                val mid1 = Offset(tipCenterX - spread * 0.33f, notchY)
                val mid2 = Offset(tipCenterX + spread * 0.33f, notchY)
                val tipL = Offset(tipCenterX - spread, tipBaseY)
                val tipM = Offset(tipCenterX, tipBaseY)
                val tipR = Offset(tipCenterX + spread, tipBaseY)
                quadraticTo(tipL.x, tipL.y, mid1.x, mid1.y)
                quadraticTo(tipM.x, tipM.y, mid2.x, mid2.y)
                quadraticTo(tipR.x, tipR.y, rightTop.x, rightTop.y)
            }
        }
        quadraticPolylineReverse(ptsR)
        close()
    }
}

private const val TAU: Float = (PI.toFloat() * 2f)

private fun fract(x: Float): Float = x - floor(x)

private fun hash01(seed: Int, v: Int): Float {
    var x = seed xor (v * 0x9E3779B9.toInt())
    x = x xor (x ushr 16)
    x *= 0x7FEB352D
    x = x xor (x ushr 15)
    x *= 0x846CA68B.toInt()
    x = x xor (x ushr 16)
    val u = (x ushr 8) and 0x00FFFFFF
    return u / 0x01000000.toFloat()
}

/**
 * 1D value-noise in [0..1] with time input; cheap + deterministic (no allocations).
 */
private fun valueNoise1D(seed: Int, x: Float, t: Float): Float {
    val p = x + t * 0.55f
    val x0 = floor(p)
    val x1 = x0 + 1f
    val f = p - x0
    val u = f * f * (3f - 2f * f) // smoothstep

    val h0 = hash01(seed, x0.toInt() * 374761393)
    val h1 = hash01(seed, x1.toInt() * 374761393)
    return lerp(h0, h1, u)
}

private fun lerp(a: Float, b: Float, t: Float): Float = a + (b - a) * t

private fun smooth01(x: Float): Float {
    val t = x.coerceIn(0f, 1f)
    return t * t * (3f - 2f * t)
}

private fun Path.quadraticPolyline(points: List<Offset>) {
    if (points.size < 2) return
    for (i in 1 until points.size - 1) {
        val p0 = points[i]
        val p1 = points[i + 1]
        val mx = (p0.x + p1.x) * 0.5f
        val my = (p0.y + p1.y) * 0.5f
        quadraticTo(p0.x, p0.y, mx, my)
    }
    val last = points.last()
    lineTo(last.x, last.y)
}

private fun Path.quadraticPolylineReverse(points: List<Offset>) {
    if (points.size < 2) return
    for (i in points.size - 2 downTo 1) {
        val p0 = points[i]
        val p1 = points[i - 1]
        val mx = (p0.x + p1.x) * 0.5f
        val my = (p0.y + p1.y) * 0.5f
        quadraticTo(p0.x, p0.y, mx, my)
    }
    val first = points.first()
    lineTo(first.x, first.y)
}
