package com.grippo.data.features.api.weight.history

import com.grippo.data.features.api.user.UserFeature

public class UpdateWeightUseCase(
    private val userFeature: UserFeature,
    private val weightHistoryFeature: WeightHistoryFeature
) {
    public suspend fun execute(value: Float): Result<Unit> {
        val request = weightHistoryFeature.updateWeight(value)

        request.onSuccess {
            userFeature.getUser()
        }

        return request
    }
}
