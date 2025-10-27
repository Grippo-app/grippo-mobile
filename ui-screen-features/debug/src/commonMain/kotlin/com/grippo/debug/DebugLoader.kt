package com.grippo.debug

import androidx.compose.runtime.Immutable
import com.grippo.core.foundation.models.BaseLoader

@Immutable
public sealed interface DebugLoader : BaseLoader {
    public data object Logs : DebugLoader
    public data object GenerateTraining : DebugLoader
}