package com.grippo.design.components.modifiers

import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.IntSize
import kotlin.math.ceil
import kotlin.math.max
import kotlin.math.pow
import kotlin.math.roundToInt

public fun Modifier.fire(intensity: Float = 1f): Modifier = composed {
    val seed = remember { (kotlin.random.Random.nextInt() xor 0x6D2B79F5) }
    val sim = remember { DoomFireSim(seed = seed) }

    var hostSize by remember { mutableStateOf(IntSize.Zero) }
    var frameTick by remember { mutableIntStateOf(0) }

    val intensityClamped = intensity.coerceIn(0f, 1f)
    val intensityState by rememberUpdatedState(intensityClamped)

    LaunchedEffect(hostSize) {
        if (hostSize.width <= 0 || hostSize.height <= 0) return@LaunchedEffect

        var frame = 0
        while (true) {
            withFrameNanos {
                val i = intensityState
                val fireHeightPx = (hostSize.height.toFloat() * i).coerceAtLeast(0f)

                sim.resize(
                    hostWidthPx = hostSize.width,
                    fireHeightPx = fireHeightPx
                )

                sim.step(frame = frame++, intensity = i)
                frameTick++
            }
        }
    }

    this
        .onSizeChanged { hostSize = it }
        .drawWithContent {
            drawDoomFireLayer(
                sim = sim,
                intensity = intensityClamped,
                invalidate = frameTick
            )
            drawContent()
        }
}

