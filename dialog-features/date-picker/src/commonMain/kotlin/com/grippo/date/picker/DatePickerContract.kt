package com.grippo.date.picker

import kotlinx.datetime.LocalDateTime

internal interface DatePickerContract {
    fun select(value: LocalDateTime)
    fun submit()
    fun dismiss()

    companion object Empty : DatePickerContract {
        override fun select(value: LocalDateTime) {}
        override fun submit() {}
        override fun dismiss() {}
    }
}