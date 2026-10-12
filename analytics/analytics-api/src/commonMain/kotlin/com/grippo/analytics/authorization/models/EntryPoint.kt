package com.grippo.analytics.authorization.models

public enum class EntryPoint {
    LaunchWithoutSession,
    TokenAbsent,
    /** Legacy profile-failure attribution retained for previously saved routes. */
    SessionCheckFailed,
    ProfileSetupReturn,
    RestoredScenario,
}
