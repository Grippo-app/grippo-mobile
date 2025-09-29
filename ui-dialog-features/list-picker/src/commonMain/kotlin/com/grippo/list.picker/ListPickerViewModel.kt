package com.grippo.list.picker

import com.grippo.core.BaseViewModel
import com.grippo.state.item.ItemState
import kotlinx.collections.immutable.toPersistentList

public class ListPickerViewModel(
    title: String,
    items: List<ItemState>,
) : BaseViewModel<ListPickerState, ListPickerDirection, ListPickerLoader>(
    ListPickerState(
        title = title,
        items = items.toPersistentList(),
    )
), ListPickerContract {

    override fun onItemClick(id: String) {
        val item = state.value.items.find { it.id == id } ?: return
        navigateTo(ListPickerDirection.BackWithResult(item.id))
    }

    override fun onDismiss() {
        navigateTo(ListPickerDirection.Back)
    }
}
