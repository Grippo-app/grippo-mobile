package com.grippo.data.features.api.goal

import com.grippo.data.features.api.local.settings.LocalSettingsFeature
import com.grippo.toolkit.date.utils.DateTimeUtils
import kotlinx.coroutines.flow.firstOrNull
import kotlin.time.Duration.Companion.days

public class GoalSetupSuggestionUseCase(
    private val goalFeature: GoalFeature,
    private val localSettingsFeature: LocalSettingsFeature,
) {

    public companion object {
        internal val SNOOZE_PERIOD = 14.days
    }

    public suspend fun shouldSuggest(): Boolean {
        val goal = goalFeature.observeGoal().firstOrNull()

        if (goal != null && !DateTimeUtils.isPast(goal.target)) return false

        val lastShown = localSettingsFeature.observeLastGoalSuggestionShownAt().firstOrNull()

        if (goal != null) {
            if (lastShown == null || lastShown < goal.target) return true
        }

        return lastShown == null || DateTimeUtils.ago(lastShown) >= SNOOZE_PERIOD
    }

    public suspend fun markShown() {
        localSettingsFeature
            .setLastGoalSuggestionShownAt(DateTimeUtils.now())
            .getOrThrow()
    }
}
