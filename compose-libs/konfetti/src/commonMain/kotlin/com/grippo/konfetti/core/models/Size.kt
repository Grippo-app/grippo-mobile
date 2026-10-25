package com.grippo.konfetti.core.models

public data class Size(val sizeInDp: Int, val mass: Float = 5f, val massVariance: Float = 0.2f) {
    init {
        require(mass != 0F) { "mass=$mass must be != 0" }
    }

    public companion object {
        public val SMALL: Size = Size(sizeInDp = 6, mass = 4f)
        public val MEDIUM: Size = Size(8)
        public val LARGE: Size = Size(10, mass = 6f)
    }
}
