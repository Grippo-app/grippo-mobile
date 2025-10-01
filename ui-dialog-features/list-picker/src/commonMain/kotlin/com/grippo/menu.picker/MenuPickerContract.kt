package com.grippo.menu.picker

internal interface MenuPickerContract {
    fun onItemClick(id: String)
    fun onDismiss()

    companion object Empty : MenuPickerContract {
        override fun onItemClick(id: String) {}
        override fun onDismiss() {}
    }
}
