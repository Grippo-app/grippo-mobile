package com.grippo.network.mapper

import com.grippo.database.entity.TokenEntity
import com.grippo.network.dto.TokenDto

public fun TokenDto.toEntityOrNull(): TokenEntity? {
    return TokenEntity(
        access = accessToken ?: return null
    )
}