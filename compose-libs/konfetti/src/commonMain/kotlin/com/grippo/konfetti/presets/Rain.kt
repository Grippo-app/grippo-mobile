package com.grippo.konfetti.presets

import androidx.compose.ui.graphics.Color
import com.grippo.konfetti.core.Angle
import com.grippo.konfetti.core.Party
import com.grippo.konfetti.core.Position
import com.grippo.konfetti.core.Spread
import com.grippo.konfetti.core.emitter.Emitter
import com.grippo.konfetti.utils.toArgb
import kotlin.time.Duration.Companion.seconds

public fun rain(
    colors: List<Color>
): List<Party> {
    val rgbList = colors.map { it.toArgb() }

    return listOf(
        Party(
            speed = 0f,
            maxSpeed = 15f,
            damping = 0.9f,
            angle = Angle.BOTTOM,
            spread = Spread.ROUND,
            colors = rgbList,
            emitter = Emitter(5.seconds).perSecond(100),
            position = Position.Relative(0.0, 0.0).between(Position.Relative(1.0, 0.0))
        )
    )
}