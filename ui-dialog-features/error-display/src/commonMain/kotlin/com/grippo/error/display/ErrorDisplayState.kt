package com.grippo.error.display

import androidx.compose.runtime.Immutable
import com.grippo.core.state.error.AppErrorState
import com.grippo.core.state.formatters.UiText
import com.grippo.design.resources.provider.Res
import com.grippo.design.resources.provider.got_it_btn
import com.grippo.design.resources.provider.retry_btn
import com.grippo.dialog.api.DialogConfig

@Immutable
public data class ErrorDisplayState(
    val error: AppErrorState,
    val mode: ErrorDisplayModeState,
)

@Immutable
public enum class ErrorDisplayModeState {
    Retry,
    Inform;

    public fun labelText(): UiText = when (this) {
        Retry -> UiText.Res(Res.string.retry_btn)
        Inform -> UiText.Res(Res.string.got_it_btn)
    }

    public companion object {
        internal fun of(value: DialogConfig.ErrorDisplay.Mode): ErrorDisplayModeState = when (value) {
            is DialogConfig.ErrorDisplay.Mode.Retry -> Retry
            DialogConfig.ErrorDisplay.Mode.Inform -> Inform
        }
    }
}