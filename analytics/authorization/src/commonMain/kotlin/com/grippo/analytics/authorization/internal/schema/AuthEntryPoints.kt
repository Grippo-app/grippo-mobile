package com.grippo.analytics.authorization.internal.schema

import com.grippo.toolkit.analytics.context.EntryPoint
import com.grippo.analytics.authorization.models.EntryPoint as AuthEntryPoint

internal object AuthEntryPoints {
    val SessionCheck = EntryPoint("session_check")
    fun from(entryPoint: AuthEntryPoint): EntryPoint = when (entryPoint) {
        AuthEntryPoint.LaunchWithoutSession -> LaunchWithoutSession
        AuthEntryPoint.TokenAbsent -> TokenAbsent
        AuthEntryPoint.SessionCheckFailed -> SessionCheckFailed
        AuthEntryPoint.ProfileSetupReturn -> ProfileSetupReturn
        AuthEntryPoint.RestoredScenario -> RestoredScenario
    }

    private val LaunchWithoutSession = EntryPoint("launch_without_session")
    private val TokenAbsent = EntryPoint("token_absent")
    private val SessionCheckFailed = EntryPoint("session_check_failed")
    private val RestoredScenario = EntryPoint("restored_scenario")
    private val ProfileSetupReturn = EntryPoint("profile_setup_return")
}
