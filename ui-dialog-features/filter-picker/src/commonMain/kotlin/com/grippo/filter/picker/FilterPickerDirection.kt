package com.grippo.filter.picker

import com.grippo.core.foundation.models.BaseDirection
import com.grippo.core.state.filters.FilterValue
import kotlinx.collections.immutable.ImmutableList

public sealed interface FilterPickerDirection : BaseDirection {
    public data class BackWithResult(val values: ImmutableList<FilterValue>) :
        FilterPickerDirection

    public data object Back :
        FilterPickerDirection
}