package com.grippo.metrics

import kotlin.math.pow

public fun Int.grToKg(): Float {
    val factor = 10.0.pow(1)
    return (this / factor).toFloat()
}

public fun Float.kgToGr(): Int {
    val factor = 10.0.pow(1)
    return (this * factor).toInt()
}

public fun Int.cmToM(): Float {
    val factor = 10.0.pow(2)
    return (this / factor).toFloat()
}