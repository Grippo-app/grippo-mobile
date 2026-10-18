package com.grippo.data.features.api.weight.history

import com.grippo.data.features.api.user.UserFeature

public class UpdateWeightUseCase(
    private val userFeature: UserFeature,
    private val weightHistoryFeature: WeightHistoryFeature
) {
    public suspend fun execute(value: Float): Unit {
        weightHistoryFeature.updateWeight(value)
        check(userFeature.getUser()) {
            "The required user profile could not be refreshed"
        }
    }
}
