package com.grippo.month.picker

import com.grippo.core.foundation.models.BaseDirection
import com.grippo.core.state.formatters.DateTimeFormatState

public sealed interface MonthPickerDirection : BaseDirection {
    public data class BackWithResult(val value: DateTimeFormatState) : MonthPickerDirection
    public data object Back : MonthPickerDirection
}
