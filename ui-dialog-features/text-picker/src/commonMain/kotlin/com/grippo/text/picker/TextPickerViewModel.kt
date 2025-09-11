package com.grippo.text.picker

import com.grippo.core.BaseViewModel
import com.grippo.state.text.TextWithId
import kotlinx.collections.immutable.toPersistentList

public class TextPickerViewModel(
    initial: TextWithId,
    available: List<TextWithId>,
) : BaseViewModel<TextPickerState, TextPickerDirection, TextPickerLoader>(
    TextPickerState(
        value = initial,
        list = available.toPersistentList()
    )
), TextPickerContract {

    override fun onSelectClick(value: TextWithId) {
        update { it.copy(value = value) }
    }

    override fun onSubmitClick() {
        navigateTo(TextPickerDirection.BackWithResult(state.value.value))
    }

    override fun onDismiss() {
        navigateTo(TextPickerDirection.Back)
    }
}