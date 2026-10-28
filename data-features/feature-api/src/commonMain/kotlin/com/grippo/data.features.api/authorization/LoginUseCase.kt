package com.grippo.data.features.api.authorization

import com.grippo.data.features.api.equipment.EquipmentFeature
import com.grippo.data.features.api.excluded.equipments.ExcludedEquipmentsFeature
import com.grippo.data.features.api.excluded.muscles.ExcludedMusclesFeature
import com.grippo.data.features.api.goal.GoalFeature
import com.grippo.data.features.api.muscle.MuscleFeature
import com.grippo.data.features.api.user.UserFeature
import com.grippo.data.features.api.weight.history.WeightHistoryFeature

public class LoginUseCase(
    private val authorizationFeature: AuthorizationFeature,
    private val userFeature: UserFeature,
    private val excludedMusclesFeature: ExcludedMusclesFeature,
    private val excludedEquipmentsFeature: ExcludedEquipmentsFeature,
    private val weightHistoryFeature: WeightHistoryFeature,
    private val goalFeature: GoalFeature,
    private val muscleFeature: MuscleFeature,
    private val equipmentFeature: EquipmentFeature,
) {
    public suspend fun loginEmail(email: String, password: String): Boolean {
        muscleFeature.getMuscles()
        equipmentFeature.getEquipments()
        authorizationFeature.login(email, password)
        return prepare()
    }

    public suspend fun loginGoogle(token: String): Boolean {
        muscleFeature.getMuscles()
        equipmentFeature.getEquipments()
        authorizationFeature.google(token)
        return prepare()
    }

    public suspend fun loginApple(code: String): Boolean {
        muscleFeature.getMuscles()
        equipmentFeature.getEquipments()
        authorizationFeature.apple(code)
        return prepare()
    }

    private suspend fun prepare(): Boolean {
        val hasProfile = userFeature.getUser()
        if (hasProfile) {
            excludedMusclesFeature.getExcludedMuscles()
            excludedEquipmentsFeature.getExcludedEquipments()
            weightHistoryFeature.getWeightHistory()
            goalFeature.getGoal()
        }
        val pushToken = authorizationFeature.getPushToken()
        if (pushToken != null) {
            authorizationFeature.updatePushToken(pushToken)
        }
        return hasProfile
    }
}
