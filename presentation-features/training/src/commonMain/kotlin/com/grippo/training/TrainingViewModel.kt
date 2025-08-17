package com.grippo.training

import com.grippo.core.BaseViewModel

public class TrainingViewModel :
    BaseViewModel<TrainingState, TrainingDirection, TrainingLoader>(TrainingState),
    TrainingContract {

    override fun onBack() {
        navigateTo(TrainingDirection.Back)
    }
}