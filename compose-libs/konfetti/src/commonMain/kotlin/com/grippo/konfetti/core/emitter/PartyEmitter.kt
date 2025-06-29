package com.grippo.konfetti.core.emitter

import com.grippo.konfetti.core.Party
import com.grippo.konfetti.core.Position
import com.grippo.konfetti.core.Rotation
import com.grippo.konfetti.core.models.CoreRect
import com.grippo.konfetti.core.models.Shape
import com.grippo.konfetti.core.models.Size
import com.grippo.konfetti.core.models.Vector
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

/**
 * Emitter is responsible for creating a certain amount of particles per tick.
 * - Creating x amount of particles in a certain time frame
 * - Creating x amount of particles until the threshold [maxParticles] is met
 */
internal class PartyEmitter(
    private val emitterConfig: EmitterConfig,
    private val pixelDensity: Float,
    private val random: Random = Random.Default,
) : BaseEmitter() {
    // Keeping count of how many particles are created whilst running the emitter
    private var particlesCreated = 0

    /** Elapsed time in milliseconds */
    private var elapsedTime: Float = 0f

    /** Amount of time elapsed since last particle creation in milliseconds */
    private var createParticleMs: Float = 0f

    /**
     * If timer isn't started yet, set initial start time
     * Create the first confetti immediately and update the last emitting time
     */
    override fun createConfetti(
        deltaTime: Float,
        party: Party,
        drawArea: CoreRect,
    ): List<Confetti> {
        createParticleMs += deltaTime

        // Initial deltaTime can't be higher than the emittingTime, if so calculate
        // amount of particles based on max emittingTime
        val emittingTime = emitterConfig.emittingTime / 1000f
        if (elapsedTime == 0f && deltaTime > emittingTime) {
            createParticleMs = emittingTime
        }

        var particles = listOf<Confetti>()

        // Check if particle should be created
        if (createParticleMs >= emitterConfig.amountPerMs && !isTimeElapsed()) {
            // Calculate how many particle  to create in the elapsed time
            val amount: Int = (createParticleMs / emitterConfig.amountPerMs).toInt()

            particles = (1..amount).map { createParticle(party, drawArea) }

            // Reset timer and add left over time for next cycle
            createParticleMs %= emitterConfig.amountPerMs
        }

        elapsedTime += deltaTime * 1000
        return particles
    }

    /**
     * Create particle based on the [Party] configuration
     * @param party Configurations used for creating the initial Confetti states
     * @param drawArea the area and size of the canvas
     */
    private fun createParticle(
        party: Party,
        drawArea: CoreRect,
    ): Confetti {
        particlesCreated++
        with(party) {
            val randomSize = size[random.nextInt(size.size)]
            return Confetti(
                location = position.get(drawArea).run { Vector(x, y) },
                width = randomSize.sizeInDp * pixelDensity,
                mass = randomSize.massWithVariance(),
                shape = getRandomShape(shapes),
                color = colors[random.nextInt(colors.size)],
                lifespan = timeToLive,
                fadeOut = fadeOutEnabled,
                velocity = getVelocity(),
                damping = party.damping,
                rotationSpeed2D = rotation.rotationSpeed() * party.rotation.multiplier2D,
                rotationSpeed3D = rotation.rotationSpeed() * party.rotation.multiplier3D,
                pixelDensity = pixelDensity,
            )
        }
    }

    /**
     * Calculate a rotation speed multiplier based on the base and variance
     * @return rotation speed and return 0 when rotation is disabled
     */
    private fun Rotation.rotationSpeed(): Float {
        if (!enabled) return 0f
        val randomValue = random.nextFloat() * 2f - 1f
        return speed + (speed * variance * randomValue)
    }

    private fun Party.getSpeed(): Float =
        if (maxSpeed == -1f) {
            speed
        } else {
            ((maxSpeed - speed) * random.nextFloat()) + speed
        }

    /**
     * Get the mass with a slight variance added to create randomness between how each particle
     * will react in speed when moving up or down
     */
    private fun Size.massWithVariance(): Float = mass + (mass * (random.nextFloat() * massVariance))

    /**
     * Calculate velocity based on radian and speed
     * @return [Vector] velocity
     */
    private fun Party.getVelocity(): Vector {
        val speed = getSpeed()
        val radian = getAngle() * (PI / 180)
        val vx = speed * cos(radian).toFloat()
        val vy = speed * sin(radian).toFloat()
        return Vector(vx, vy)
    }

    private fun Party.getAngle(): Double {
        if (spread == 0) return angle.toDouble()

        val minAngle = angle - (spread / 2)
        val maxAngle = angle + (spread / 2)
        return (maxAngle - minAngle) * random.nextDouble() + minAngle
    }

    private fun Position.get(drawArea: CoreRect): Position.Absolute {
        return when (this) {
            is Position.Absolute -> Position.Absolute(x, y)
            is Position.Relative -> {
                Position.Absolute(
                    drawArea.width * x.toFloat(),
                    drawArea.height * y.toFloat(),
                )
            }

            is Position.Between -> {
                val minPos = min.get(drawArea)
                val maxPos = max.get(drawArea)
                return Position.Absolute(
                    x = random.nextFloat().times(maxPos.x.minus(minPos.x)) + minPos.x,
                    y = random.nextFloat().times(maxPos.y.minus(minPos.y)) + minPos.y,
                )
            }
        }
    }

    /**
     * Get a random shape from the list of shapes
     */
    private fun getRandomShape(shapes: List<Shape>): Shape {
        return shapes[random.nextInt(shapes.size)]
    }

    /**
     * If the [duration] is 0 it's not set and not relevant
     * If the emitting time is set check if [elapsedTime] exceeded the emittingTime
     */
    private fun isTimeElapsed(): Boolean {
        return when (emitterConfig.emittingTime) {
            0L -> false
            else -> elapsedTime >= emitterConfig.emittingTime
        }
    }

    override fun isFinished(): Boolean {
        return if (emitterConfig.emittingTime > 0L) {
            elapsedTime >= emitterConfig.emittingTime
        } else {
            false
        }
    }
}
