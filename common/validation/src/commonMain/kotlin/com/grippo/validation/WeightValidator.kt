package com.grippo.validation

public object WeightValidator {

    public fun isValid(value: Float): Boolean {
        val withinRange = value in 30.0f..150.0f
        val hasOneDecimal = (value * 10).rem(1f) == 0f
        return withinRange && hasOneDecimal
    }
}
