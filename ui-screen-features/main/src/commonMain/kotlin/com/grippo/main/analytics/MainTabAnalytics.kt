package com.grippo.main.analytics

import com.grippo.analytics.main.models.MainTab
import com.grippo.screen.api.MainRouter

internal fun MainRouter.toAnalytics(): MainTab = when (this) {
    MainRouter.Home -> MainTab.Home
    is MainRouter.Calendar -> MainTab.Calendar
    MainRouter.Library -> MainTab.Library
    MainRouter.Profile -> MainTab.Profile
}
