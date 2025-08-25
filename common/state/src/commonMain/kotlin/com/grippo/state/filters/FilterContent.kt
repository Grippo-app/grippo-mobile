package com.grippo.state.filters

import androidx.compose.runtime.Immutable
import kotlinx.collections.immutable.ImmutableList
import kotlinx.serialization.Serializable

@Immutable
@Serializable
public sealed interface FilterContent {
    @Immutable
    @Serializable
    public data class Chips<T : FilterValue>(
        val checked: Boolean,
        val value: T,
        val options: ImmutableList<T>
    ) : FilterContent

    @Immutable
    @Serializable
    public data class Range<T : FilterValue>(
        val from: T,
        val to: T,
        val limits: Pair<T, T>
    ) : FilterContent
}