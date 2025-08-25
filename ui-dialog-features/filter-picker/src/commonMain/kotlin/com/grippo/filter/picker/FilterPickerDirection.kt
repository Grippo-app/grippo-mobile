package com.grippo.filter.picker

import com.grippo.core.models.BaseDirection
import com.grippo.state.filters.FilterContent
import kotlinx.collections.immutable.ImmutableList

public sealed interface FilterPickerDirection : BaseDirection {
    public data class BackWithResult(val values: ImmutableList<FilterContent>) :
        FilterPickerDirection

    public data object Back :
        FilterPickerDirection
}