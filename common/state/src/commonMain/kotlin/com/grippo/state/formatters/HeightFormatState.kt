package com.grippo.state.formatters

import androidx.compose.runtime.Immutable

@Immutable
public sealed class HeightFormatState(public open val value: Int) {
    @Immutable
    public data class Valid(
        override val value: Int
    ) : HeightFormatState(value = value)

    @Immutable
    public data class Invalid(
        override val value: Int
    ) : HeightFormatState(value = value)

    public companion object {
        public fun of(value: Int): HeightFormatState {
            return if (HeightValidator.isValid(value)) {
                Valid(value)
            } else {
                Invalid(value)
            }
        }
    }
}

private object HeightValidator {
    fun isValid(value: Int): Boolean {
        val withinRange = value in 100..250
        return withinRange
    }
}
