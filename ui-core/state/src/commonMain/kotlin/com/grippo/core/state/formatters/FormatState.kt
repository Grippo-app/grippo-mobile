package com.grippo.core.state.formatters

import androidx.compose.runtime.Immutable

@Immutable
public sealed interface FormatState<T> {
    public val display: String
    public val value: T?

    public interface Valid<T> : FormatState<T>
    public interface Invalid<T> : FormatState<T>
    public interface Empty<T> : FormatState<T>
}
