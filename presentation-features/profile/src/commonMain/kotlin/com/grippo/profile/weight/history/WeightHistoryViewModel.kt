package com.grippo.profile.weight.history

import com.grippo.core.BaseViewModel

internal class WeightHistoryViewModel :
    BaseViewModel<WeightHistoryState, WeightHistoryDirection, WeightHistoryLoader>(
        WeightHistoryState
    ),
    WeightHistoryContract {

    override fun back() {
        navigateTo(WeightHistoryDirection.Back)
    }
}