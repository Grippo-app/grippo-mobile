package com.grippo.period.picker

import com.grippo.core.foundation.models.BaseDirection
import com.grippo.toolkit.date.utils.DateRangeKind

public sealed interface PeriodPickerDirection : BaseDirection {
    public data class BackWithResult(val value: DateRangeKind) : PeriodPickerDirection
    public data object Back : PeriodPickerDirection
}
