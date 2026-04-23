package com.grippo.date.picker

import com.grippo.core.foundation.models.BaseDirection
import com.grippo.core.state.formatters.DateTimeFormatState

public sealed interface DatePickerDirection : BaseDirection {
    public data class BackWithResult(val value: DateTimeFormatState) : DatePickerDirection
    public data object Back : DatePickerDirection
}
