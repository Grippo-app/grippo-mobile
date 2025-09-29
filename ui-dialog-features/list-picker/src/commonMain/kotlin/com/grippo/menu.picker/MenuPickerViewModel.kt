package com.grippo.menu.picker

import com.grippo.core.BaseViewModel
import com.grippo.state.menu.MenuItemState
import kotlinx.collections.immutable.toPersistentList

public class MenuPickerViewModel(
    title: String,
    items: List<MenuItemState>,
) : BaseViewModel<MenuPickerState, MenuPickerDirection, MenuPickerLoader>(
    MenuPickerState(
        title = title,
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
