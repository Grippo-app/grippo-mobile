package com.grippo.menu.picker

import com.grippo.analytics.menu.picker.MenuPickerAnalytics
import com.grippo.analytics.menu.picker.models.MenuPickerEvent
import com.grippo.core.foundation.BaseViewModel
import com.grippo.core.state.menu.PickerMenuItem
import kotlinx.collections.immutable.toPersistentList

public class MenuPickerViewModel(
    items: List<PickerMenuItem>,
    private val analytics: MenuPickerAnalytics,
) : BaseViewModel<MenuPickerState, MenuPickerDirection, MenuPickerLoader>(
    MenuPickerState(
        items = items.toPersistentList(),
    )
), MenuPickerContract {

    override fun onItemClick(item: PickerMenuItem) {
        analytics.trackEvent(MenuPickerEvent.ItemClicked)
        navigateTo(MenuPickerDirection.BackWithResult(item))
    }

    override fun onDismiss() {
        analytics.trackEvent(MenuPickerEvent.DismissRequested)
        navigateTo(MenuPickerDirection.Back)
    }
}
