package com.grippo.data.features.api

import com.grippo.data.features.api.authorization.LoginUseCase
import com.grippo.data.features.api.authorization.LogoutUseCase
import com.grippo.data.features.api.authorization.RegisterUseCase
import com.grippo.data.features.api.exercise.example.UserExerciseExamplesUseCase
import com.grippo.data.features.api.goal.GoalSetupSuggestionUseCase
import com.grippo.data.features.api.metrics.distribution.ExerciseDistributionUseCase
import com.grippo.data.features.api.metrics.distribution.MuscleLoadingSummaryUseCase
import com.grippo.data.features.api.metrics.engagement.TrainingDigestUseCase
import com.grippo.data.features.api.metrics.engagement.TrainingStreakUseCase
import com.grippo.data.features.api.metrics.performance.EstimatedOneRepMaxUseCase
import com.grippo.data.features.api.metrics.performance.ExerciseSpotlightUseCase
import com.grippo.data.features.api.metrics.performance.PerformanceTrendUseCase
import com.grippo.data.features.api.metrics.profile.GoalFollowingUseCase
import com.grippo.data.features.api.metrics.profile.TrainingLoadProfileUseCase
import com.grippo.data.features.api.metrics.volume.TrainingTotalUseCase
import com.grippo.data.features.api.metrics.volume.VolumeSeriesUseCase
import com.grippo.data.features.api.training.DeleteTrainingUseCase
import com.grippo.data.features.api.training.ExerciseValidatorUseCase
import com.grippo.data.features.api.training.GenerateTrainingUseCase
import com.grippo.data.features.api.training.SetTrainingUseCase
import com.grippo.data.features.api.training.TrainingTimelineUseCase
import com.grippo.data.features.api.training.UpdateTrainingUseCase
import com.grippo.data.features.api.user.CreateProfileUseCase
import com.grippo.data.features.api.weight.history.UpdateWeightUseCase
import org.koin.core.annotation.Module
import org.koin.dsl.module
import kotlin.jvm.JvmName
import org.koin.core.module.Module as ModuleObject

@Module
public class FeatureApiModule {

    @get:JvmName("module")
    public val module: ModuleObject = module {
        single {
            LoginUseCase(
                authorizationFeature = get(),
                userFeature = get(),
                excludedMusclesFeature = get(),
                excludedEquipmentsFeature = get(),
                exerciseExampleFeature = get(),
                weightHistoryFeature = get(),
                goalFeature = get()
            )
        }

        single {
            LogoutUseCase(
                authorizationFeature = get(),
            )
        }

        single {
            SetTrainingUseCase(
                trainingFeature = get(),
                userFeature = get()
            )
        }

        single {
            UpdateTrainingUseCase(
                trainingFeature = get(),
                userFeature = get()
            )
        }

        single {
            DeleteTrainingUseCase(
                trainingFeature = get(),
                userFeature = get()
            )
        }

        single {
            ExerciseValidatorUseCase()
        }

        single {
            UpdateWeightUseCase(
                userFeature = get(),
                weightHistoryFeature = get()
            )
        }

        single {
            RegisterUseCase(
                authorizationFeature = get(),
                userFeature = get(),
                excludedMusclesFeature = get(),
                excludedEquipmentsFeature = get(),
                exerciseExampleFeature = get(),
                goalFeature = get()
            )
        }

        single {
            CreateProfileUseCase(
                userFeature = get(),
                excludedMusclesFeature = get(),
                excludedEquipmentsFeature = get(),
                exerciseExampleFeature = get(),
                weightHistoryFeature = get()
            )
        }

        single {
            UserExerciseExamplesUseCase(
                excludedMusclesFeature = get(),
                excludedEquipmentsFeature = get(),
                exerciseExampleFeature = get(),
                userFeature = get()
            )
        }

        single {
            GenerateTrainingUseCase(
                exerciseExampleFeature = get(),
            )
        }

        single {
            MuscleLoadingSummaryUseCase(
                exerciseExampleFeature = get(),
                muscleFeature = get(),
            )
        }

        single {
            ExerciseDistributionUseCase()
        }

        single {
            ExerciseSpotlightUseCase()
        }

        single {
            TrainingStreakUseCase(
                userFeature = get(),
            )
        }

        single {
            TrainingDigestUseCase()
        }

        single {
            TrainingTimelineUseCase()
        }

        single {
            TrainingLoadProfileUseCase(
                exerciseExampleFeature = get()
            )
        }

        single {
            GoalFollowingUseCase(
                exerciseExampleFeature = get(),
                goalFeature = get()
            )
        }

        single {
            GoalSetupSuggestionUseCase(
                goalFeature = get(),
                localSettingsFeature = get(),
            )
        }

        single {
            PerformanceTrendUseCase(
                userFeature = get(),
            )
        }

        single {
            VolumeSeriesUseCase()
        }

        single {
            TrainingTotalUseCase()
        }

        single {
            EstimatedOneRepMaxUseCase()
        }
    }
}
