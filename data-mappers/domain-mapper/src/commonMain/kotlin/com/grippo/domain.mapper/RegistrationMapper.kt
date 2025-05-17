package com.grippo.domain.mapper

import com.grippo.data.features.api.authorization.models.SetRegistration
import com.grippo.network.dto.RegisterBody

public fun SetRegistration.toDto(): RegisterBody {
    return RegisterBody(
        email = email,
        password = password,
        name = name,
        weight = weight,
        experience = experience.key,
        height = height,
        excludeMuscleIds = excludeMuscleIds,
        excludeEquipmentIds = excludeEquipmentIds
    )
}