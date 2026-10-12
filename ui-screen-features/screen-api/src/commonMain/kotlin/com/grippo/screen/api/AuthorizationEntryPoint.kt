package com.grippo.screen.api

import kotlinx.serialization.Serializable

@Serializable
public enum class AuthorizationEntryPoint {
    LaunchWithoutSession,
    TokenAbsent,
    /** Profile lookup failed during the startup session check. */
    SessionCheckFailed,
    ProfileSetupReturn,
    /** Saved routes from before entry attribution was introduced have no original source. */
    RestoredScenario,
}
