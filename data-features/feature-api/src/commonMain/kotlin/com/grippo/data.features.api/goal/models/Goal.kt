package com.grippo.data.features.api.goal.models

import kotlinx.datetime.LocalDateTime

public data class Goal(
    val primaryGoal: GoalPrimaryGoalEnum,
    val secondaryGoal: GoalSecondaryGoalEnum?,
    val target: LocalDateTime,
    val personalizations: List<PersonalizationKeyEnum>,
    val confidence: Double,
    val createdAt: LocalDateTime,
    val updatedAt: LocalDateTime,
    val lastConfirmedAt: String?,
)
