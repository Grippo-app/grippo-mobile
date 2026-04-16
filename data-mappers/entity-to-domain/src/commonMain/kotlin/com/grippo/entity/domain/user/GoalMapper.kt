package com.grippo.entity.domain.user

import com.grippo.data.features.api.goal.models.Goal
import com.grippo.data.features.api.goal.models.GoalPrimaryGoalEnum
import com.grippo.data.features.api.goal.models.GoalSecondaryGoalEnum
import com.grippo.data.features.api.goal.models.PersonalizationKeyEnum
import com.grippo.services.database.entity.GoalEntity
import com.grippo.toolkit.date.utils.DateTimeUtils
import com.grippo.toolkit.logger.AppLogger

public fun GoalEntity.toDomain(): Goal? {
    val mappedPrimaryGoal = AppLogger.Mapping.log(GoalPrimaryGoalEnum.of(primaryGoal)) {
        "GoalEntity $userId has unrecognized primaryGoal: $primaryGoal"
    } ?: return null

    return Goal(
        primaryGoal = mappedPrimaryGoal,
        secondaryGoal = GoalSecondaryGoalEnum.of(secondaryGoal),
        target = DateTimeUtils.toLocalDateTime(target),
        personalizations = personalizations.mapNotNull {
            PersonalizationKeyEnum.of(it)
        },
        confidence = confidence,
        createdAt = createdAt,
        updatedAt = updatedAt,
        lastConfirmedAt = lastConfirmedAt,
    )
}
