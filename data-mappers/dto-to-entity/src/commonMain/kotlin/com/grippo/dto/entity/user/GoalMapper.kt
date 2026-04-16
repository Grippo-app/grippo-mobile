package com.grippo.dto.entity.user

import com.grippo.services.backend.dto.user.GoalResponse
import com.grippo.services.database.entity.GoalEntity
import com.grippo.toolkit.logger.AppLogger

public fun GoalResponse.toEntityOrNull(userId: String): GoalEntity? {
    val entityPrimaryGoal = AppLogger.Mapping.log(primaryGoal) {
        "GoalResponse.primaryGoal is null for user $userId"
    } ?: return null

    val entityConfidence = AppLogger.Mapping.log(confidence) {
        "GoalResponse.confidence is null for user $userId"
    } ?: return null

    val entityCreatedAt = AppLogger.Mapping.log(createdAt) {
        "GoalResponse.createdAt is null for user $userId"
    } ?: return null

    val entityUpdatedAt = AppLogger.Mapping.log(updatedAt) {
        "GoalResponse.updatedAt is null for user $userId"
    } ?: return null

    val entityTarget = AppLogger.Mapping.log(target) {
        "GoalResponse.target is null for user $userId"
    } ?: return null

    return GoalEntity(
        userId = userId,
        primaryGoal = entityPrimaryGoal,
        secondaryGoal = secondaryGoal,
        target = entityTarget,
        personalizations = personalizations ?: emptyList(),
        confidence = entityConfidence,
        createdAt = entityCreatedAt,
        updatedAt = entityUpdatedAt,
        lastConfirmedAt = lastConfirmedAt,
    )
}
