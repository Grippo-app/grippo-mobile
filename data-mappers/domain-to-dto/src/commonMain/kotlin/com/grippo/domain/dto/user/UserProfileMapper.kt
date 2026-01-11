package com.grippo.domain.dto.user

import com.grippo.data.features.api.user.models.CreateUserProfile
import com.grippo.services.backend.dto.user.CreateProfileBody

public fun CreateUserProfile.toBody(): CreateProfileBody {
    return CreateProfileBody(
        name = name,
        weight = weight,
        height = height,
        experience = experience.key,
        excludeMuscleIds = excludeMuscleIds.ifEmpty { null },
        excludeEquipmentIds = excludeEquipmentIds.ifEmpty { null }
    )
}
