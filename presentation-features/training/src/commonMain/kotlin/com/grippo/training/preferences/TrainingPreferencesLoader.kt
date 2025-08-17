package com.grippo.training.preferences

import com.grippo.core.models.BaseLoader

internal sealed interface TrainingPreferencesLoader : BaseLoader {
    data object Muscles : TrainingPreferencesLoader
}