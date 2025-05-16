package com.grippo.konfetti.compose

import androidx.compose.animation.core.withInfiniteAnimationFrameMillis
import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import com.grippo.konfetti.core.Particle
import com.grippo.konfetti.core.Party
import com.grippo.konfetti.core.PartySystem
import com.grippo.konfetti.core.models.CoreRectImpl
import kotlin.time.Clock
import kotlin.time.Instant

@Composable
public fun Konfetti(
    modifier: Modifier = Modifier,
    parties: List<Party>,
    updateListener: OnParticleSystemUpdateListener? = null,
) {
    lateinit var partySystems: List<PartySystem>

    /**
     * Particles to draw
     */
    val particles = remember { mutableStateOf(emptyList<Particle>()) }

    /**
     * Latest stored frameTimeMilliseconds
     */
    val frameTime = remember { mutableStateOf(0L) }

    /**
     * Area in which the particles are being drawn
     */
    val drawArea = remember { mutableStateOf(CoreRectImpl()) }

    val density = LocalDensity.current.density


    LaunchedEffect(Unit) {
        partySystems = parties.map {
            PartySystem(party = it, pixelDensity = density)
        }

        while (true) {
            withInfiniteAnimationFrameMillis { frameMs ->
                // Calculate time between frames, fallback to 0 when previous frame doesn't exist
                val deltaMs = if (frameTime.value > 0) (frameMs - frameTime.value) else 0
                frameTime.value = frameMs

                particles.value =
                    partySystems.map { particleSystem ->

                        val totalTimeRunning = getTotalTimeRunning(particleSystem.createdAt)
                        // Do not start particleSystem yet if totalTimeRunning is below delay
                        if (totalTimeRunning < particleSystem.party.delay) return@map listOf()

                        if (particleSystem.isDoneEmitting()) {
                            updateListener?.onParticleSystemEnded(
                                system = particleSystem,
                                activeSystems = partySystems.count { !it.isDoneEmitting() },
                            )
                        }

                        particleSystem.render(deltaMs.div(1000f), drawArea.value)
                    }.flatten()
            }
        }
    }

    Canvas(
        modifier =
            modifier
                .onGloballyPositioned {
                    drawArea.value =
                        CoreRectImpl(0f, 0f, it.size.width.toFloat(), it.size.height.toFloat())
                },
        onDraw = {
            particles.value.forEach { particle ->
                withTransform({
                    rotate(
                        degrees = particle.rotation,
                        pivot =
                            Offset(
                                x = particle.x + (particle.width / 2),
                                y = particle.y + (particle.height / 2),
                            ),
                    )
                    scale(
                        scaleX = particle.scaleX,
                        scaleY = 1f,
                        pivot = Offset(particle.x + (particle.width / 2), particle.y),
                    )
                }) {
                    particle.shape.draw(
                        drawScope = this,
                        particle = particle,
                    )
                }
            }
        },
    )
}

internal fun getTotalTimeRunning(startTime: Instant): Long {
    val currentTime = Clock.System.now()
    return currentTime.minus(startTime).inWholeMilliseconds
}
