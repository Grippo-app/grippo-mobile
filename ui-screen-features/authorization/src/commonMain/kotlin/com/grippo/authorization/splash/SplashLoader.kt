package com.grippo.authorization.splash

import androidx.compose.runtime.Immutable
import com.grippo.core.foundation.models.BaseLoader

@Immutable
internal sealed interface SplashLoader : BaseLoader {
    @Immutable
    data object AppContent : SplashLoader
}