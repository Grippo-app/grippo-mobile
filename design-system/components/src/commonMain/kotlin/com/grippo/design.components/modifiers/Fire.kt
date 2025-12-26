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
            0.55f to Color(0xFFFF6D00).copy(alpha = baseGlowAlphaTop),
            0.80f to Color(0xFFFF6D00).copy(
                alpha = (baseGlowAlphaBottom * 0.75f).coerceIn(
                    0f,
                    0.40f
                )
            ),
            1.00f to Color(0xFFFFB300).copy(alpha = baseGlowAlphaBottom)
        ),
        startY = top,
        endY = h
    )

    clipRect(left = 0f, top = top, right = w, bottom = h) {
        drawRect(
            brush = baseGlow,
            topLeft = Offset(0f, top),
            size = Size(w, flameH)
        )

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

        val fireH = fireHeightPx

        val targetRows = 22
        val cell = max(1f, fireH / targetRows.toFloat())

        val w = max(1, ceil(hostWidthPx / cell).toInt())
        val h = max(1, ceil(fireH / cell).toInt())

        val same =
            (w == gridWidth) &&
                    (h == gridHeight) &&
                    (hostWidthPx == lastHostWidth) &&
                    kotlin.math.abs(fireH - lastFireHeightPx) < 0.5f

        if (same) return

        lastHostWidth = hostWidthPx
        lastFireHeightPx = fireH

        gridWidth = w
        gridHeight = h
        pixels = IntArray(w * h)
        ignite(maxIntensity = paletteMax)
    }

    private fun ignite(maxIntensity: Int) {
        if (gridWidth <= 0 || gridHeight <= 0) return
        val baseStart = (gridHeight - 1) * gridWidth
        for (x in 0 until gridWidth) {
            pixels[baseStart + x] = maxIntensity.coerceIn(0, paletteMax)
        }
    }

    fun step(frame: Int, intensity: Float) {
        val w = gridWidth
        val h = gridHeight
        if (w <= 0 || h <= 0) return

        val i = intensity.coerceIn(0f, 1f)
        if (i <= 0.001f) {
            pixels.fill(0)
            return
        }

        val maxI = (paletteMax * (0.85f + 0.15f * i)).roundToInt().coerceIn(0, paletteMax)
        val extraDecay = (6 + ((1f - i) * 6f).roundToInt()).coerceIn(6, 12)

        val baseStart = (h - 1) * w

        val sourceOnChance = (0.08f + 0.14f * i).coerceIn(0.08f, 0.22f)
        val sourceBanding = (7 + ((1f - i) * 5f).roundToInt()).coerceIn(7, 12)

        for (x in 0 until w) {
            val r = rand(seed, frame, x)
            val on01 = ((r ushr 3) and 1023) / 1023f

            val bandOff = ((x + (frame / 6)) % sourceBanding) == 0
            val isOn = !bandOff && (on01 < sourceOnChance)

            val base = if (isOn) maxI else 0
            val jitter = (r and 7)
            pixels[baseStart + x] = (base - jitter).coerceAtLeast(0)
        }

        for (y in 0 until (h - 1)) {
            val rowBase = y * w
            for (x in 0 until w) {
                val idx = rowBase + x
                val belowIdx = idx + w
                val belowIntensity = pixels[belowIdx]

                val r = rand(seed, frame, idx)

                val decayBase = r and 7
                val decay = (decayBase + extraDecay).coerceAtMost(18)

                var newIntensity = (belowIntensity - decay).coerceAtLeast(0)

                val burnout = ((r ushr 10) and 3) == 0
                if (burnout) {
                    newIntensity = 0
                } else {
                    val sparsePenalty = ((r ushr 6) and 3) + if (((r ushr 2) and 31) == 0) 3 else 0
                    newIntensity = (newIntensity - sparsePenalty).coerceAtLeast(0)
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
