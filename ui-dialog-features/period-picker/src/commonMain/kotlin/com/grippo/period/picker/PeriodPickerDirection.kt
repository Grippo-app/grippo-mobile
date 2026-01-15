package com.grippo.period.picker

import com.grippo.core.foundation.models.BaseDirection
import com.grippo.toolkit.date.utils.DateRange

public sealed interface PeriodPickerDirection : BaseDirection {
    public data class BackWithResult(val value: DateRange.Range) : PeriodPickerDirection
    public data object Back : PeriodPickerDirection
}
