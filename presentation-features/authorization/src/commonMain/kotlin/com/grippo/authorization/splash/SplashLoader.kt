package com.grippo.authorization.splash

import com.grippo.core.models.BaseLoader

internal sealed interface SplashLoader : BaseLoader {
    data object AppContent : SplashLoader
}