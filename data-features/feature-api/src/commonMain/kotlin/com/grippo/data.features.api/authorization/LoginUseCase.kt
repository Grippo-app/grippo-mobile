package com.grippo.data.features.api.authorization

import com.grippo.data.features.api.excluded.equipments.ExcludedEquipmentsFeature
import com.grippo.data.features.api.excluded.muscles.ExcludedMusclesFeature
import com.grippo.data.features.api.exercise.example.ExerciseExampleFeature
import com.grippo.data.features.api.user.UserFeature

public class LoginUseCase(
    private val authorizationFeature: AuthorizationFeature,
    private val userFeature: UserFeature,
    private val excludedMusclesFeature: ExcludedMusclesFeature,
    private val excludedEquipmentsFeature: ExcludedEquipmentsFeature,
    private val exerciseExampleFeature: ExerciseExampleFeature
) {
    public suspend fun executeEmail(email: String, password: String): Boolean {
        authorizationFeature.login(email, password).getOrThrow()

        val hasProfile = userFeature.getUser().getOrThrow()

        if (hasProfile) {
            excludedMusclesFeature.getExcludedMuscles().getOrThrow()
            excludedEquipmentsFeature.getExcludedEquipments().getOrThrow()
            exerciseExampleFeature.getExerciseExamples().getOrThrow()
        }

        return hasProfile
    }

    public suspend fun executeGoogle(token: String): Boolean {
        authorizationFeature.google(token).getOrThrow()

        val hasProfile = userFeature.getUser().getOrThrow()

        if (hasProfile) {
            excludedMusclesFeature.getExcludedMuscles().getOrThrow()
            excludedEquipmentsFeature.getExcludedEquipments().getOrThrow()
            exerciseExampleFeature.getExerciseExamples().getOrThrow()
        }

        return hasProfile
    }

    public suspend fun executeApple(code: String): Boolean {
        authorizationFeature.apple(code).getOrThrow()

        val hasProfile = userFeature.getUser().getOrThrow()

        if (hasProfile) {
            excludedMusclesFeature.getExcludedMuscles().getOrThrow()
            excludedEquipmentsFeature.getExcludedEquipments().getOrThrow()
            exerciseExampleFeature.getExerciseExamples().getOrThrow()
        }

        return hasProfile
    }
}
