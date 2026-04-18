package com.grippo.menu.picker

import com.grippo.core.foundation.BaseViewModel
import com.grippo.core.state.menu.PickerMenuItem
import kotlinx.collections.immutable.toPersistentList

public class MenuPickerViewModel(
    items: List<PickerMenuItem>,
) : BaseViewModel<MenuPickerState, MenuPickerDirection, MenuPickerLoader>(
    MenuPickerState(
        items = items.toPersistentList(),
    )
), MenuPickerContract {

    override fun onItemClick(item: PickerMenuItem) {
        navigateTo(MenuPickerDirection.BackWithResult(item))
    }

    override fun onDismiss() {
        navigateTo(MenuPickerDirection.Back)
    }
}
