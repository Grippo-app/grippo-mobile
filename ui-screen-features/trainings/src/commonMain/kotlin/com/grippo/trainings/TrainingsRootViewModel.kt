package com.grippo.trainings

import com.grippo.core.foundation.BaseViewModel

public class TrainingsRootViewModel :
    BaseViewModel<TrainingsRootState, TrainingsRootDirection, TrainingsRootLoader>(TrainingsRootState),
    TrainingsRootContract {
    override fun onBack() {
        navigateTo(TrainingsRootDirection.Back)
    }

    override fun toEditTraining(id: String) {
        navigateTo(TrainingsRootDirection.EditTraining(id))
    }
}
