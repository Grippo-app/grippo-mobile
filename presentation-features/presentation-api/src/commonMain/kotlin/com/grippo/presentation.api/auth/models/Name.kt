package com.grippo.presentation.api.auth.models

import androidx.compose.runtime.Immutable
import com.grippo.validation.NameValidator

@Immutable
public sealed class Name(public open val value: String) {
    public data class Valid(
        override val value: String
    ) : Name(value = value)

    public data class Invalid(
        override val value: String
    ) : Name(value = value)

    public companion object {
        public fun of(value: String): Name {
            return if (NameValidator.isValid(value)) {
                Valid(value)
            } else {
                Invalid(value)
            }
        }
    }
}