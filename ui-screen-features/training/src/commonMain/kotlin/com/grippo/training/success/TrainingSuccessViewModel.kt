package com.grippo.training.success

import com.grippo.core.BaseViewModel
import kotlinx.coroutines.delay

internal class TrainingSuccessViewModel :
    BaseViewModel<TrainingSuccessState, TrainingSuccessDirection, TrainingSuccessLoader>(
        TrainingSuccessState
    ), TrainingSuccessContract {


    init {
        safeLaunch(loader = TrainingSuccessLoader.SaveTraining) {
            delay(1000)
        }
    }

    override fun onBack() {
        navigateTo(TrainingSuccessDirection.Back)
    }
}
