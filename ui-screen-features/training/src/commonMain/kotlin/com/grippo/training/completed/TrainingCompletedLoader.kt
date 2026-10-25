package com.grippo.training.completed

import androidx.compose.runtime.Immutable
import com.grippo.core.foundation.models.BaseLoader

@Immutable
internal sealed interface TrainingCompletedLoader : BaseLoader {
    @Immutable
    data object SaveTraining : TrainingCompletedLoader

    @Immutable
    data object LoadRecords : TrainingCompletedLoader

    @Immutable
    data object ShareRecap : TrainingCompletedLoader
}
