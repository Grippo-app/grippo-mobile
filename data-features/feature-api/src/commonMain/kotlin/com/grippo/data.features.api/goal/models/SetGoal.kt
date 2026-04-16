package com.grippo.data.features.api.goal.models

import kotlinx.datetime.LocalDateTime

public data class SetGoal(
    val primaryGoal: GoalPrimaryGoalEnum,
    val secondaryGoal: GoalSecondaryGoalEnum? = null,
    val target: LocalDateTime,
    val personalizations: List<PersonalizationKeyEnum> = emptyList(),
)
