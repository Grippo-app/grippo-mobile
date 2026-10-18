package com.grippo.data.features.api.training

import com.grippo.data.features.api.user.UserFeature

public class DeleteTrainingUseCase(
    public val trainingFeature: TrainingFeature,
    public val userFeature: UserFeature,
) {
    public suspend fun execute(id: String): Unit {
        trainingFeature.deleteTraining(id)
        check(userFeature.getUser()) {
            "The required user profile could not be refreshed"
        }
    }
}
