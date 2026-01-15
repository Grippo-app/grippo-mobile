package com.grippo.period.picker

import androidx.compose.runtime.Immutable
import com.grippo.toolkit.date.utils.DateRange

@Immutable
internal interface PeriodPickerContract {
    fun onSelectRange(range: DateRange.Range)
    fun onDismiss()

    @Immutable
    companion object Empty : PeriodPickerContract {
        override fun onSelectRange(range: DateRange.Range) {}
        override fun onDismiss() {}
    }
}
