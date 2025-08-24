package com.grippo.height.picker

import com.grippo.core.models.BaseDirection
import com.grippo.state.formatters.HeightFormatState

public sealed interface HeightPickerDirection : BaseDirection {
    public data class BackWithResult(val value: HeightFormatState) : HeightPickerDirection
    public data object Back : HeightPickerDirection
}