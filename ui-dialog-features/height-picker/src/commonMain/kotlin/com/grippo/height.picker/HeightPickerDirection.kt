package com.grippo.height.picker

import com.grippo.core.foundation.models.BaseDirection
import com.grippo.core.state.formatters.HeightFormatState

public sealed interface HeightPickerDirection : BaseDirection {
    public data class BackWithResult(val value: HeightFormatState) : HeightPickerDirection
    public data object Back : HeightPickerDirection
}