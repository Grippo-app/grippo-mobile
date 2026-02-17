package com.grippo.profile.body

import androidx.compose.runtime.Immutable
import com.grippo.core.foundation.models.BaseLoader

@Immutable
internal sealed interface ProfileBodyLoader : BaseLoader {
    @Immutable
    data object ApplyBodyChanges : ProfileBodyLoader
}