private fun DrawScope.drawDoomFireLayer(
    sim: DoomFireSim,
    intensity: Float,
    invalidate: Int
) {
    if (invalidate < 0) return

    val w = size.width
    val h = size.height
    if (w <= 2f || h <= 2f) return

    val gridW = sim.gridWidth
    val gridH = sim.gridHeight
    if (gridW <= 0 || gridH <= 0) return

    val i = intensity.coerceIn(0f, 1f)
    if (i <= 0.001f) return

    val flameH = (h * i).coerceAtLeast(1f)
    val top = h - flameH

    val cellW = w / gridW.toFloat()
    val cellH = flameH / gridH.toFloat()

    val last = firePalette.lastIndex.coerceAtLeast(1)
    val paletteBias = (last * 0.22f).roundToInt().coerceIn(0, last)

    val drawThreshold = (5 + (1f - i) * 3f).roundToInt().coerceIn(4, 9)

    val baseGlowAlphaTop = (0.00f + 0.12f * i).coerceIn(0.00f, 0.12f)
    val baseGlowAlphaBottom = (0.18f + 0.22f * i).coerceIn(0.18f, 0.40f)

    val baseGlow = Brush.verticalGradient(
        colorStops = arrayOf(
            0.00f to Color.Transparent,
            0.55f to Color(0xFFFF6A00).copy(alpha = glowTopA),
            0.78f to Color(0xFFFF7A00).copy(alpha = glowBottomA * 0.85f),
            1.00f to Color(0xFFFFC400).copy(alpha = glowBottomA),
        ),
        startY = top,
        endY = bottom,
    )

    clipRect(left = 0f, top = top, right = w, bottom = bottom) {
        drawRect(brush = baseGlow)

        // Fewer tongues, but thicker. `frequency` controls density. `intensity` also affects width.
        // - lower frequency -> fewer tongues, much thicker
        // - higher frequency -> more tongues, slightly thinner
        val density = (0.70f + 0.80f * f).coerceIn(0.60f, 1.50f)
        // Stronger impact with a non-linear curve:
        // - low intensity -> noticeably thinner (avoid "too wide" at ~0.2)
        // - high intensity -> thicker, but capped (max ~25% narrower vs older impl).
        val intensityWidth = (0.30f + 1.00f * smooth01(i)).coerceIn(0.30f, 1.20f)
        val minTongueW = (5f + 10f * i).coerceIn(5f, 13f)
        val tongueBaseW = (((flameH * 0.80f) / density) * intensityWidth).coerceIn(minTongueW, 58f)
        val spacing = tongueBaseW * (1.35f - 0.55f * f)
        val count = max(3, ceil(w / spacing).toInt())
        val maxTongueH = (flameH * 0.98f).coerceAtLeast(1f)

        // Soft base layer so the very bottom feels like one flame, not many isolated tongues.
        // Enabled only at moderate+ intensity to avoid "mush" when fire is small.
        val baseBlend = ((i - 0.18f) / 0.82f).coerceIn(0f, 1f)
        if (baseBlend > 0f) {
            val bandH = (flameH * (0.18f + 0.26f * baseBlend)).coerceIn(8f, flameH * 0.52f)
            val bandTop = (bottom - bandH).coerceAtLeast(top)
            val bandA = (0.10f + 0.28f * baseBlend).coerceIn(0.10f, 0.42f)
            val baseBand = Brush.verticalGradient(
                colorStops = arrayOf(
                    0.00f to Color(0xFFFF6A00).copy(alpha = bandA * 0.35f),
                    0.55f to Color(0xFFFF8F00).copy(alpha = bandA * 0.70f),
                    1.00f to Color(0xFFFFC400).copy(alpha = bandA),
                ),
                startY = bandTop,
                endY = bottom,
            )
            // Wave the top edge a bit, so it feels like a single fire-front rather than a flat band.
            val front = Path().apply {
                val seg = 22
                moveTo(0f, bottom)
                lineTo(0f, bandTop)
                for (s in 0..seg) {
                    val xu = s / seg.toFloat()
                    val x = xu * w
                    val n = valueNoise1D(seed xor 0x3C6EF372.toInt(), x = xu * 2.6f, t = tSec * 0.22f)
                    val y = bandTop + (1f - n) * (bandH * (0.10f + 0.30f * baseBlend))
                    lineTo(x, y)
                }
                lineTo(w, bottom)
                close()
            }
            drawPath(path = front, brush = baseBand, blendMode = BlendMode.Plus)
        }

        // Heat-haze / shimmer just above the flame top (very subtle).
        val hazeBlend = ((i - 0.28f) / 0.72f).coerceIn(0f, 1f)
        if (hazeBlend > 0.01f) {
            val hazeH = (flameH * (0.10f + 0.16f * hazeBlend)).coerceIn(10f, flameH * 0.35f)
            val hazeTop = (top - hazeH * 0.55f).coerceAtLeast(0f)
            val hazeBottom = (top + hazeH).coerceAtMost(bottom)
            clipRect(left = 0f, top = hazeTop, right = w, bottom = hazeBottom) {
                val lines = 4
                for (k in 0 until lines) {
                    val phase = k * 1.7f
                    val y0 = top + hazeH * (0.15f + 0.22f * k)
                    val a = (0.012f + 0.020f * hazeBlend).coerceIn(0.012f, 0.045f)
                    val col = Color(0xFFFFE0B2).copy(alpha = a)
                    val path = Path().apply {
                        val seg = 28
                        moveTo(0f, y0)
                        for (s in 1..seg) {
                            val xu = s / seg.toFloat()
                            val x = xu * w
                            val n = valueNoise1D(seed xor 0xA341316C.toInt(), x = xu * 3.2f + phase, t = tSec * 0.35f)
                            val y = y0 + (n - 0.5f) * hazeH * 0.30f
                            lineTo(x, y)
                        }
                    }
                    drawPath(path = path, color = col, style = Stroke(width = 1.0f + 0.6f * k), blendMode = BlendMode.Plus)
                }
            }
        }

        for (idx in 0 until count) {
            val u = (idx + 0.5f) / count.toFloat()
            // Gust-field across X breaks the "even spacing" look (groups of tongues grow/shrink together).
            val gustField = valueNoise1D(seed xor 0xC2B2AE35.toInt(), x = u * 2.3f, t = tSec * 0.18f)
            val gustX = (gustField * 2f - 1f)
            val xJitter = gustX * spacing * 0.22f
            val xBase = (u * w + xJitter).coerceIn(0f, w)

            val r1 = hash01(seed, idx * 7 + 1)
            val r2 = hash01(seed, idx * 7 + 2)
            val r3 = hash01(seed, idx * 7 + 3)
            val r4 = hash01(seed, idx * 7 + 4)
            val r5 = hash01(seed, idx * 7 + 5)
            val r6 = hash01(seed, idx * 7 + 6)

            // Keep tongues inside the flame area (no top clipping).
            val baseH = flameH * (0.42f + 0.58f * r1)
            // Slightly narrower at low intensity; also reduce width variance to avoid "fat spikes".
            val widthIntensity = (0.86f + 0.14f * i).coerceIn(0.86f, 1.0f)
            val baseW = tongueBaseW * (0.74f + 0.86f * r2) * widthIntensity * (0.92f + 0.18f * gustField)
            val speed = 0.85f + 1.55f * r3
            val phase = r4 * TAU

            // Branches shouldn't "pop" in/out. Use slow noise for persistence + smooth presence.
            val branchNoise = valueNoise1D(
                seed = seed xor 0x4F1BBCDC,
                x = (idx * 0.31f) + r5 * 3.0f,
                t = tSec * 0.08f,
            )
            val branchPresence = smooth01(((branchNoise - 0.72f) / 0.22f).coerceIn(0f, 1f))
            val tipSplitCount = when {
                branchNoise > 0.965f -> 3
                branchNoise > 0.905f -> 2
                else -> 1
            }
            val tipSplit = branchPresence

            val gust = (0.78f + 0.26f * sin(tSec * (0.55f + 0.45f * r6) + phase)).coerceIn(0.55f, 1.05f)
            val height = (baseH * gust * (0.90f + 0.20f * gustField)).coerceAtMost(maxTongueH)

            // Outer → inner → core layers per tongue (gives depth + a more "hot" center).
            drawTongueLayers(
                seed = seed,
                idx = idx,
                centerX = xBase,
                bottomY = bottom,
                width = baseW,
                height = height,
                clipTopY = top + 0.75f,
                tSec = tSec,
                speed = speed,
                phase = phase,
                intensity = i,
                baseTaper = 1.0f,
                tipSplitCount = tipSplitCount,
                tipSplit = tipSplit,
            )

        }

        // Subtle embers/particles: small spots that occasionally float up.
        val particleBlend = ((i - 0.25f) / 0.75f).coerceIn(0f, 1f)
        if (particleBlend > 0.01f) {
            val particleCount = max(4, (w / 140f).toInt() + 2)
            val baseAlpha = (0.03f + 0.10f * particleBlend).coerceIn(0.03f, 0.16f)
            val activeWindow = (0.07f + 0.14f * particleBlend).coerceIn(0.06f, 0.22f)
            val globalWind = (valueNoise1D(seed xor 0x22D3A3B1, x = 0.0f, t = tSec * 0.10f) * 2f - 1f)

            for (p in 0 until particleCount) {
                val base = hash01(seed xor 0x5A1F7A13, p * 31 + 7)
                val period = (2.6f - 1.2f * s).coerceIn(1.0f, 2.6f) * (0.85f + 0.60f * hash01(seed, p * 31 + 8))
                val cycle = fract((tSec / period) + base)
                if (cycle > activeWindow) continue

                val u01 = (cycle / activeWindow).coerceIn(0f, 1f)
                val fade = (sin(u01 * PI.toFloat())).coerceIn(0f, 1f)

                val x0 = w * hash01(seed, p * 31 + 10)
                val driftLocal = (hash01(seed, p * 31 + 11) - 0.5f) * (w * 0.05f)
                val x = (x0 + (globalWind * w * 0.03f + driftLocal) * u01).coerceIn(0f, w)

                val rise = flameH * (0.55f + 0.60f * hash01(seed, p * 31 + 9))
                val y = (bottom - (u01.pow(1.35f) * rise)).coerceIn(top + 1f, bottom - 1f)

                val r = (0.55f + 2.20f * hash01(seed, p * 31 + 12)) * (0.70f + 0.60f * particleBlend)
                val warm = hash01(seed, p * 31 + 13)
                val c = if (warm > 0.72f) Color(0xFFFFF8E1) else Color(0xFFFFD180)

                drawCircle(
                    color = c.copy(alpha = baseAlpha * fade),
                    radius = r,
                    center = Offset(x, y)
                )
            }
        }

        // Subtle "embers" line at the very bottom (helps the edge feel hot/active).
        val emberA = (0.08f + 0.10f * i).coerceIn(0.06f, 0.22f)
        drawLine(
            color = Color(0xFFFFD180).copy(alpha = emberA),
            start = Offset(0f, bottom - 0.75f),
            end = Offset(w, bottom - 0.75f),
            strokeWidth = 1.25f,
        )
        drawLine(
            color = Color(0xFFFF6A00).copy(alpha = emberA * 0.65f),
            start = Offset(0f, bottom - 1.85f),
            end = Offset(w, bottom - 1.85f),
            strokeWidth = 1.0f,
        )
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
        endY = h
    )

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

        val pixels = sim.pixels

        for (y in 0 until (gridH - 1)) {
            val rowBase = y * gridW
            val py = top + y * cellH

            val fadeUp = 1f - (y / (gridH - 1).coerceAtLeast(1).toFloat())
            val rowAlphaBase = (0.18f + 0.12f * i).coerceIn(0.18f, 0.30f)
            val rowAlpha = (rowAlphaBase * (0.35f + 0.65f * fadeUp)).coerceIn(0.08f, 0.42f)

            for (x in 0 until gridW) {
                val v = pixels[rowBase + x]
                if (v <= drawThreshold) continue

                val boosted = (v + paletteBias).coerceIn(0, last)
                val c = firePalette[boosted]

                val n = (boosted / last.toFloat()).coerceIn(0f, 1f)
                val bright = n.toDouble().pow(1.6).toFloat()
                val a = (rowAlpha * (0.25f + 0.75f * bright)).coerceIn(0.10f, 0.55f)

                drawRect(
                    color = c,
                    topLeft = Offset(x * cellW, py),
                    size = Size(cellW + 0.35f, cellH + 0.35f),
                    alpha = a
                )
            }
        }
    }
}

