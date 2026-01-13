package com.grippo.performance.trend

import androidx.compose.runtime.Immutable
import com.grippo.core.foundation.models.BaseLoader

@Immutable
public sealed interface PerformanceTrendLoader : BaseLoader {
    @Immutable
    public data object Content : PerformanceTrendLoader
}
