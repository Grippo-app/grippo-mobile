package com.grippo.data.features.api.training

import com.grippo.data.features.api.training.models.SetTraining
import com.grippo.data.features.api.user.UserFeature

public class UpdateTrainingUseCase(
    public val trainingFeature: TrainingFeature,
    public val userFeature: UserFeature,
) {
    public suspend fun execute(id: String, training: SetTraining): Result<String?> {
        val result = trainingFeature.updateTraining(id, training)

        result.onSuccess {
            userFeature.getUser().getOrNull()
        }

        return result
    }
}