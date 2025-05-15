package com.grippo.data.features.api.user

import com.grippo.data.features.api.excluded.equipments.ExcludedEquipmentsFeature
import com.grippo.data.features.api.excluded.muscles.ExcludedMusclesFeature

public class GetUserUseCase(
    private val userFeature: UserFeature,
    private val excludedEquipmentsFeature: ExcludedEquipmentsFeature,
    private val excludedMusclesFeature: ExcludedMusclesFeature
) {
    public suspend fun execute() {
        userFeature.getUser().getOrThrow()
        excludedMusclesFeature.getExcludedMuscles().getOrThrow()
        excludedEquipmentsFeature.getExcludedEquipments().getOrThrow()
    }
}