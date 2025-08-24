package com.grippo.period.picker

import com.grippo.core.models.BaseDirection
import com.grippo.state.datetime.PeriodState

public sealed interface PeriodPickerDirection : BaseDirection {
    public data class BackWithResult(val value: PeriodState) : PeriodPickerDirection
    public data object Back : PeriodPickerDirection
}