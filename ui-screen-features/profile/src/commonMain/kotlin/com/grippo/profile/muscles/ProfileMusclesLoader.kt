package com.grippo.profile.muscles

import androidx.compose.runtime.Immutable
import com.grippo.core.models.BaseLoader

@Immutable
internal sealed interface ProfileMusclesLoader : BaseLoader {
    @Immutable
    data object ApplyButton : ProfileMusclesLoader
}