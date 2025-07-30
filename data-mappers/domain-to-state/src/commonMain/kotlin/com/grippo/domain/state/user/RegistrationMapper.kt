package com.grippo.domain.state.user

import com.grippo.data.features.api.authorization.models.SetRegistration
import com.grippo.network.dto.auth.RegisterBody

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