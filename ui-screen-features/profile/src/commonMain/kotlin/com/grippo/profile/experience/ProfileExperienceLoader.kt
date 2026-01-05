package com.grippo.profile.experience

import androidx.compose.runtime.Immutable
import com.grippo.core.foundation.models.BaseLoader

@Immutable
internal sealed interface ProfileExperienceLoader : BaseLoader {
    @Immutable
    data object ApplyButton : ProfileExperienceLoader
}
