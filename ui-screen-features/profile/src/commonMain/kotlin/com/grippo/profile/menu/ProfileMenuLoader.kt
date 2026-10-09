package com.grippo.profile.menu

import androidx.compose.runtime.Immutable
import com.grippo.core.foundation.models.BaseLoader

@Immutable
internal sealed interface ProfileMenuLoader : BaseLoader {
    @Immutable
    data object User : ProfileMenuLoader
}