private class DoomFireSim(
    private val seed: Int
) {
    var gridWidth: Int = 0
        private set

    var gridHeight: Int = 0
        private set

    var pixels: IntArray = IntArray(0)
        private set

    private val paletteMax = firePalette.lastIndex

    private var lastHostWidth: Int = -1
    private var lastFireHeightPx: Float = -1f

    fun resize(
        hostWidthPx: Int,
        fireHeightPx: Float
    ) {
        if (hostWidthPx <= 0 || fireHeightPx <= 0.5f) {
            gridWidth = 0
            gridHeight = 0
            pixels = IntArray(0)
            lastHostWidth = hostWidthPx
            lastFireHeightPx = fireHeightPx
            return
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

                val dx = ((r ushr 2) and 3) - 1
                val dstX = (x + dx).coerceIn(0, w - 1)

                val dst = rowBase + dstX
                if (newIntensity > pixels[dst]) {
                    pixels[dst] = newIntensity
                }
            }
        }
    }

    private fun rand(seed: Int, frame: Int, index: Int): Int {
        var x = seed
        x = x xor (frame * 0x9E3779B9.toInt())
        x = x xor (index * 0x85EBCA6B.toInt())
        x = x xor (x ushr 16)
        x *= 0x7FEB352D
        x = x xor (x ushr 15)
        x *= 0x846CA68B.toInt()
        x = x xor (x ushr 16)
        return x
    }
}

