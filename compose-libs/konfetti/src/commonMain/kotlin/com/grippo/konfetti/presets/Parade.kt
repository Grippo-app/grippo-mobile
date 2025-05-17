package com.grippo.konfetti.presets

import androidx.compose.ui.graphics.Color
import com.grippo.konfetti.core.Angle
import com.grippo.konfetti.core.Party
import com.grippo.konfetti.core.Position
import com.grippo.konfetti.core.Spread
import com.grippo.konfetti.core.emitter.Emitter
import com.grippo.konfetti.utils.toArgb
import kotlin.time.Duration.Companion.seconds

public fun parade(
    colors: List<Color>
): List<Party> {
    val rgbList = colors.map { it.toArgb() }

    val party = Party(
        speed = 10f,
        maxSpeed = 30f,
        damping = 0.9f,
        angle = Angle.RIGHT - 45,
        spread = Spread.SMALL,
        colors = rgbList,
        emitter = Emitter(1.seconds).perSecond(30),
        position = Position.Relative(0.0, 0.5)
    )

    return listOf(
        party,
        party.copy(
            angle = party.angle - 90,
            position = Position.Relative(1.0, 0.5)
        ),
    )
}