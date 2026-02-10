package com.grippo.duration.picker

import androidx.compose.runtime.Immutable
import kotlin.time.Duration

@Immutable
internal interface DurationPickerContract {
    fun onSelectDuration(value: Duration)
    fun onSubmitClick()
    fun onDismiss()

    @Immutable
    companion object Empty : DurationPickerContract {
        override fun onSelectDuration(value: Duration) {}
        override fun onSubmitClick() {}
        override fun onDismiss() {}
    }
}
