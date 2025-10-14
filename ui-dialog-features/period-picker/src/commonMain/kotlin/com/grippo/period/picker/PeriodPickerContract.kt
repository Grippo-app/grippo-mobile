package com.grippo.period.picker

import com.grippo.core.state.datetime.PeriodState

internal interface PeriodPickerContract {
    fun onSelectClick(value: PeriodState)
    fun onFromClick()
    fun onToClick()
    fun onSubmitClick()
    fun onDismiss()

    companion object Empty : PeriodPickerContract {
        override fun onSelectClick(value: PeriodState) {}
        override fun onFromClick() {}
        override fun onToClick() {}
        override fun onSubmitClick() {}
        override fun onDismiss() {}
    }
}