package com.grippo.core.state.formatters

import androidx.compose.runtime.Immutable

@Immutable
public sealed interface FormatState<T> {
    public val display: String
    public val value: T?

    @Immutable
    public data class Valid<T>(
        override val display: String,
        override val value: T
    ) : FormatState<T>

    @Immutable
    public data class Invalid<T>(
        override val display: String,
        override val value: T? = null
    ) : FormatState<T>

    @Immutable
    public data class Empty<T>(
        override val display: String,
        override val value: T? = null
    ) : FormatState<T>
}
