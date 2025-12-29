package com.grippo.month.picker

import androidx.compose.runtime.Immutable
import kotlinx.datetime.LocalDateTime

@Immutable
internal interface MonthPickerContract {
    fun onSelectMonth(value: LocalDateTime)
    fun onSubmitClick()
    fun onDismiss()

    @Immutable
    companion object Empty : MonthPickerContract {
        override fun onSelectMonth(value: LocalDateTime) {}
        override fun onSubmitClick() {}
        override fun onDismiss() {}
    }
}
