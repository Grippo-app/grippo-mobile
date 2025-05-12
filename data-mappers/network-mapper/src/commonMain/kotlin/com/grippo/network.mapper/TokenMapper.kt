package com.grippo.network.mapper

import com.grippo.database.entity.TokenEntity
import com.grippo.logger.AppLogger
import com.grippo.network.dto.TokenDto

public fun TokenDto.toEntityOrNull(): TokenEntity? {
    val entityAccess = AppLogger.mapping(accessToken, { "TokenDto.accessToken is null" }) ?: return null

    return TokenEntity(
        access = entityAccess
    )
}