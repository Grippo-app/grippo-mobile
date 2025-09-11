package com.grippo.text.picker

import com.grippo.state.text.TextWithId

internal interface TextPickerContract {
    fun onSelectClick(value: TextWithId)
    fun onSubmitClick()
    fun onDismiss()

    companion object Empty : TextPickerContract {
        override fun onSelectClick(value: TextWithId) {}
        override fun onSubmitClick() {}
        override fun onDismiss() {}
    }
}