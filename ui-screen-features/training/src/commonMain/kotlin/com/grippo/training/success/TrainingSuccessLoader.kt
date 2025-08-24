package com.grippo.training.success

import androidx.compose.runtime.Immutable
import com.grippo.core.models.BaseLoader

@Immutable
internal sealed interface TrainingSuccessLoader : BaseLoader {
    @Immutable
    data object SaveTraining : TrainingSuccessLoader
}