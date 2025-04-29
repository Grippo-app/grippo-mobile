package com.grippo.domain.mapper

import com.grippo.data.features.api.authorization.models.Registration
import com.grippo.network.dto.RegisterDto

public fun Registration.toDto(): RegisterDto {
    return RegisterDto(
        email = email,
        password = password,
        name = name,
        weight = weight,
        experience = experience,
        height = height,
        excludeMuscleIds = excludeMuscleIds,
        excludeEquipmentIds = excludeEquipmentIds
    )
}