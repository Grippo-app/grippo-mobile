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
    val timeScale = ((0.22f + 1.55f * s) * (1f + s) * (1f + s)).coerceIn(0.15f, 7.2f)
    val tSec = (timeNanos / 1_000_000_000f) * timeScale

    clipRect(left = 0f, top = top, right = w, bottom = bottom) {
        // === IMPROVED FIRE RENDERING using advanced techniques ===

        // 1) Base merging layer: dense overlapping base to unify tongue bottoms
        drawBaseMergingLayer(
            seed = seed,
            w = w,
            bottom = bottom,
            flameH = flameH,
            top = top,
            tSec = tSec,
            intensity = i,
            speed = s,
            frequency = f
        )

        // 2) Enhanced layered flame tongues with better turbulence
        drawEnhancedFlameTongues(
            seed = seed,
            w = w,
            bottom = bottom,
            flameH = flameH,
            top = top,
            tSec = tSec,
            intensity = i,
            speed = s,
            frequency = f
        )

        // 3) Advanced particle system with embers and sparks
        drawAdvancedParticles(
            seed = seed,
            w = w,
            h = h,
            bottom = bottom,
            flameH = flameH,
            top = top,
            tSec = tSec,
            intensity = i,
            speed = s
        )
    }
}

// === ADVANCED FIRE RENDERING FUNCTIONS (Claude Sonnet 4.5 Enhanced) ===

/**
 * Draws dense overlapping base layer to create visual merging at tongue bottoms.
 * Uses many small overlapping shapes (not a flat band) for organic look.
 */
private fun DrawScope.drawBaseMergingLayer(
    seed: Int,
    w: Float,
    bottom: Float,
    flameH: Float,
    top: Float,
    tSec: Float,
    intensity: Float,
    speed: Float,
    frequency: Float,
) {
    // Base merging zone: lower 40% of flame height (MORE visible)
    val mergeH = (flameH * 0.40f).coerceIn(10f, flameH * 0.50f)
    val mergeTop = (bottom - mergeH).coerceAtLeast(top)

    // VERY dense array of overlapping "base bumps" to create VISIBLE unified bottom
    val bumpCount = max(12, (w / 35f * (1.0f + 0.7f * frequency)).toInt())

    for (idx in 0 until bumpCount) {
        val u = idx / (bumpCount - 1f).coerceAtLeast(1f)
        val xBase = u * w

        // Minimal horizontal wiggle (more stable base)
        val xNoise = valueNoise1D(seed xor 0x7A2C4E91.toInt(), x = u * 2.5f, t = tSec * 0.16f)
        val x = (xBase + (xNoise - 0.5f) * (w / bumpCount) * 0.4f).coerceIn(0f, w)

        // Variable height for organic edge
        val hNoise = valueNoise1D(seed xor 0x3F8D6B12.toInt(), x = u * 3.2f, t = tSec * 0.22f)
        val h = (mergeH * (0.80f + 0.30f * hNoise)).coerceIn(8f, mergeH * 1.15f)

        // VERY wide overlapping width (creates SOLID coverage at base)
        val bumpW =
            (w / bumpCount * 2.20f * (0.90f + 0.35f * smooth01(intensity))).coerceIn(25f, w * 0.40f)

        // Draw as vertical gradient oval/circle
        val bumpTop = max(mergeTop, bottom - h)
        val radiusX = bumpW * 0.5f
        val radiusY = h * 0.5f

        // Color gradient (red-orange-yellow) with MUCH HIGHER alpha (more visible)
        val alphaBase = (0.65f + 0.30f * intensity).coerceIn(0.60f, 0.98f)
        val gradient = Brush.verticalGradient(
            colorStops = arrayOf(
                0.00f to Color(0xFFD84315).copy(alpha = alphaBase * 0.50f),
                0.35f to Color(0xFFFF6A00).copy(alpha = alphaBase * 0.80f),
                0.70f to Color(0xFFFF8F00).copy(alpha = alphaBase * 0.95f),
                1.00f to Color(0xFFFFC400).copy(alpha = alphaBase),
            ),
            startY = bumpTop,
            endY = bottom
        )

        // Draw ellipse/circle bump
        drawOval(
            brush = gradient,
            topLeft = Offset(x - radiusX, bumpTop),
            size = androidx.compose.ui.geometry.Size(radiusX * 2f, radiusY * 2f)
        )
    }
}

