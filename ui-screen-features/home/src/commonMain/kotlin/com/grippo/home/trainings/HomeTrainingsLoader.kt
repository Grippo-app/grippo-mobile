package com.grippo.home.trainings

import androidx.compose.runtime.Immutable
import com.grippo.core.foundation.models.BaseLoader

@Immutable
internal sealed interface HomeTrainingsLoader : BaseLoader {
    @Immutable
    data object Trainings : HomeTrainingsLoader
}