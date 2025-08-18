package com.grippo.training

import com.grippo.core.BaseViewModel

public class TrainingViewModel :
    BaseViewModel<TrainingState, TrainingDirection, TrainingLoader>(TrainingState),
    TrainingContract {

    override fun onBack() {
        navigateTo(TrainingDirection.Back)
    }

    override fun toRecording() {
        navigateTo(TrainingDirection.ToRecording)
    }

    override fun toSuccess() {
        navigateTo(TrainingDirection.ToSuccess)
    }
}