/**
 * Holds parameters for a single flame tongue to be drawn.
 */
private data class TongueParams(
    val idx: Int,
    val centerX: Float,
    val width: Float,
    val height: Float,
    val alphaMul: Float,
    val isBackLayer: Boolean,
    val zOrder: Float, // Depth for sorting (lower = back, higher = front)
)

/**
 * Draws refined flame tongues with natural overlapping (not "ladder" effect).
 * Computes all tongues first, sorts by depth (z-order), then draws back-to-front.
 */
private fun DrawScope.drawEnhancedFlameTongues(
    seed: Int,
    w: Float,
    bottom: Float,
    flameH: Float,
    top: Float,
    tSec: Float,
    intensity: Float,
    speed: Float,
    frequency: Float,
) {
    val minY = top + 0.75f
    val maxTongueH = (flameH * 0.98f).coerceAtLeast(1f)

    // More tongues = better overlap = unified base
    val baseCount = max(5, (w / 75f * (0.8f + 0.5f * frequency)).toInt())

    // Gentle global wind (not extreme turbulence)
    val wind01 = valueNoise1D(seed xor 0x12E15E35, x = 0.0f, t = tSec * 0.18f)
    val wind = (wind01 * 2f - 1f) * 0.6f

    // Collect all tongue parameters first
    val tongues = mutableListOf<TongueParams>()

    // Back layer: creates depth and fills gaps
    val backCount = max(4, (baseCount * 0.75f).toInt())
    for (idx in 0 until backCount) {
        val u = (idx + 0.5f) / backCount.toFloat()

        // Moderate position variation (not extreme FBM chaos)
        val xNoise = valueNoise1D(seed xor 0x6D2B79F5, x = u * 2.0f, t = tSec * 0.14f)
        val x = (u * w + (xNoise - 0.5f) * w * 0.08f).coerceIn(0f, w)

        // Smooth height variation
        val hNoise = fbm1D(
            seed xor 0x85EBCA6B.toInt(),
            x = u * (2.2f + 1.2f * frequency),
            t = tSec * 0.32f,
            octaves = 2
        )
        val tongueH = (maxTongueH * (0.60f + 0.45f * hNoise)).coerceIn(10f, maxTongueH)

        // Controlled width (ensures overlap)
        val baseW = (w / backCount * 1.60f * (0.75f + 0.45f * smooth01(intensity)))
        val tongueW = (baseW * (1.05f + 0.35f * hNoise)).coerceIn(18f, w * 0.65f)

        // SLOWER gentle sway (reduced amplitude)
        val sway =
            (wind * tongueW * 0.06f + sin(tSec * (0.70f + 1.30f * speed) + idx * 0.6f) * tongueW * 0.04f).coerceIn(
                -tongueW * 0.12f,
                tongueW * 0.12f
            )

        // Z-order: back layer gets lower values (0.0-0.4 range) with randomization
        val zBase = hash01(seed, 1000 + idx) * 0.4f

        tongues.add(
            TongueParams(
                idx = 1000 + idx,
                centerX = (x + sway).coerceIn(0f, w),
                width = tongueW,
                height = tongueH,
                alphaMul = 0.75f,
                isBackLayer = true,
                zOrder = zBase,
            )
        )
    }

    // Front layer: visual definition
    val frontCount = baseCount + max(3, (baseCount * 0.55f).toInt())
    for (idx in 0 until frontCount) {
        val u = (idx + 0.5f) / frontCount.toFloat()

        // Moderate position offset
        val xNoise =
            valueNoise1D(seed xor 0xC2B2AE35.toInt(), x = u * 3.5f + idx * 0.09f, t = tSec * 0.22f)
        val x = (u * w + (xNoise - 0.5f) * w * (0.06f + 0.08f * frequency)).coerceIn(0f, w)

        // Smooth height with some peaks
        val hNoise = fbm1D(
            seed xor 0x27D4EB2F,
            x = u * (3.5f + 2.5f * frequency),
            t = tSec * (0.45f + 0.40f * speed),
            octaves = 2
        )
        val tongueH = (maxTongueH * (0.45f + 0.60f * hNoise.pow(1.08f))).coerceIn(8f, maxTongueH)

        // Controlled width
        val baseW = (w / baseCount * 1.40f * (0.70f + 0.40f * smooth01(intensity)))
        val tongueW = (baseW * (0.95f + 0.45f * hNoise)).coerceIn(16f, w * 0.50f)

        // SLOWER combined sway (reduced amplitude)
        val wobbleFast = sin(tSec * (1.60f + 3.20f * speed) + idx * 0.50f) * tongueW * 0.05f
        val wobbleSlow =
            valueNoise1D(seed xor 0x165667B1, x = idx * 0.21f, t = tSec * (0.70f + 1.05f * speed))
        val sway =
            (wind * tongueW * 0.07f + wobbleFast + (wobbleSlow - 0.5f) * tongueW * 0.04f).coerceIn(
                -tongueW * 0.14f,
                tongueW * 0.14f
            )

        // Z-order: front layer gets higher values (0.6-1.0 range) with randomization
        val zBase = 0.6f + hash01(seed, idx) * 0.4f

        tongues.add(
            TongueParams(
                idx = idx,
                centerX = (x + sway).coerceIn(0f, w),
                width = tongueW,
                height = tongueH,
                alphaMul = 1.0f,
                isBackLayer = false,
                zOrder = zBase,
            )
        )
    }

    // Sort by z-order (back to front) for natural overlapping
    tongues.sortBy { it.zOrder }

    // Draw all tongues in sorted order
    tongues.forEach { tongue ->
        drawRefinedCartoonTongue(
            seed = seed,
            idx = tongue.idx,
            centerX = tongue.centerX,
            bottomY = bottom,
            width = tongue.width,
            height = tongue.height,
            minY = minY,
            flameH = flameH,
            tSec = tSec,
            speed = speed,
            intensity = intensity,
            alphaMul = tongue.alphaMul,
            isBackLayer = tongue.isBackLayer,
        )
    }
}

