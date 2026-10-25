package com.grippo.konfetti.core

import com.grippo.konfetti.core.emitter.BaseEmitter
import com.grippo.konfetti.core.emitter.Confetti
import com.grippo.konfetti.core.emitter.PartyEmitter
import com.grippo.konfetti.core.models.CoreRect
import kotlin.time.Clock
import kotlin.time.Instant

public class PartySystem(
    public val party: Party,
    public val createdAt: Instant = Clock.System.now(),
    pixelDensity: Float,
) {
    public var enabled: Boolean = true

    private var emitter: BaseEmitter = PartyEmitter(party.emitter, pixelDensity)

    private val activeParticles = mutableListOf<Confetti>()

    public fun render(
        deltaTime: Float,
        drawArea: CoreRect,
    ): List<Particle> {
        if (enabled) {
            activeParticles.addAll(emitter.createConfetti(deltaTime, party, drawArea))
        }

        activeParticles.forEach { it.render(deltaTime, drawArea) }

        activeParticles.removeAll { it.isDead() }

        return activeParticles.filter { it.drawParticle }.map { it.toParticle() }
    }

    public fun isDoneEmitting(): Boolean =
        (emitter.isFinished() && activeParticles.isEmpty()) || (!enabled && activeParticles.isEmpty())

    public fun getActiveParticleAmount(): Int = activeParticles.size
}

internal fun Confetti.toParticle(): Particle {
    return Particle(
        location.x,
        location.y,
        width,
        width,
        alphaColor,
        rotation,
        scaleX,
        shape,
        alpha,
    )
}
