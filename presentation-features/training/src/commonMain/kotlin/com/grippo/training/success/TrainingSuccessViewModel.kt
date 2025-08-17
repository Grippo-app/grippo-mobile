package com.grippo.training.success

import com.grippo.core.BaseViewModel

internal class TrainingSuccessViewModel :
    BaseViewModel<TrainingSuccessState, TrainingSuccessDirection, TrainingSuccessLoader>(
        TrainingSuccessState
    ), TrainingSuccessContract {

    override fun onBack() {
        navigateTo(TrainingSuccessDirection.Back)
    }
}