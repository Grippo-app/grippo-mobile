package com.grippo.period.picker

import com.grippo.core.foundation.models.BaseDirection
import com.grippo.core.state.formatters.PeriodFormatState

public sealed interface PeriodPickerDirection : BaseDirection {
    public data class BackWithResult(val value: PeriodFormatState) : PeriodPickerDirection
    public data object Back : PeriodPickerDirection
}
