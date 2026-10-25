package com.grippo.konfetti.core.emitter

import com.grippo.konfetti.core.models.CoreRect
import com.grippo.konfetti.core.models.Shape
import com.grippo.konfetti.core.models.Vector
import kotlin.math.abs

internal class Confetti(
    var location: Vector,
    private val color: Int,
    val width: Float,
    private val mass: Float,
    val shape: Shape,
    var lifespan: Long = -1L,
    val fadeOut: Boolean = true,
    private var acceleration: Vector = Vector(0f, 0f),
    var velocity: Vector = Vector(),
    var damping: Float,
    val rotationSpeed3D: Float = 1f,
    val rotationSpeed2D: Float = 1f,
    val pixelDensity: Float,
) {
    companion object {
        private const val DEFAULT_FRAME_RATE = 60f
        private const val GRAVITY = 0.02f
        private const val ALPHA_DECREMENT = 5
        private const val MILLIS_IN_SECOND = 1000
        private const val FULL_CIRCLE = 360f
    }

    var rotation = 0f
    private var rotationWidth = width

    private var frameRate = DEFAULT_FRAME_RATE
    private var gravity = Vector(0f, GRAVITY)

    var alpha: Int = 255
    var scaleX = 0f

    var alphaColor: Int = 0

    var drawParticle = true
        private set

    fun getSize(): Float = width

    fun isDead(): Boolean = alpha <= 0

    fun applyForce(force: Vector) {
        acceleration.addScaled(force, 1f / mass)
    }

    fun render(
        deltaTime: Float,
        drawArea: CoreRect,
    ) {
        applyForce(gravity)
        update(deltaTime, drawArea)
    }

    private fun update(
        deltaTime: Float,
        drawArea: CoreRect,
    ) {
        frameRate = if (deltaTime > 0) 1f / deltaTime else DEFAULT_FRAME_RATE

        if (location.y > drawArea.height) {
            alpha = 0
            return
        }

        velocity.add(acceleration)
        velocity.mult(damping)

        location.addScaled(velocity, deltaTime * frameRate * pixelDensity)

        lifespan -= (deltaTime * MILLIS_IN_SECOND).toLong()
        if (lifespan <= 0) updateAlpha(deltaTime)

        rotation += rotationSpeed2D * deltaTime * frameRate
        if (rotation >= FULL_CIRCLE) rotation = 0f

        rotationWidth -= abs(rotationSpeed3D) * deltaTime * frameRate
        if (rotationWidth < 0) rotationWidth = width

        scaleX = abs(rotationWidth / width - 0.5f) * 2
        alphaColor = (alpha shl 24) or (color and 0xffffff)

        drawParticle = drawArea.contains(location.x.toInt(), location.y.toInt())
    }

    private fun updateAlpha(deltaTime: Float) {
        alpha =
            if (fadeOut) {
                val interval = ALPHA_DECREMENT * deltaTime * frameRate
                (alpha - interval.toInt()).coerceAtLeast(0)
            } else {
                0
            }
    }
}
