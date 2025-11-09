package com.grippo.trainings

import androidx.compose.runtime.Immutable
import com.grippo.core.foundation.models.BaseLoader

@Immutable
internal sealed interface TrainingsLoader : BaseLoader {
    @Immutable
    data object Trainings : TrainingsLoader
}