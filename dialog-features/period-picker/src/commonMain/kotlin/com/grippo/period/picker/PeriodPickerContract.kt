package com.grippo.period.picker

import com.grippo.state.datetime.PeriodState

internal interface PeriodPickerContract {
    fun onSelectClick(value: PeriodState)
    fun onFromClick()
    fun onToClick()
    fun submit()
    fun dismiss()

    companion object Empty : PeriodPickerContract {
        override fun onSelectClick(value: PeriodState) {}
        override fun onFromClick() {}
        override fun onToClick() {}
        override fun submit() {}
        override fun dismiss() {}
    }
}