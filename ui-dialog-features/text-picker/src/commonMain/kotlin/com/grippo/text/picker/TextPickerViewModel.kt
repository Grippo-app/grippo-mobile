package com.grippo.text.picker

import com.grippo.core.BaseViewModel
import com.grippo.state.text.TextWithId
import kotlinx.collections.immutable.toPersistentList
import kotlinx.coroutines.delay
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds

public class TextPickerViewModel(
    initial: TextWithId,
    available: List<TextWithId>,
) : BaseViewModel<TextPickerState, TextPickerDirection, TextPickerLoader>(
    TextPickerState(
        value = initial,
        list = available.toPersistentList()
    )
), TextPickerContract {

    private companion object {
        private val SELECTION_FEEDBACK_DELAY: Duration = 150.milliseconds
    }

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