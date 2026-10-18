package com.grippo.data.features.api.authorization

import com.grippo.data.features.api.excluded.equipments.ExcludedEquipmentsFeature
import com.grippo.data.features.api.excluded.muscles.ExcludedMusclesFeature
import com.grippo.data.features.api.goal.GoalFeature
import com.grippo.data.features.api.user.UserFeature
import com.grippo.data.features.api.weight.history.WeightHistoryFeature

public class RegisterUseCase(
    private val authorizationFeature: AuthorizationFeature,
    private val userFeature: UserFeature,
    private val excludedMusclesFeature: ExcludedMusclesFeature,
    private val excludedEquipmentsFeature: ExcludedEquipmentsFeature,
    private val weightHistoryFeature: WeightHistoryFeature,
    private val goalFeature: GoalFeature,
) {
    public suspend fun createAccount(email: String, password: String): Boolean {
        authorizationFeature.register(email, password)
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
