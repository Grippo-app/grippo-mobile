package com.grippo.list.picker

internal interface ListPickerContract {
    fun onItemClick(id: String)
    fun onDismiss()

    companion object Empty : ListPickerContract {
        override fun onItemClick(id: String) {}
        override fun onDismiss() {}
    }
}
