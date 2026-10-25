package com.grippo.konfetti.core.emitter

import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds

public data class Emitter(
    val duration: Duration = 1000.milliseconds,
) {
    public fun max(amount: Int): EmitterConfig = EmitterConfig(this).max(amount)

    public fun perSecond(amount: Int): EmitterConfig = EmitterConfig(this).perSecond(amount)
}

public class EmitterConfig(
    emitter: Emitter,
) {
    public var emittingTime: Long = 0L

    public var amountPerMs: Float = 0f

    init {
        this.emittingTime = emitter.duration.inWholeMilliseconds
    }

    public fun max(amount: Int): EmitterConfig {
        if (amount <= 0 || emittingTime <= 0) {
            this.amountPerMs = 0f
        } else {
            this.amountPerMs = emittingTime.toFloat() / amount / 1000f
        }
        return this
    }

    public fun perSecond(amount: Int): EmitterConfig {
        if (amount <= 0) {
            this.amountPerMs = 0f
        } else {
            this.amountPerMs = 1f / amount
        }
        return this
    }
}
