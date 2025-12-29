package com.grippo.month.picker

import com.grippo.core.foundation.models.BaseDirection
import com.grippo.core.state.formatters.DateFormatState

public sealed interface MonthPickerDirection : BaseDirection {
    public data class BackWithResult(val value: DateFormatState) : MonthPickerDirection
    public data object Back : MonthPickerDirection
}
