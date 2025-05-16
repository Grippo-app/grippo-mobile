package com.grippo.konfetti.core.models

public interface CoreRect {
    public var x: Float
    public var y: Float
    public var width: Float
    public var height: Float

    public fun set(
        x: Float,
        y: Float,
        width: Float,
        height: Float,
    ) {
        this.x = x
        this.y = y
        this.width = width
        this.height = height
    }

    public fun contains(
        px: Int,
        py: Int,
    ): Boolean {
        return px >= x && px <= x + width && py >= y && py <= y + height
    }
}

public class CoreRectImpl(
    override var x: Float = 0f,
    override var y: Float = 0f,
    override var width: Float = 0f,
    override var height: Float = 0f,
) : CoreRect {
    override fun set(
        x: Float,
        y: Float,
        width: Float,
        height: Float,
    ) {
        super.set(x, y, width, height)
    }

    override fun contains(
        px: Int,
        py: Int,
    ): Boolean {
        return super.contains(px, py)
    }
}
