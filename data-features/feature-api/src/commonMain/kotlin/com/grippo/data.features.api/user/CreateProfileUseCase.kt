package com.grippo.data.features.api.user

import com.grippo.data.features.api.excluded.equipments.ExcludedEquipmentsFeature
import com.grippo.data.features.api.excluded.muscles.ExcludedMusclesFeature
import com.grippo.data.features.api.exercise.example.ExerciseExampleFeature
import com.grippo.data.features.api.user.models.CreateUserProfile

public class CreateProfileUseCase(
    private val userFeature: UserFeature,
    private val excludedMusclesFeature: ExcludedMusclesFeature,
    private val excludedEquipmentsFeature: ExcludedEquipmentsFeature,
    private val exerciseExampleFeature: ExerciseExampleFeature
) {
    public suspend fun execute(profile: CreateUserProfile) {
        val created = userFeature.createProfile(profile).getOrThrow()
        check(created) { "Failed to create user profile" }

        val hasProfile = userFeature.getUser().getOrThrow()
        check(hasProfile) { "User profile is missing after creation" }
        excludedMusclesFeature.getExcludedMuscles().getOrThrow()
        excludedEquipmentsFeature.getExcludedEquipments().getOrThrow()
        exerciseExampleFeature.getExerciseExamples().getOrThrow()
    }
}
