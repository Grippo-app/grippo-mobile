package com.grippo.date.picker

import androidx.compose.runtime.Immutable
import kotlinx.datetime.LocalDateTime

@Immutable
internal interface DatePickerContract {
    fun onSelectDate(value: LocalDateTime)
    fun onSubmitClick()
    fun onDismiss()

    @Immutable
    companion object Empty : DatePickerContract {
        override fun onSelectDate(value: LocalDateTime) {}
        override fun onSubmitClick() {}
        override fun onDismiss() {}
    }
}