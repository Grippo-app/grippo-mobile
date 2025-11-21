package com.grippo.menu.picker

import androidx.compose.runtime.Immutable

@Immutable
internal interface MenuPickerContract {
    fun onItemClick(id: String)
    fun onDismiss()

    @Immutable
    companion object Empty : MenuPickerContract {
        override fun onItemClick(id: String) {}
        override fun onDismiss() {}
    }
}
