package com.grippo.text.picker

import com.grippo.core.BaseViewModel
import com.grippo.dialog.api.SELECTION_FEEDBACK_DELAY
import com.grippo.state.text.TextWithId
import kotlinx.collections.immutable.toPersistentList
import kotlinx.coroutines.delay

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
        safeLaunch {
            delay(SELECTION_FEEDBACK_DELAY)
            navigateTo(TextPickerDirection.BackWithResult(state.value.value))
        }
    }

    override fun onDismiss() {
        navigateTo(TextPickerDirection.Back)
    }
}