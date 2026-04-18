package com.grippo.menu.picker

import androidx.compose.runtime.Immutable
import com.grippo.core.state.menu.PickerMenuItem

@Immutable
internal interface MenuPickerContract {
    fun onItemClick(item: PickerMenuItem)
    fun onDismiss()

    @Immutable
    companion object Empty : MenuPickerContract {
        override fun onItemClick(item: PickerMenuItem) {}
        override fun onDismiss() {}
    }
}
