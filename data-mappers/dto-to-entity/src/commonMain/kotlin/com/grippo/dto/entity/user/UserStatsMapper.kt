package com.grippo.dto.entity.user

import com.grippo.services.backend.dto.user.UserStatsResponse
import com.grippo.services.database.entity.UserStatsEntity
import com.grippo.toolkit.logger.AppLogger

public fun UserStatsResponse?.toEntityOrNull(userId: String): UserStatsEntity? {
    val entityTrainingsCount = AppLogger.Mapping.log(this?.trainingsCount) {
        "UserStatsResponse.trainingsCount is null for user $userId"
    } ?: return null

    val entityTotalDuration = AppLogger.Mapping.log(this?.totalDuration) {
        "UserStatsResponse.totalDuration is null for user $userId"
    } ?: return null

    val entityTotalVolume = AppLogger.Mapping.log(this?.totalVolume) {
        "UserStatsResponse.totalVolume is null for user $userId"
    } ?: return null

    val entityTotalRepetitions = AppLogger.Mapping.log(this?.totalRepetitions) {
        "UserStatsResponse.totalRepetitions is null for user $userId"
    } ?: return null

    return UserStatsEntity(
        userId = userId,
        trainingsCount = entityTrainingsCount,
        totalDuration = entityTotalDuration,
        totalVolume = entityTotalVolume,
        totalRepetitions = entityTotalRepetitions
    )
}