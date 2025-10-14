package com.grippo.menu.picker

import com.grippo.core.foundation.BaseViewModel
import com.grippo.core.state.menu.MenuItemState
import kotlinx.collections.immutable.toPersistentList

public class MenuPickerViewModel(
    items: List<MenuItemState>,
) : BaseViewModel<MenuPickerState, MenuPickerDirection, MenuPickerLoader>(
    MenuPickerState(
        items = items.toPersistentList(),
    )
), MenuPickerContract {

    override fun onItemClick(id: String) {
        val item = state.value.items.find { it.id == id } ?: return
        navigateTo(MenuPickerDirection.BackWithResult(item.id))
    }

    override fun onDismiss() {
        navigateTo(MenuPickerDirection.Back)
    }
}
