package com.grippo.entity.domain.user

import com.grippo.data.features.api.user.models.User
import com.grippo.services.database.models.UserPack
import com.grippo.toolkit.logger.AppLogger

public fun UserPack.toDomain(): User? {
    val mappedStats = AppLogger.Mapping.log(stats?.toDomain()) {
        "UserPack ${user.id} has unrecognized stats: $stats"
    } ?: return null

    return user.toDomain(stats = mappedStats)
}