package com.grippo.validation

public object HeightValidator {

    public fun isValid(value: Int): Boolean {
        val withinRange = value in 100..250
        return withinRange
    }
}
