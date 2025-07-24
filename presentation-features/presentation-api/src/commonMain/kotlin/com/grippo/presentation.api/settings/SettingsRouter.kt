package com.grippo.presentation.api.settings

import com.grippo.core.models.BaseRouter
import kotlinx.serialization.Serializable

@Serializable
public sealed class SettingsRouter : BaseRouter {
    @Serializable
    public data object System : SettingsRouter()
}