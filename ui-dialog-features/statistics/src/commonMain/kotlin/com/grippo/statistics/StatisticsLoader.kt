package com.grippo.statistics

import androidx.compose.runtime.Immutable
import com.grippo.core.foundation.models.BaseLoader

@Immutable
public sealed interface StatisticsLoader : BaseLoader {
    @Immutable
    public data object Charts : StatisticsLoader
}
