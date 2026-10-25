package com.grippo.konfetti.core

import com.grippo.konfetti.core.models.Shape

public data class Particle(
    val x: Float,
    val y: Float,
    val width: Float,
    val height: Float,
    val color: Int,
    val rotation: Float,
    val scaleX: Float,
    val shape: Shape,
    val alpha: Int,
)
