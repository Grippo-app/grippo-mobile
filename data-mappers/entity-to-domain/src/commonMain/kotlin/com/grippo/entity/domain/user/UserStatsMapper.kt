package com.grippo.entity.domain.user

import com.grippo.data.features.api.user.models.UserStats
import com.grippo.services.database.entity.UserStatsEntity
import kotlin.time.Duration.Companion.minutes

public fun UserStatsEntity.toDomain(): UserStats {
    return UserStats(
        trainingsCount = trainingsCount,
        totalDuration = totalDuration.minutes,
        totalVolume = totalVolume,
        totalRepetitions = totalRepetitions
    )
}