package com.grippo.core.state.formatters

import androidx.compose.runtime.Immutable

/**
 * Common contract for all format states.
 *
 * Each concrete state (Email, Name, Weight, ...) is its own sealed hierarchy whose
 * nested `Valid`/`Invalid`/`Empty` data classes also implement the corresponding
 * marker interface below. This lets consumers reason about all format states
 * uniformly — e.g. `formatState is FormatState.Invalid<*>` — without duplicating
 * per-type helpers.
 */
@Immutable
public sealed interface FormatState<T> {
    public val display: String
    public val value: T?

    public interface Valid<T> : FormatState<T>
    public interface Invalid<T> : FormatState<T>
    public interface Empty<T> : FormatState<T>
}
