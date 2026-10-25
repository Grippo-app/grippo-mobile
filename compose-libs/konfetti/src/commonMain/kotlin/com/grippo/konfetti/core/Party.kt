package com.grippo.konfetti.core

import com.grippo.konfetti.core.Angle.Companion.BOTTOM
import com.grippo.konfetti.core.Angle.Companion.LEFT
import com.grippo.konfetti.core.Angle.Companion.RIGHT
import com.grippo.konfetti.core.Angle.Companion.TOP
import com.grippo.konfetti.core.emitter.EmitterConfig
import com.grippo.konfetti.core.models.Shape
import com.grippo.konfetti.core.models.Size

public data class Party(
    val angle: Int = 0,
    val spread: Int = 360,
    val speed: Float = 30f,
    val maxSpeed: Float = 0f,
    val damping: Float = 0.9f,
    val size: List<Size> = listOf(Size.SMALL, Size.MEDIUM, Size.LARGE),
    val colors: List<Int> = listOf(0xfce18a, 0xff726d, 0xf4306d, 0xb48def),
    val shapes: List<Shape> = listOf(Shape.Square, Shape.Circle),
    val timeToLive: Long = 2000,
    val fadeOutEnabled: Boolean = true,
    val position: Position = Position.Relative(0.5, 0.5),
    val delay: Int = 0,
    val rotation: Rotation = Rotation(),
    val emitter: EmitterConfig,
)

internal class Angle {
    companion object {
        const val TOP: Int = 270
        const val RIGHT: Int = 0
        const val BOTTOM: Int = 90
        const val LEFT: Int = 180
    }
}

internal class Spread {
    companion object {
        const val SMALL: Int = 30
        const val WIDE: Int = 100
        const val ROUND: Int = 360
    }
}

public sealed class Position {
    public data class Absolute(val x: Float, val y: Float) : Position() {
        public fun between(value: Absolute): Position = Between(this, value)
    }

    public data class Relative(val x: Double, val y: Double) : Position() {
        public fun between(value: Relative): Position = Between(this, value)
    }

    internal data class Between(val min: Position, val max: Position) : Position()
}

public data class Rotation(
    val enabled: Boolean = true,
    val speed: Float = 1f,
    val variance: Float = 0.5f,
    val multiplier2D: Float = 8f,
    val multiplier3D: Float = 1.5f,
) {
    public companion object {
        public fun enabled(): Rotation = Rotation(enabled = true)

        public fun disabled(): Rotation = Rotation(enabled = false)
    }
}