private val firePalette: Array<Color> = arrayOf(
    Color(7, 7, 7),
    Color(31, 7, 7),
    Color(47, 15, 7),
    Color(71, 15, 7),
    Color(87, 23, 7),
    Color(103, 31, 7),
    Color(119, 31, 7),
    Color(143, 39, 7),
    Color(159, 47, 7),
    Color(175, 63, 7),
    Color(191, 71, 7),
    Color(199, 71, 7),
    Color(223, 79, 7),
    Color(223, 87, 7),
    Color(223, 87, 7),
    Color(215, 95, 7),
    Color(215, 95, 7),
    Color(215, 95, 7),
    Color(215, 103, 15),
    Color(207, 111, 15),
    Color(207, 119, 15),
    Color(207, 127, 15),
    Color(207, 135, 23),
    Color(199, 135, 23),
    Color(199, 143, 23),
    Color(199, 151, 31),
    Color(191, 159, 31),
    Color(191, 159, 31),
    Color(191, 167, 39),
    Color(191, 167, 39),
    Color(191, 175, 47),
    Color(183, 175, 47),
    Color(183, 183, 47),
    Color(183, 183, 55),
    Color(207, 207, 111),
    Color(223, 223, 159),
    Color(239, 239, 199),
    Color(255, 255, 255)
)
