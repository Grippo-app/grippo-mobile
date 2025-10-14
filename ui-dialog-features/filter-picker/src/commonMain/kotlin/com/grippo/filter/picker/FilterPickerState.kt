package com.grippo.filter.picker

import androidx.compose.runtime.Immutable
import com.grippo.core.state.filters.FilterValue
import kotlinx.collections.immutable.ImmutableList

@Immutable
public data class FilterPickerState(
    val list: ImmutableList<FilterValue>
)