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
    public suspend fun execute(email: String, password: String) {
        authorizationFeature.login(email, password).getOrThrow()
        userFeature.getUser().getOrThrow()
        excludedMusclesFeature.getExcludedMuscles().getOrThrow()
        excludedEquipmentsFeature.getExcludedEquipments().getOrThrow()
        exerciseExampleFeature.getExerciseExamples().getOrThrow()
    }
}