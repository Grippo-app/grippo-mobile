package com.grippo.profile.body

import androidx.compose.runtime.Immutable
import com.grippo.core.foundation.models.BaseLoader

@Immutable
internal sealed interface ProfileBodyLoader : BaseLoader {
    @Immutable
    data object User : ProfileBodyLoader
    @Immutable
    data object RefreshHistory : ProfileBodyLoader
    @Immutable
    data object ApplyBodyChanges : ProfileBodyLoader
}
