package com.grippo.domain.mapper

import com.grippo.data.features.api.authorization.models.SetRegistration
import com.grippo.network.dto.RegisterDto

public fun SetRegistration.toDto(): RegisterDto {
    return RegisterDto(
        email = email,
        password = password,
        name = name,
        weight = weight.toDouble(),
        experience = experience.key,
        height = height,
        excludeMuscleIds = excludeMuscleIds,
        excludeEquipmentIds = excludeEquipmentIds
    )
}