package com.grippo.konfetti.core.models

public sealed interface Shape {
    public object Circle : Shape {
        public val rect: CoreRectImpl = CoreRectImpl()
    }

    public object Square : Shape

    public class Rectangle(
        public val heightRatio: Float,
    ) : Shape {
        init {
            require(heightRatio in 0f..1f)
        }
    }
}
