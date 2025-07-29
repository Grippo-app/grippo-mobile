package com.grippo.state.profile

import androidx.compose.runtime.Immutable
import com.grippo.validation.WeightValidator

@Immutable
public sealed class WeightFormatState(public open val value: Float) {
    public data class Valid(
        override val value: Float
    ) : WeightFormatState(value = value)

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