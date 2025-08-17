package com.grippo.training.setup

import com.grippo.core.models.BaseLoader

internal sealed interface TrainingSetupLoader : BaseLoader {
    data object Muscles : TrainingSetupLoader
}