/**
 * Advanced particle system with improved physics and visual variety.
 */
private fun DrawScope.drawAdvancedParticles(
    seed: Int,
    w: Float,
    h: Float,
    bottom: Float,
    flameH: Float,
    top: Float,
    tSec: Float,
    intensity: Float,
    speed: Float,
) {
    val particleBlend = ((intensity - 0.22f) / 0.78f).coerceIn(0f, 1f)
    if (particleBlend < 0.01f) return

    val particleCount = max(6, (w / 110f * (1.0f + 0.8f * particleBlend)).toInt())
    val baseAlpha = (0.05f + 0.14f * particleBlend).coerceIn(0.05f, 0.24f)
    val activeWindow = (0.10f + 0.18f * particleBlend).coerceIn(0.08f, 0.30f)

    // Global wind field
    val globalWind = fbm1D(seed xor 0x22D3A3B1, x = 0.0f, t = tSec * 0.12f, octaves = 2) * 2f - 1f

    for (p in 0 until particleCount) {
        val base = hash01(seed xor 0x5A1F7A13, p * 37 + 11)
        val period =
            (2.2f - 1.0f * speed).coerceIn(0.9f, 2.2f) * (0.80f + 0.65f * hash01(seed, p * 37 + 13))
        val cycle = fract((tSec / period) + base)
        if (cycle > activeWindow) continue

        val u01 = (cycle / activeWindow).coerceIn(0f, 1f)
        val fade = (sin(u01 * PI.toFloat())).coerceIn(0f, 1f)

        // Starting position
        val x0 = w * hash01(seed, p * 37 + 17)

        // Turbulent drift (more realistic than linear drift)
        val driftTurb = fbm1D(
            seed xor 0x9FA8C2D1.toInt(),
            x = p * 0.23f,
            t = tSec * 0.35f + u01 * 2.5f,
            octaves = 2
        )
        val driftX = (hash01(seed, p * 37 + 19) - 0.5f) * w * 0.08f + (driftTurb - 0.5f) * w * 0.06f
        val x = (x0 + globalWind * w * 0.04f * u01 + driftX * u01).coerceIn(0f, w)

        // Rising with acceleration (realistic physics)
        val rise = flameH * (0.60f + 0.70f * hash01(seed, p * 37 + 23))
        val y = (bottom - (u01.pow(1.25f) * rise)).coerceIn(top + 1f, bottom - 1f)

        // Size variation
        val r = (0.70f + 2.60f * hash01(seed, p * 37 + 29)) * (0.75f + 0.65f * particleBlend)

        // Color variation (temperature-based)
        val temp = hash01(seed, p * 37 + 31)
        val c = when {
            temp > 0.85f -> Color(0xFFFFFBF3) // Hot white
            temp > 0.65f -> Color(0xFFFFF3D0) // Warm white
            temp > 0.40f -> Color(0xFFFFE082) // Yellow
            else -> Color(0xFFFFB350) // Orange
        }

        // Slight flicker for life
        val flicker = 0.85f + 0.15f * sin(tSec * 8.5f + p * 1.7f)

        drawCircle(
            color = c.copy(alpha = baseAlpha * fade * flicker),
            radius = r,
            center = Offset(x, y)
        )
    }
}

