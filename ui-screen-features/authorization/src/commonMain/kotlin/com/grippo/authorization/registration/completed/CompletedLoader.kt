package com.grippo.authorization.registration.completed

import androidx.compose.runtime.Immutable
import com.grippo.core.foundation.models.BaseLoader

@Immutable
internal sealed interface CompletedLoader : BaseLoader {
    @Immutable
    data object Registration : CompletedLoader
}