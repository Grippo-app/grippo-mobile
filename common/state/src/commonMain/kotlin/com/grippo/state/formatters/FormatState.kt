package com.grippo.state.formatters

import androidx.compose.runtime.Immutable

@Immutable
public sealed interface FormatState<T> {
    public val displayValue: String
    public val value: T?
    public val isValid: Boolean

    @Immutable
    public data class Valid<T>(
        override val displayValue: String,
        override val value: T
    ) : FormatState<T> {
        override val isValid: Boolean = true
    }

    @Immutable
    public data class Invalid<T>(
        override val displayValue: String,
        override val value: T? = null
    ) : FormatState<T> {
        override val isValid: Boolean = false
    }
}