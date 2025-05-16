package com.grippo.konfetti.presets

import com.grippo.konfetti.core.Party
import com.grippo.konfetti.core.Position
import com.grippo.konfetti.core.emitter.Emitter
import kotlin.time.Duration.Companion.milliseconds

public fun explode(): List<Party> {
    return listOf(
        Party(
            speed = 0f,
            maxSpeed = 30f,
            damping = 0.9f,
            spread = 360,
            colors = listOf(0xfce18a, 0xff726d, 0xf4306d, 0xb48def),
            emitter = Emitter(100.milliseconds).max(100),
            position = Position.Relative(0.5, 0.3)
        )
    )
}