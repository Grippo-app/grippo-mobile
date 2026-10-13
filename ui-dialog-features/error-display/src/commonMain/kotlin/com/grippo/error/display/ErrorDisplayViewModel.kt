package com.grippo.error.display

import com.grippo.analytics.error.display.ErrorDisplayAnalytics
import com.grippo.analytics.error.display.models.ErrorDisplayEvent
import com.grippo.core.foundation.BaseViewModel
import com.grippo.core.state.error.AppErrorState

public class ErrorDisplayViewModel(
    error: AppErrorState,
    private val analytics: ErrorDisplayAnalytics,
) : BaseViewModel<ErrorDisplayState, ErrorDisplayDirection, ErrorDisplayLoader>(
    ErrorDisplayState(
        error = error,
    )
), ErrorDisplayContract {

    override fun onDismiss() {
        analytics.trackEvent(ErrorDisplayEvent.DismissRequested)
        navigateTo(ErrorDisplayDirection.Back)
    }
}