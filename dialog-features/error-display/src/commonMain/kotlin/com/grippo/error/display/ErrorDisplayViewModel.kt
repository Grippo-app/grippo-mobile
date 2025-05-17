package com.grippo.error.display

import com.grippo.core.BaseViewModel

public class ErrorDisplayViewModel(
    title: String,
    description: String,
) : BaseViewModel<ErrorDisplayState, ErrorDisplayDirection, ErrorDisplayLoader>(
    ErrorDisplayState(
        title = title,
        description = description
    )
), ErrorDisplayContract {

    override fun next() {
        navigateTo(ErrorDisplayDirection.Dismiss)
    }
}