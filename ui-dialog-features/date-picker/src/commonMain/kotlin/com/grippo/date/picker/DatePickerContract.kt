package com.grippo.date.picker

import kotlinx.datetime.LocalDateTime

internal interface DatePickerContract {
    fun onSelectDate(value: LocalDateTime)
    fun onSubmitClick()
    fun onDismiss()

    companion object Empty : DatePickerContract {
        override fun onSelectDate(value: LocalDateTime) {}
        override fun onSubmitClick() {}
        override fun onDismiss() {}
    }
}