package com.grippo.data.features.api.authorization

import com.grippo.data.features.api.excluded.equipments.ExcludedEquipmentsFeature
import com.grippo.data.features.api.excluded.muscles.ExcludedMusclesFeature
import com.grippo.data.features.api.goal.GoalFeature
import com.grippo.data.features.api.user.UserFeature
import com.grippo.data.features.api.weight.history.WeightHistoryFeature

public class LoginUseCase(
    private val authorizationFeature: AuthorizationFeature,
    private val userFeature: UserFeature,
    private val excludedMusclesFeature: ExcludedMusclesFeature,
    private val excludedEquipmentsFeature: ExcludedEquipmentsFeature,
    private val weightHistoryFeature: WeightHistoryFeature,
    private val goalFeature: GoalFeature,
) {
    public suspend fun loginEmail(email: String, password: String): Boolean {
        authorizationFeature.login(email, password).getOrThrow()
        return prepare()
    }

    public suspend fun loginGoogle(token: String): Boolean {
        authorizationFeature.google(token).getOrThrow()
        return prepare()
    }

    public suspend fun loginApple(code: String): Boolean {
        authorizationFeature.apple(code).getOrThrow()
        return prepare()
    }

    private suspend fun prepare(): Boolean {
        val hasProfile = userFeature.getUser().getOrThrow()
        if (hasProfile) {
            excludedMusclesFeature.getExcludedMuscles().getOrThrow()
            excludedEquipmentsFeature.getExcludedEquipments().getOrThrow()
            weightHistoryFeature.getWeightHistory().getOrThrow()
            goalFeature.getGoal().getOrThrow()
        }
        val pushToken = authorizationFeature.getPushToken()
        if (pushToken != null) {
            authorizationFeature.updatePushToken(pushToken).getOrThrow()
        }
        return hasProfile
    }
}
