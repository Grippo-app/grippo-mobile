package com.grippo.period.picker

import androidx.compose.runtime.Immutable
import com.grippo.toolkit.date.utils.DateRangeKind

@Immutable
internal interface PeriodPickerContract {
    fun onSelectRange(kind: DateRangeKind)
    fun onDismiss()

    @Immutable
    companion object Empty : PeriodPickerContract {
        override fun onSelectRange(kind: DateRangeKind) {}
        override fun onDismiss() {}
    }
}
