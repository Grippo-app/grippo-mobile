package com.grippo.domain.dto.user

import com.grippo.data.features.api.user.models.CreateUserProfile

public fun CreateUserProfile.toBody(): com.grippo.services.backend.dto.user.CreateProfileBody {
    return com.grippo.services.backend.dto.user.CreateProfileBody(
        name = name,
        weight = weight,
        height = height,
        experience = experience.key,
        excludeMuscleIds = excludeMuscleIds.ifEmpty { null },
        excludeEquipmentIds = excludeEquipmentIds.ifEmpty { null }
    )
}
