package com.grippo.statistics

import com.grippo.core.foundation.BaseViewModel
import com.grippo.core.state.datetime.PeriodState
import com.grippo.core.state.examples.ExerciseExampleState
import com.grippo.core.state.muscles.MuscleGroupState
import com.grippo.core.state.muscles.MuscleRepresentationState
import com.grippo.core.state.trainings.ExerciseState
import com.grippo.core.state.trainings.TrainingState
import com.grippo.data.features.api.exercise.example.ExerciseExampleFeature
import com.grippo.data.features.api.exercise.example.models.ExerciseExample
import com.grippo.data.features.api.muscle.MuscleFeature
import com.grippo.data.features.api.muscle.models.MuscleGroup
import com.grippo.design.resources.provider.providers.ColorProvider
import com.grippo.design.resources.provider.providers.StringProvider
import com.grippo.dialog.api.DialogConfig
import com.grippo.domain.state.exercise.example.toState
import com.grippo.domain.state.muscles.toState
import com.grippo.toolkit.calculation.AnalyticsApi
import com.grippo.toolkit.date.utils.DateRange
import com.grippo.toolkit.date.utils.DateTimeUtils
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toPersistentList
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.onEach

@OptIn(FlowPreview::class)
public class StatisticsViewModel(
    config: DialogConfig.Statistics,
    muscleFeature: MuscleFeature,
    colorProvider: ColorProvider,
    stringProvider: StringProvider,
    private val exerciseExampleFeature: ExerciseExampleFeature,
) : BaseViewModel<StatisticsState, StatisticsDirection, StatisticsLoader>(
    StatisticsState(
        trainings = (config as? DialogConfig.Statistics.Trainings)?.trainings
            ?.toPersistentList() ?: persistentListOf(),
        exercises = (config as? DialogConfig.Statistics.Exercises)?.exercises
            ?.toPersistentList() ?: persistentListOf(),
    )
), StatisticsContract {

    private val analytics = AnalyticsApi(stringProvider, colorProvider)

    init {
        muscleFeature.observeMuscles()
            .onEach(::provideMuscles)
            .safeLaunch()

        state
            .map { collectExampleIds(it.trainings, it.exercises) }
            .filter { it.isNotEmpty() }
            .distinctUntilChanged()
            .flatMapLatest(exerciseExampleFeature::observeExerciseExamples)
            .onEach(::provideExerciseExamples)
            .safeLaunch()

        state
            .map { s -> listOf(s.mode, s.trainings, s.exercises, s.examples, s.muscles) }
            .debounce(200)
            .distinctUntilChanged()
            .mapLatest { generateStatistics() }
            .safeLaunch(loader = StatisticsLoader.Charts)
    }

    private fun provideMuscles(list: List<MuscleGroup>) {
        update { it.copy(muscles = list.toState()) }
    }

    private fun provideExerciseExamples(list: List<ExerciseExample>) {
        update { it.copy(examples = list.toState()) }
    }

    override fun onBack() {
        navigateTo(StatisticsDirection.Back)
    }

    private suspend fun generateStatistics() {
        val current = state.value
        val muscles = current.muscles
        val examples = current.examples

        when (current.mode) {
            StatisticsMode.Trainings -> {
                val period = current.trainings.detectPeriodState()

                provideTrainingStatistics(
                    trainings = current.trainings,
                    period = period,
                    muscles = muscles,
                    examples = examples
                )
            }

            StatisticsMode.Exercises -> provideExerciseStatistics(
                exercises = current.exercises,
                muscles = muscles,
                examples = examples
            )
        }
    }

    private suspend fun provideTrainingStatistics(
        trainings: ImmutableList<TrainingState>,
        period: PeriodState,
        muscles: ImmutableList<MuscleGroupState<MuscleRepresentationState.Plain>>,
        examples: ImmutableList<ExerciseExampleState>,
    ) {
        if (trainings.isEmpty()) {
            clearStatistics()
            return
        }

        val totalMetrics = analytics.metricsFromTrainings(
            trainings = trainings
        )

        val categoryDistribution = analytics.categoryDistributionFromTrainings(
            trainings = trainings,
            period = period
        )

        val weightTypeDistribution = analytics.weightTypeDistributionFromTrainings(
            trainings = trainings,
            period = period
        )

        val forceTypeDistribution = analytics.forceTypeDistributionFromTrainings(
            trainings = trainings,
            period = period
        )

        val exerciseVolume = analytics.volumeFromTrainings(
            trainings = trainings,
            period = period
        )

        val muscleLoad = analytics.muscleLoadFromTrainings(
            trainings = trainings,
            period = period,
            examples = examples,
            groups = muscles
        )

        val heatmap = analytics.heatmapFromTrainings(
            trainings = trainings,
            period = period,
            examples = examples,
            groups = muscles
        )

        update {
            it.copy(
                totalMetrics = totalMetrics,
                exerciseVolume = exerciseVolume,
                categoryDistribution = categoryDistribution,
                weightTypeDistribution = weightTypeDistribution,
                forceTypeDistribution = forceTypeDistribution,
                muscleLoad = muscleLoad,
                temporalHeatmap = heatmap,
            )
        }
    }

    private suspend fun provideExerciseStatistics(
        exercises: ImmutableList<ExerciseState>,
        muscles: ImmutableList<MuscleGroupState<MuscleRepresentationState.Plain>>,
        examples: ImmutableList<ExerciseExampleState>,
    ) {
        if (exercises.isEmpty()) {
            clearStatistics()
            return
        }

        val totalMetrics = analytics.metricsFromExercises(exercises)
        val categoryDistribution = analytics.categoryDistributionFromExercises(exercises)
        val weightTypeDistribution = analytics.weightTypeDistributionFromExercises(exercises)
        val forceTypeDistribution = analytics.forceTypeDistributionFromExercises(exercises)
        val exerciseVolume = analytics.volumeFromExercises(exercises)
        val muscleLoad = analytics.muscleLoadFromExercises(exercises, examples, muscles)

        update {
            it.copy(
                totalMetrics = totalMetrics,
                exerciseVolume = exerciseVolume,
                categoryDistribution = categoryDistribution,
                weightTypeDistribution = weightTypeDistribution,
                forceTypeDistribution = forceTypeDistribution,
                muscleLoad = muscleLoad,
                temporalHeatmap = null,
            )
        }
    }

    private fun clearStatistics() {
        update {
            it.copy(
                totalMetrics = null,
                exerciseVolume = null,
                categoryDistribution = null,
                weightTypeDistribution = null,
                forceTypeDistribution = null,
                muscleLoad = null,
                temporalHeatmap = null,
            )
        }
    }

    private fun collectExampleIds(
        trainings: ImmutableList<TrainingState>,
        exercises: ImmutableList<ExerciseState>
    ): List<String> {
        val trainingIds = trainings
            .flatMap { training -> training.exercises.map { it.exerciseExample.id } }
        val exerciseIds = exercises.map { it.exerciseExample.id }
        return (trainingIds + exerciseIds).distinct()
    }

}

internal fun List<TrainingState>.detectPeriodState(): PeriodState {
    if (isEmpty()) {
        return PeriodState.ThisWeek
    }

    val sorted = sortedBy { it.createdAt }
    val start = DateTimeUtils.startOfDay(sorted.first().createdAt)
    val end = DateTimeUtils.endOfDay(sorted.last().createdAt)

    return PeriodState.Custom(
        range = DateRange(from = start, to = end),
        limitations = DateTimeUtils.trailingYear()
    )
}
