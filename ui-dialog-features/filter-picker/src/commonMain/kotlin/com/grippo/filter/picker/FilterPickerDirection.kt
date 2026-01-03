package com.grippo.filter.picker

import com.grippo.core.foundation.models.BaseDirection
import com.grippo.core.state.filters.FilterValueState
import kotlinx.collections.immutable.ImmutableList

public sealed interface FilterPickerDirection : BaseDirection {
    public data class BackWithResult(val values: ImmutableList<FilterValueState>) :
        FilterPickerDirection

    public data object Back :
        FilterPickerDirection
}