package com.grippo.profile.goal

import androidx.compose.runtime.Immutable
import com.grippo.core.foundation.models.BaseLoader

@Immutable
internal sealed interface ProfileGoalLoader : BaseLoader {
    @Immutable
    data object SaveButton : ProfileGoalLoader
}
