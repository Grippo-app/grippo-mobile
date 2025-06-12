package com.grippo.presentation.api.profile.models

import androidx.compose.runtime.Immutable
import com.grippo.validation.HeightValidator

@Immutable
public sealed class HeightFormatState(public open val value: Int) {
    public data class Valid(
        override val value: Int
    ) : HeightFormatState(value = value)

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