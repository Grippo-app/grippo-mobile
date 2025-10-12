package com.grippo.network.database.user

import com.grippo.backend.dto.auth.TokenResponse
import com.grippo.database.entity.TokenEntity
import com.grippo.logger.AppLogger

public fun TokenResponse.toEntityOrNull(): TokenEntity? {
    val entityAccess = AppLogger.Mapping.log(accessToken) {
        "TokenResponse.accessToken is null"
    } ?: return null

    val id = AppLogger.Mapping.log(id) {
        "TokenResponse.id is null"
    } ?: return null

    val refresh = AppLogger.Mapping.log(refreshToken) {
        "TokenResponse.refreshToken is null"
    } ?: return null

    return TokenEntity(
        id = id,
        access = entityAccess,
        refresh = refresh
    )
}