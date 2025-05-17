package com.grippo.konfetti.presets

import androidx.compose.ui.graphics.Color
import com.grippo.konfetti.core.Party
import com.grippo.konfetti.core.Position
import com.grippo.konfetti.core.emitter.Emitter
import com.grippo.konfetti.utils.toArgb
import kotlin.time.Duration.Companion.milliseconds

public fun explode(
    colors: List<Color>
): List<Party> {
    val rgbList = colors.map { it.toArgb() }

    return listOf(
        Party(
            speed = 0f,
            maxSpeed = 30f,
            damping = 0.9f,
            spread = 360,
            colors = rgbList,
            emitter = Emitter(100.milliseconds).max(100),
            position = Position.Relative(0.5, 0.3)
        )
    )
}