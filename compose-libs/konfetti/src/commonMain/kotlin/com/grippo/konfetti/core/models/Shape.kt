package com.grippo.konfetti.core.models

public sealed interface Shape {
    public object Circle : Shape {
        // Default replacement for RectF
        public val rect: CoreRectImpl = CoreRectImpl()
    }

    public object Square : Shape

    public class Rectangle(
        /** The ratio of height to width. Must be within range [0, 1] */
        public val heightRatio: Float,
    ) : Shape {
        init {
            require(heightRatio in 0f..1f)
        }
    }
}
