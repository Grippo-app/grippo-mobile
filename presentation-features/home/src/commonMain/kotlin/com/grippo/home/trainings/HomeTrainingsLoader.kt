package com.grippo.home.trainings

import com.grippo.core.models.BaseLoader

internal sealed interface HomeTrainingsLoader : BaseLoader {
    data object Trainings : HomeTrainingsLoader
}