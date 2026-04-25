package com.grippo.data.features.api.training

import com.grippo.data.features.api.training.models.SetTraining
import com.grippo.data.features.api.user.UserFeature

public class SetTrainingUseCase(
    public val trainingFeature: TrainingFeature,
    public val userFeature: UserFeature,
) {
    public suspend fun execute(training: SetTraining): Result<String?> {
        val result = trainingFeature.setTraining(training)

        result.onSuccess {
            userFeature.getUser().getOrNull()
        }

        return result
    }
}