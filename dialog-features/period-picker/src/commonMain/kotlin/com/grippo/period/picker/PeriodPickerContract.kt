package com.grippo.period.picker

import com.grippo.state.datetime.PeriodState

internal interface PeriodPickerContract {
    fun select(value: PeriodState)
    fun submit()
    fun dismiss()

    companion object Empty : PeriodPickerContract {
        override fun select(value: PeriodState) {}
        override fun submit() {}
        override fun dismiss() {}
    }
}