package com.grippo.error.display

import com.grippo.core.foundation.BaseViewModel
import com.grippo.state.error.AppErrorState

public class ErrorDisplayViewModel(
    error: AppErrorState,
) : BaseViewModel<ErrorDisplayState, ErrorDisplayDirection, ErrorDisplayLoader>(
    ErrorDisplayState(
        error = error,
    )
), ErrorDisplayContract {

    override fun onDismiss() {
        navigateTo(ErrorDisplayDirection.Back)
    }
}