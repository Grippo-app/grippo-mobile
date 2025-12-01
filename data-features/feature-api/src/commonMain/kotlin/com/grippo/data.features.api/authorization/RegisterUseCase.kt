package com.grippo.data.features.api.authorization

import com.grippo.data.features.api.excluded.equipments.ExcludedEquipmentsFeature
import com.grippo.data.features.api.excluded.muscles.ExcludedMusclesFeature
import com.grippo.data.features.api.exercise.example.ExerciseExampleFeature
import com.grippo.data.features.api.user.UserFeature

public class RegisterUseCase(
    private val authorizationFeature: AuthorizationFeature,
    private val userFeature: UserFeature,
    private val excludedMusclesFeature: ExcludedMusclesFeature,
    private val excludedEquipmentsFeature: ExcludedEquipmentsFeature,
    private val exerciseExampleFeature: ExerciseExampleFeature
) {
    public suspend fun execute(email: String, password: String): Boolean {
        authorizationFeature.register(email, password).getOrThrow()

        val hasProfile = userFeature.getUser().getOrThrow()

        if (hasProfile) {
            excludedMusclesFeature.getExcludedMuscles().getOrThrow()
            excludedEquipmentsFeature.getExcludedEquipments().getOrThrow()
            exerciseExampleFeature.getExerciseExamples().getOrThrow()
        }

        return hasProfile
    }
}
