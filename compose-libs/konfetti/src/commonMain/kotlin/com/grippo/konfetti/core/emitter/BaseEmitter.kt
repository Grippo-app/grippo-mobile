package com.grippo.konfetti.core.emitter

import com.grippo.konfetti.core.Party
import com.grippo.konfetti.core.models.CoreRect

internal abstract class BaseEmitter {
    abstract fun createConfetti(
        deltaTime: Float,
        party: Party,
        drawArea: CoreRect,
    ): List<Confetti>

    abstract fun isFinished(): Boolean
}
