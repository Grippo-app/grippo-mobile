package com.grippo.data.features.api.user

import com.grippo.data.features.api.excluded.equipments.ExcludedEquipmentsFeature
import com.grippo.data.features.api.excluded.muscles.ExcludedMusclesFeature
import com.grippo.data.features.api.user.models.CreateUserProfile
import com.grippo.data.features.api.weight.history.WeightHistoryFeature

public class CreateProfileUseCase(
    private val userFeature: UserFeature,
    private val excludedMusclesFeature: ExcludedMusclesFeature,
    private val excludedEquipmentsFeature: ExcludedEquipmentsFeature,
    private val weightHistoryFeature: WeightHistoryFeature,
) {
    public suspend fun execute(profile: CreateUserProfile) {
        userFeature.createProfile(profile)
        check(userFeature.getUser()) {
            "Created profile could not be loaded"
        }
        excludedMusclesFeature.getExcludedMuscles()
        excludedEquipmentsFeature.getExcludedEquipments()
        weightHistoryFeature.getWeightHistory()
    }
}
