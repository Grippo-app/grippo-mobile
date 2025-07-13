package com.grippo.date.picker

import com.grippo.core.models.BaseDirection
import kotlinx.datetime.LocalDateTime

public sealed interface DatePickerDirection : BaseDirection {
    public data class BackWithResult(val value: LocalDateTime) : DatePickerDirection
    public data object Back : DatePickerDirection
}