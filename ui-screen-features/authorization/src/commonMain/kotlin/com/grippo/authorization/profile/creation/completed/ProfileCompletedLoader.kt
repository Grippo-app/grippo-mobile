package com.grippo.authorization.profile.creation.completed

import androidx.compose.runtime.Immutable
import com.grippo.core.foundation.models.BaseLoader

@Immutable
internal sealed interface ProfileCompletedLoader : BaseLoader {
    @Immutable
    data object ProfileCreation : ProfileCompletedLoader
}
