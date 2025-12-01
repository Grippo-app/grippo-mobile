package com.grippo.authorization.profile.creation.completed

import androidx.compose.runtime.Immutable
import com.grippo.core.foundation.models.BaseLoader

@Immutable
internal sealed interface CompletedLoader : BaseLoader {
    @Immutable
    data object ProfileCreation : CompletedLoader
}