/**
 * Refined cartoon tongue with tighter layer gaps and smart base merging.
 * Layers are closer together (less "gap"), and alpha increases near the base
 * to create smooth merging between tongues.
 */
private fun DrawScope.drawRefinedCartoonTongue(
    seed: Int,
    idx: Int,
    centerX: Float,
    bottomY: Float,
    width: Float,
    height: Float,
    minY: Float,
    flameH: Float,
    tSec: Float,
    speed: Float,
    intensity: Float,
    alphaMul: Float,
    isBackLayer: Boolean,
) {
    val i = intensity.coerceIn(0f, 1f)

    // Moderate tip wobble (not excessive)
    val tipNoise = valueNoise1D(
        seed xor 0xA341316C.toInt(),
        x = idx * 0.33f,
        t = tSec * (1.15f + 1.50f * speed)
    )
    // SLOWER tip wobble (reduced amplitude for less side-to-side motion)
    val tipWobble =
        (tipNoise - 0.5f) * width * 0.08f + sin(tSec * (2.10f + 4.50f * speed) + idx * 0.75f) * width * 0.07f
    val tipX = (centerX + tipWobble).coerceIn(0f, Float.MAX_VALUE)

    val asymBase = (hash01(seed, idx * 29 + 11) - 0.5f) * 0.30f
    // MUCH sharper tips (reduced from 0.018-0.062 to 0.005-0.020)
    val tipRoundBase = (0.005f + 0.015f * hash01(seed, idx * 29 + 12)).coerceIn(0.005f, 0.020f)

    // Controlled per-layer variation (not chaotic, but enough for interest)
    val nMidW = valueNoise1D(seed xor 0xC2B2AE35.toInt(), x = idx * 0.23f, t = tSec * 0.58f)
    val nMidH = valueNoise1D(seed xor 0x85EBCA6B.toInt(), x = idx * 0.29f, t = tSec * 0.52f)
    val nInW = valueNoise1D(seed xor 0x27D4EB2F, x = idx * 0.21f, t = tSec * 0.72f)
    val nInH = valueNoise1D(seed xor 0x165667B1, x = idx * 0.31f, t = tSec * 0.66f)

    // TIGHTER gaps between layers (mid closer to outer, inner closer to mid)
    val midWScale = (0.68f + 0.18f * nMidW).coerceIn(0.64f, 0.88f)  // was 0.44-0.92
    val midHScale = (0.72f + 0.20f * nMidH).coerceIn(0.68f, 0.96f)  // was 0.50-1.00
    val innerWScale = (0.42f + 0.18f * nInW).coerceIn(0.38f, 0.62f) // was 0.20-0.62
    val innerHScale = (0.58f + 0.24f * nInH).coerceIn(0.52f, 0.88f) // was 0.32-1.00

    val midTipX = tipX + (nMidW - 0.5f) * width * 0.10f
    val innerTipX = tipX + (nInW - 0.5f) * width * 0.12f

    val asymMid = (asymBase + (nMidH - 0.5f) * 0.20f).coerceIn(-0.42f, 0.42f)
    val asymInner = (asymBase + (nInH - 0.5f) * 0.22f).coerceIn(-0.45f, 0.45f)
    // Sharper tips for mid and inner layers too
    val tipRoundMid = (tipRoundBase * (0.85f + 0.40f * nMidH)).coerceIn(0.004f, 0.025f)
    val tipRoundInner = (tipRoundBase * (0.75f + 0.50f * nInH)).coerceIn(0.003f, 0.028f)

    // Build three layers
    val outer = buildCartoonFlamePath(
        centerX = centerX,
        tipX = tipX,
        bottomY = bottomY,
        width = width,
        height = height,
        minY = minY,
        asym = asymBase,
        tipRound = tipRoundBase,
    )
    val mid = buildCartoonFlamePath(
        centerX = centerX,
        tipX = midTipX,
        bottomY = bottomY,
        width = width * midWScale,
        height = height * midHScale,
        minY = minY,
        asym = asymMid,
        tipRound = tipRoundMid,
    )
    val inner = buildCartoonFlamePath(
        centerX = centerX,
        tipX = innerTipX,
        bottomY = bottomY,
        width = width * innerWScale,
        height = height * innerHScale,
        minY = minY,
        asym = asymInner,
        tipRound = tipRoundInner,
    )

    // Alpha values (base merging is handled by separate layer, not alpha tricks)
    val aMul = alphaMul.coerceIn(0.42f, 1.28f)
    val outerA = ((0.48f + 0.38f * i) * aMul).coerceIn(0.32f, 0.92f)
    val midA = ((0.42f + 0.42f * i) * aMul).coerceIn(0.28f, 0.94f)
    val innerA = ((0.32f + 0.45f * i) * aMul).coerceIn(0.22f, 0.92f)

    // Refined color palette (closer to cartoon fire emoji)
    val outerTop = if (isBackLayer) Color(0xFF8B0000) else Color(0xFFFF2D1A)
    val outerMid = if (isBackLayer) Color(0xFFB71C1C) else Color(0xFFFF3B1D)
    val outerBot = if (isBackLayer) Color(0xFFD84315) else Color(0xFFFF5A00)

    val midTop = if (isBackLayer) Color(0xFFFF5A00) else Color(0xFFFF6A00)
    val midMid = if (isBackLayer) Color(0xFFFF6A00) else Color(0xFFFF8F00)
    val midBot = if (isBackLayer) Color(0xFFFF8F00) else Color(0xFFFFB300)

    val innerTop = Color(0xFFFFF3D0)
    val innerMid = Color(0xFFFFE698)
    val innerBot = Color(0xFFFFD970)

    val outerBrush = Brush.verticalGradient(
        colorStops = arrayOf(
            0.00f to outerTop.copy(alpha = outerA * 0.72f),
            0.55f to outerMid.copy(alpha = outerA),
            1.00f to outerBot.copy(alpha = outerA),
        ),
        startY = max(minY, bottomY - height),
        endY = bottomY,
    )
    val midBrush = Brush.verticalGradient(
        colorStops = arrayOf(
            0.00f to midTop.copy(alpha = midA * 0.72f),
            0.55f to midMid.copy(alpha = midA),
            1.00f to midBot.copy(alpha = midA),
        ),
        startY = max(minY, bottomY - height * 0.85f),
        endY = bottomY,
    )
    val innerBrush = Brush.verticalGradient(
        colorStops = arrayOf(
            0.00f to innerTop.copy(alpha = innerA * 0.65f),
            0.65f to innerMid.copy(alpha = innerA),
            1.00f to innerBot.copy(alpha = innerA * 0.94f),
        ),
        startY = max(minY, bottomY - height * 0.70f),
        endY = bottomY,
    )

    drawPath(path = outer, brush = outerBrush)
    drawPath(path = mid, brush = midBrush)
    drawPath(path = inner, brush = innerBrush)
}

/**
 * Fractal Brownian Motion (FBM) - multi-octave noise for natural-looking turbulence.
 * This is the key to making fire look organic and chaotic.
 */
private fun fbm1D(seed: Int, x: Float, t: Float, octaves: Int): Float {
    var value = 0f
    var amplitude = 0.5f
    var frequency = 1.0f
    var maxValue = 0f

    for (i in 0 until octaves) {
        value += amplitude * valueNoise1D(
            seed xor (i * 0x9E3779B9.toInt()),
            x = x * frequency,
            t = t * frequency
        )
        maxValue += amplitude
        amplitude *= 0.5f
        frequency *= 2.0f
    }

    return (value / maxValue).coerceIn(0f, 1f)
}

// === END OF ADVANCED FIRE RENDERING FUNCTIONS ===

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
