package com.grippo.konfetti.presets

import com.grippo.konfetti.core.Angle
import com.grippo.konfetti.core.Party
import com.grippo.konfetti.core.Position
import com.grippo.konfetti.core.Spread
import com.grippo.konfetti.core.emitter.Emitter
import kotlin.time.Duration.Companion.seconds

public fun rain(): List<Party> {
    return listOf(
        Party(
            speed = 0f,
            maxSpeed = 15f,
            damping = 0.9f,
            angle = Angle.BOTTOM,
            spread = Spread.ROUND,
            colors = listOf(0xfce18a, 0xff726d, 0xf4306d, 0xb48def),
            emitter = Emitter(5.seconds).perSecond(100),
            position = Position.Relative(0.0, 0.0).between(Position.Relative(1.0, 0.0))
        )
    )
}