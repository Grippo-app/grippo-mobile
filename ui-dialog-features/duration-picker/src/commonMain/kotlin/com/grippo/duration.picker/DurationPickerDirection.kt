package com.grippo.duration.picker

import com.grippo.core.foundation.models.BaseDirection
import com.grippo.core.state.formatters.DurationFormatState

public sealed interface DurationPickerDirection : BaseDirection {
    public data class BackWithResult(val value: DurationFormatState) : DurationPickerDirection
    public data object Back : DurationPickerDirection
}
