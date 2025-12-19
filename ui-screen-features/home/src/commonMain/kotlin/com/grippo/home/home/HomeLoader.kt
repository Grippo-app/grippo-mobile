package com.grippo.home.home

import androidx.compose.runtime.Immutable
import com.grippo.core.foundation.models.BaseLoader

@Immutable
internal sealed interface HomeLoader : BaseLoader {
    @Immutable
    data object Trainings : HomeLoader
}