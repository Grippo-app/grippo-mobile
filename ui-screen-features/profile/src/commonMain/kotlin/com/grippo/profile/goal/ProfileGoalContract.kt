package com.grippo.profile.goal

import androidx.compose.runtime.Immutable
import com.grippo.core.state.profile.GoalPrimaryGoalEnumState
import com.grippo.core.state.profile.GoalSecondaryGoalEnumState
import com.grippo.core.state.profile.PersonalizationKeyEnumState
import kotlinx.datetime.LocalDate

@Immutable
internal interface ProfileGoalContract {
    fun onBack()
    fun onSelectPrimary(goal: GoalPrimaryGoalEnumState)
    fun onSaveFastPath()
    fun onToggleCustomize()
    fun onSelectSecondary(goal: GoalSecondaryGoalEnumState?)
    fun onSelectTargetDate(date: LocalDate?)
    fun onToggleConstraint(constraint: PersonalizationKeyEnumState)
    fun onSave()

    @Immutable
    companion object Empty : ProfileGoalContract {
        override fun onBack() {}
        override fun onSelectPrimary(goal: GoalPrimaryGoalEnumState) {}
        override fun onSaveFastPath() {}
        override fun onToggleCustomize() {}
        override fun onSelectSecondary(goal: GoalSecondaryGoalEnumState?) {}
        override fun onSelectTargetDate(date: LocalDate?) {}
        override fun onToggleConstraint(constraint: PersonalizationKeyEnumState) {}
        override fun onSave() {}
    }
}
