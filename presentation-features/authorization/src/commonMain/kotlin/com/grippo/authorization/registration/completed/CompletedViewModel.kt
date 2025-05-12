package com.grippo.authorization.registration.completed

import com.grippo.core.BaseViewModel

internal class CompletedViewModel(
    name: String
) : BaseViewModel<CompletedState, CompletedDirection, CompletedLoader>(
    CompletedState(name = name)
), CompletedContract {

    override fun complete() {

    }
}