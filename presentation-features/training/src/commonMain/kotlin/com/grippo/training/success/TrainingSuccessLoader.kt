package com.grippo.training.success

import com.grippo.core.models.BaseLoader

internal sealed interface TrainingSuccessLoader : BaseLoader {
    data object SaveTraining : TrainingSuccessLoader
}