package com.grippo.domain.network.user

import com.grippo.backend.dto.auth.RegisterBody
import com.grippo.data.features.api.authorization.models.SetRegistration

public fun SetRegistration.toBody(): RegisterBody {
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