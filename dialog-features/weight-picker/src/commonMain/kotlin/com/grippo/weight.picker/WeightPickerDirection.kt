package com.grippo.weight.picker

import com.grippo.core.models.BaseDirection
import com.grippo.state.formatters.WeightFormatState

public sealed interface WeightPickerDirection : BaseDirection {
    public data class BackWithResult(val value: WeightFormatState) : WeightPickerDirection
    public data object Back : WeightPickerDirection
}