package com.grippo.data.features.api.training

import com.grippo.data.features.api.training.models.SetTraining
import com.grippo.data.features.api.user.UserFeature

public class SetTrainingUseCase(
    public val trainingFeature: TrainingFeature,
    public val userFeature: UserFeature,
) {
    public suspend fun execute(training: SetTraining): String {
        val id = trainingFeature.setTraining(training)
        check(userFeature.getUser()) {
            "The required user profile could not be refreshed"
        }
        return id
    }
}
