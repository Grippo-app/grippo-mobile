package com.grippo.data.features.api

import com.grippo.data.features.api.authorization.LoginUseCase
import com.grippo.data.features.api.authorization.RegisterUseCase
import com.grippo.data.features.api.exercise.example.UserExerciseExamplesUseCase
import com.grippo.data.features.api.metrics.EstimatedOneRepMaxUseCase
import com.grippo.data.features.api.metrics.ExerciseDistributionUseCase
import com.grippo.data.features.api.metrics.ExerciseSpotlightUseCase
import com.grippo.data.features.api.metrics.MuscleLoadTimelineUseCase
import com.grippo.data.features.api.metrics.MuscleLoadingSummaryUseCase
import com.grippo.data.features.api.metrics.PerformanceTrendUseCase
import com.grippo.data.features.api.metrics.TrainingDigestUseCase
import com.grippo.data.features.api.metrics.TrainingStreakUseCase
import com.grippo.data.features.api.metrics.TrainingTotalUseCase
import com.grippo.data.features.api.metrics.VolumeSeriesUseCase
import com.grippo.data.features.api.training.GenerateTrainingUseCase
import com.grippo.data.features.api.training.TrainingTimelineUseCase
import com.grippo.data.features.api.user.CreateProfileUseCase
import org.koin.core.annotation.Module
import org.koin.dsl.module
import kotlin.jvm.JvmName

@Module
public class FeatureApiModule {

    @get:JvmName("module")
    public val module: org.koin.core.module.Module = module {
        single {
            LoginUseCase(
                authorizationFeature = get(),
                userFeature = get(),
                excludedMusclesFeature = get(),
                excludedEquipmentsFeature = get(),
                exerciseExampleFeature = get()
            )
        }

        single {
            RegisterUseCase(
                authorizationFeature = get(),
                userFeature = get(),
                excludedMusclesFeature = get(),
                excludedEquipmentsFeature = get(),
                exerciseExampleFeature = get()
            )
        }

        single {
            CreateProfileUseCase(
                userFeature = get(),
                excludedMusclesFeature = get(),
                excludedEquipmentsFeature = get(),
                exerciseExampleFeature = get()
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
            MuscleLoadTimelineUseCase(
                exerciseExampleFeature = get(),
                muscleFeature = get(),
            )
        }

        single {
            ExerciseDistributionUseCase()
        }

        single {
            ExerciseSpotlightUseCase(
                exerciseExampleFeature = get(),
            )
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
