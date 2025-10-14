package com.grippo.home.statistics

import androidx.compose.runtime.Immutable
import com.grippo.core.foundation.models.BaseLoader

@Immutable
internal sealed interface HomeStatisticsLoader : BaseLoader {
    @Immutable
    data object Charts : HomeStatisticsLoader
}