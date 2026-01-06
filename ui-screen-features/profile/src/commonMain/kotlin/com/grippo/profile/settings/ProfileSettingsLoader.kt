package com.grippo.profile.settings

import androidx.compose.runtime.Immutable
import com.grippo.core.foundation.models.BaseLoader

@Immutable
internal sealed interface ProfileSettingsLoader : BaseLoader {
    @Immutable
    data object DeleteAccountButton : ProfileSettingsLoader
}