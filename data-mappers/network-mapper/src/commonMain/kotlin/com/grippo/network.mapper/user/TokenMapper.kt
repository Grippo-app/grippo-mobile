package com.grippo.network.mapper.user

import com.grippo.database.entity.TokenEntity
import com.grippo.logger.AppLogger
import com.grippo.network.dto.auth.TokenResponse

public fun TokenResponse.toEntityOrNull(): TokenEntity? {
    val entityAccess = AppLogger.checkOrLog(accessToken) {
        "TokenResponse.accessToken is null"
    } ?: return null

    val id = AppLogger.checkOrLog(id) {
        "TokenResponse.id is null"
    } ?: return null

    val refresh = AppLogger.checkOrLog(refreshToken) {
        "TokenResponse.refreshToken is null"
    } ?: return null

    return TokenEntity(
        id = id,
        access = entityAccess,
        refresh = refresh
    )
}