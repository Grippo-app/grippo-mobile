package com.grippo.data.features.api.training

import com.grippo.data.features.api.training.models.SetTraining
import com.grippo.data.features.api.user.UserFeature

public class UpdateTrainingUseCase(
    public val trainingFeature: TrainingFeature,
    public val userFeature: UserFeature,
) {
    public suspend fun execute(id: String, training: SetTraining): String {
        val savedId = trainingFeature.updateTraining(id = id, training = training)
        check(userFeature.getUser()) {
            "The required user profile could not be refreshed"
        }
        return savedId
    }
}
