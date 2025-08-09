package com.grippo.state.formatters

import androidx.compose.runtime.Immutable

@Immutable
public sealed class WeightFormatState(public open val value: Float) {
    @Immutable
    public data class Valid(
        override val value: Float
    ) : WeightFormatState(value = value)

    @Immutable
    public data class Invalid(
        override val value: Float
    ) : WeightFormatState(value = value)

    public companion object {
        public fun of(value: Float): WeightFormatState {
            return if (WeightValidator.isValid(value)) {
                Valid(value)
            } else {
                Invalid(value)
            }
        }
    }
}

private object WeightValidator {

    fun isValid(value: Float): Boolean {
        val withinRange = value in 30.0f..150.0f
        val hasOneDecimal = (value * 10).rem(1f) == 0f
        return withinRange && hasOneDecimal
    }
}