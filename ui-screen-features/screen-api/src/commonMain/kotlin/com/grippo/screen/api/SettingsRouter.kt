package com.grippo.screen.api

import com.grippo.core.models.BaseRouter
import kotlinx.serialization.Serializable

@Serializable
public sealed class SettingsRouter : BaseRouter {
    @Serializable
    public data object System : SettingsRouter()
}