package com.grippo.start.training

import com.grippo.analytics.start.training.StartTrainingAnalytics
import com.grippo.analytics.start.training.models.StartTrainingEvent
import com.grippo.analytics.start.training.models.StartTrainingOperation
import com.grippo.core.foundation.BaseViewModel
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
import com.grippo.toolkit.date.utils.DateRangePresets
import com.grippo.toolkit.date.utils.DateTimeUtils
import kotlin.time.Duration.Companion.days
import kotlin.uuid.Uuid
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toPersistentList
import kotlinx.coroutines.flow.first

public class StartTrainingViewModel(
    private val analytics: StartTrainingAnalytics,
    trainingFeature: TrainingFeature,
    generatePresetTrainingUseCase: GeneratePresetTrainingUseCase,
) : BaseViewModel<StartTrainingState, StartTrainingDirection, StartTrainingLoader>(
    StartTrainingState()
), StartTrainingContract {

    init {
        safeLaunch(loader = StartTrainingLoader.Options) {
            val now = DateTimeUtils.now()
            val recentRangeStart = DateTimeUtils.minus(
                value = now,
                minus = RECENT_TRAININGS_WINDOW_DAYS.days,
            )

            trainingFeature.getTrainings(start = recentRangeStart, end = now).getOrThrow()

            analytics.trackOperation(StartTrainingOperation.BuildOptions) {
                val recents = trainingFeature.observeTrainings(start = recentRangeStart, end = now)
                    .first()

                val preset = generatePresetTrainingUseCase.execute()

                provideOptions(recents = recents, preset = preset)
            }
        }
    }

    private fun provideOptions(recents: List<Training>, preset: PresetTraining?) {
        val recentOptions = recents
            .filter { it.exercises.isNotEmpty() }
            .sortedByDescending { it.createdAt }
            .take(MAX_RECENT_TRAININGS)
            .map(::toRecentOption)

        val presetOption = preset?.exercises
            ?.map(::toPresetExercise)
            ?.toPersistentList()
            ?.takeIf { it.isNotEmpty() }
            ?.let { StartTrainingOption.Preset(exercises = it) }

        val options = buildList {
            presetOption?.let(::add)
            add(StartTrainingOption.Empty)
            addAll(recentOptions)
        }.toPersistentList()

        update { it.copy(options = options) }
    }

    private fun toRecentOption(training: Training): StartTrainingOption.Recent {
        val mapped = training.toState()

        val presetExercises = mapped.exercises
            .map { it.asPreset() }
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

    private fun ExerciseState.asPreset(): ExerciseState {
        val iterations = iterations
            .map { it.asPreset() }
            .toPersistentList()

        return copy(iterations = iterations)
    }

    private fun IterationState.asPreset(): IterationState = copy(
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
            format = DateFormat.DateOnly.DateMmmDdYyyy,
        )

        val cloned = source.map { it.cloned(createdAt = createdAt) }.toPersistentList()

        navigateTo(StartTrainingDirection.UseExercises(exercises = cloned))
    }

    private fun ExerciseState.cloned(createdAt: DateTimeFormatState): ExerciseState {
        val id = Uuid.random().toString()

        val iterations = iterations
            .map { it.cloned() }
            .toPersistentList()

        return copy(id = id, iterations = iterations, createdAt = createdAt)
    }

    private fun IterationState.cloned(): IterationState {
        val id = Uuid.random().toString()

        return copy(id = id)
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
        private const val RECENT_TRAININGS_WINDOW_DAYS = 14
        private const val MAX_RECENT_TRAININGS = 10
    }
}
