package com.grippo.analytics.goal.setup.suggestion.internal

import com.grippo.analytics.goal.setup.suggestion.GoalSetupSuggestionAnalytics
import com.grippo.analytics.goal.setup.suggestion.internal.schema.GoalSetupSuggestionEventMatrix
import com.grippo.analytics.goal.setup.suggestion.models.GoalSetupSuggestionEvent
import com.grippo.toolkit.analytics.Analytics
import org.koin.core.annotation.Single

@Single(binds = [GoalSetupSuggestionAnalytics::class])
internal class GoalSetupSuggestionAnalyticsImpl(private val analytics: Analytics) : GoalSetupSuggestionAnalytics {
    override fun trackEvent(event: GoalSetupSuggestionEvent) {
        try {
            analytics.track(GoalSetupSuggestionEventMatrix.event(event))
        } catch (_: Exception) {
            // Reporting cannot block goal setup navigation or returning to the caller.
        }
    }
}
