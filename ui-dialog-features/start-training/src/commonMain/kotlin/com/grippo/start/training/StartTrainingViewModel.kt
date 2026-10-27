package com.grippo.start.training

import com.grippo.analytics.start.training.StartTrainingAnalytics
import com.grippo.analytics.start.training.models.StartTrainingEvent
import com.grippo.analytics.start.training.models.StartTrainingOperation
import com.grippo.core.foundation.BaseViewModel
import com.grippo.core.foundation.models.ErrorRecovery
import com.grippo.core.state.formatters.DateTimeFormatState
import com.grippo.core.state.formatters.VolumeFormatState
import com.grippo.core.state.formatters.WeightFormatState
import com.grippo.core.state.trainings.ExerciseState
import com.grippo.core.state.trainings.IterationState
import com.grippo.data.features.api.training.GeneratePresetTrainingUseCase
import com.grippo.data.features.api.training.TrainingFeature
import com.grippo.data.features.api.training.models.PresetExercise
import com.grippo.data.features.api.training.models.PresetTraining
import com.grippo.data.features.api.training.models.Training
import com.grippo.domain.state.training.toState
import com.grippo.start.training.analytics.toAnalytics
import com.grippo.toolkit.date.utils.DateFormat
import com.grippo.toolkit.date.utils.DateRange
import com.grippo.toolkit.date.utils.DateRangePresets
import com.grippo.toolkit.date.utils.DateTimeUtils
import kotlin.uuid.Uuid
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toPersistentList
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach

public class StartTrainingViewModel(
    private val analytics: StartTrainingAnalytics,
    private val trainingFeature: TrainingFeature,
    private val generatePresetTrainingUseCase: GeneratePresetTrainingUseCase,
) : BaseViewModel<StartTrainingState, StartTrainingDirection, StartTrainingLoader>(
    StartTrainingState()
), StartTrainingContract {

    init {
        safeLaunch(loader = StartTrainingLoader.Options, recovery = ErrorRecovery.Retry) {
            val recentRange = state.value.recentRange

            trainingFeature.getTrainings(start = recentRange.from, end = recentRange.to)
        }

        observeOptions(range = state.value.recentRange)
            .onEach(::provideOptions)
            .safeLaunch(processing = Processing.Infinity, recovery = ErrorRecovery.Retry)
    }

    private fun observeOptions(
        range: DateRange,
    ): Flow<Pair<List<Training>, StartTrainingOption.Preset?>> = flow {
        val generated = analytics.trackOperation(StartTrainingOperation.BuildOptions) {
            generatePresetTrainingUseCase.execute()
        }

        val preset = toPresetOption(generated)

        val recents = trainingFeature
            .observeTrainings(start = range.from, end = range.to)
            .map { recents -> recents to preset }

        emitAll(recents)
    }

    private fun provideOptions(value: Pair<List<Training>, StartTrainingOption.Preset?>) {
        val (recents, preset) = value

        val recentOptions = recents
            .filter { it.exercises.isNotEmpty() }
            .sortedByDescending { it.createdAt }
            .take(MAX_RECENT_TRAININGS)
            .map(::toRecentOption)

        val options = buildList {
            preset?.let(::add)
            add(StartTrainingOption.Empty)
            addAll(recentOptions)
        }.toPersistentList()

        update { it.copy(options = options) }
    }

    private fun toPresetOption(value: PresetTraining?): StartTrainingOption.Preset? = value
        ?.exercises
        ?.map(::toPresetExercise)
        ?.toPersistentList()
        ?.takeIf { it.isNotEmpty() }
        ?.let { StartTrainingOption.Preset(exercises = it) }

    private fun toRecentOption(training: Training): StartTrainingOption.Recent {
        val mapped = training.toState()

        val presetExercises = mapped.exercises
            .map(::prepareRecentExercise)
            .toPersistentList()

        return StartTrainingOption.Recent(
            trainingId = mapped.id,
            createdAt = training.createdAt,
            exercises = presetExercises,
        )
    }

    private fun toPresetExercise(value: PresetExercise): ExerciseState {
        val iterations = value.iterations.map { iteration ->
            val id = Uuid.random().toString()
            iteration.toState(id = id)
        }.toPersistentList()

        val id = Uuid.random().toString()

        val createdAt = DateTimeUtils.now()

        return value.toState(id = id, iterations = iterations, createdAt = createdAt)
    }

    private fun prepareRecentExercise(value: ExerciseState): ExerciseState {
        val iterations = value.iterations
            .map(::prepareRecentIteration)
            .toPersistentList()

        return value.copy(iterations = iterations)
    }

    private fun prepareRecentIteration(value: IterationState): IterationState = value.copy(
        externalWeight = VolumeFormatState.Empty(),
        extraWeight = VolumeFormatState.Empty(),
        assistWeight = VolumeFormatState.Empty(),
        bodyWeight = WeightFormatState.Empty(),
    )

    private fun emitExercises(source: ImmutableList<ExerciseState>) {
        val now = DateTimeUtils.now()

        val range = DateRangePresets.infinity()

        val createdAt = DateTimeFormatState.of(
            value = now,
            range = range,
            format = DateFormat.DateOnly.DateDdMmmYyyy,
        )

        val cloned = source.map { cloneExercise(value = it, createdAt = createdAt) }.toPersistentList()

        navigateTo(StartTrainingDirection.UseExercises(exercises = cloned))
    }

    private fun cloneExercise(value: ExerciseState, createdAt: DateTimeFormatState): ExerciseState {
        val id = Uuid.random().toString()

        val iterations = value.iterations
            .map(::cloneIteration)
            .toPersistentList()

        return value.copy(id = id, iterations = iterations, createdAt = createdAt)
    }

    private fun cloneIteration(value: IterationState): IterationState {
        val id = Uuid.random().toString()

        return value.copy(id = id)
    }

    override fun onSelect(key: String) {
        val option = state.value.options.firstOrNull { it.key == key } ?: return

        val optionKind = option.toAnalytics()

        analytics.trackEvent(StartTrainingEvent.OptionConfirmed(option = optionKind))

        when (option) {
            StartTrainingOption.Empty -> {
                navigateTo(StartTrainingDirection.StartEmpty)
            }
            is StartTrainingOption.Preset -> {
                emitExercises(option.exercises)
            }
            is StartTrainingOption.Recent -> {
                emitExercises(option.exercises)
            }
        }
    }

    override fun onBack() {
        analytics.trackEvent(StartTrainingEvent.DismissRequested)
        navigateTo(StartTrainingDirection.Back)
    }

    private companion object {
        private const val MAX_RECENT_TRAININGS = 10
    }
}
