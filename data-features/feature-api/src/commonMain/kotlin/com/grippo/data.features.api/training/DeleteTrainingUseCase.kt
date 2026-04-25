package com.grippo.data.features.api.training

import com.grippo.data.features.api.user.UserFeature

public class DeleteTrainingUseCase(
    public val trainingFeature: TrainingFeature,
    public val userFeature: UserFeature,
) {
    public suspend fun execute(id: String): Result<Unit> {
        val result = trainingFeature.deleteTraining(id)

        result.onSuccess {
            userFeature.getUser().getOrNull()
        }

        return result
    }
}