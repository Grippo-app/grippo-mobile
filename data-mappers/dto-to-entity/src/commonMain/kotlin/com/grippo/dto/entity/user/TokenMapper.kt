package com.grippo.dto.entity.user

import com.grippo.toolkit.logger.AppLogger

public fun com.grippo.services.backend.dto.auth.TokenResponse.toEntityOrNull(): com.grippo.services.database.entity.TokenEntity? {
    val entityAccess = AppLogger.Mapping.log(accessToken) {
        "TokenResponse.accessToken is null"
    } ?: return null

    val id = AppLogger.Mapping.log(id) {
        "TokenResponse.id is null"
    } ?: return null

    val refresh = AppLogger.Mapping.log(refreshToken) {
        "TokenResponse.refreshToken is null"
    } ?: return null

    return _root_ide_package_.com.grippo.services.database.entity.TokenEntity(
        id = id,
        access = entityAccess,
        refresh = refresh
    )
}