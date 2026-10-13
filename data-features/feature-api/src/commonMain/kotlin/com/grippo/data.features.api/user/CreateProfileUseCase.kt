package com.grippo.data.features.api.user

import com.grippo.data.features.api.excluded.equipments.ExcludedEquipmentsFeature
import com.grippo.data.features.api.excluded.muscles.ExcludedMusclesFeature
import com.grippo.data.features.api.exercise.example.ExerciseExampleFeature
import com.grippo.data.features.api.user.models.CreateUserProfile
import com.grippo.data.features.api.weight.history.WeightHistoryFeature

public class CreateProfileUseCase(
    private val userFeature: UserFeature,
    private val excludedMusclesFeature: ExcludedMusclesFeature,
    private val excludedEquipmentsFeature: ExcludedEquipmentsFeature,
    private val exerciseExampleFeature: ExerciseExampleFeature,
    private val weightHistoryFeature: WeightHistoryFeature,
) {
    public suspend fun execute(profile: CreateUserProfile) {
        check(userFeature.createProfile(profile).getOrThrow()) {
            "Profile could not be created"
        }
        check(userFeature.getUser().getOrThrow()) {
            "Created profile could not be loaded"
        }
        excludedMusclesFeature.getExcludedMuscles().getOrThrow()
        excludedEquipmentsFeature.getExcludedEquipments().getOrThrow()
        exerciseExampleFeature.getExerciseExamples().getOrThrow()
        weightHistoryFeature.getWeightHistory().getOrThrow()
    }
}
