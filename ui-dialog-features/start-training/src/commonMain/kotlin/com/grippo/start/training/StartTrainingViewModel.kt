package com.grippo.start.training

import com.grippo.core.foundation.BaseViewModel
import com.grippo.core.state.formatters.VolumeFormatState
import com.grippo.core.state.formatters.WeightFormatState
import com.grippo.core.state.trainings.ExerciseState
import com.grippo.core.state.trainings.IterationState
import com.grippo.data.features.api.training.GeneratePresetTrainingUseCase
import com.grippo.data.features.api.training.TrainingFeature
import com.grippo.data.features.api.training.models.Training
import com.grippo.domain.state.training.toState
import com.grippo.toolkit.date.utils.DateTimeUtils
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toPersistentList
import kotlinx.coroutines.flow.firstOrNull
import kotlin.time.Duration.Companion.days
import kotlin.uuid.Uuid

public class StartTrainingViewModel(
    private val trainingFeature: TrainingFeature,
    private val generatePresetTrainingUseCase: GeneratePresetTrainingUseCase,
) : BaseViewModel<StartTrainingState, StartTrainingDirection, StartTrainingLoader>(
    StartTrainingState()
), StartTrainingContract {

    init {
        safeLaunch {
            val now = DateTimeUtils.now()
            val recentRangeStart = DateTimeUtils.minus(now, RECENT_TRAININGS_WINDOW_DAYS.days)

            trainingFeature.getTrainings(start = recentRangeStart, end = now)

            val recents = trainingFeature
                .observeTrainings(start = recentRangeStart, end = now)
                .firstOrNull()
                .orEmpty()
                .filter { it.exercises.isNotEmpty() }
                .sortedByDescending { it.createdAt }
                .take(MAX_RECENT_TRAININGS)
                .map(::toRecentOption)

            val preset = generatePresetTrainingUseCase
                .execute()
                ?.toState()
                ?.takeIf { it.isNotEmpty() }
                ?.let(StartTrainingOption::Preset)

            val options = buildList {
                add(StartTrainingOption.Empty)
                preset?.let(::add)
                addAll(recents)
            }.toPersistentList()

            update { it.copy(options = options) }
        }
    }

    private fun toRecentOption(training: Training): StartTrainingOption.Recent {
        val state = training.toState()
        val presetExercises = state.exercises
            .map { it.asPreset() }
            .toPersistentList()
        return StartTrainingOption.Recent(
            trainingId = state.id,
            createdAt = state.createdAt,
            exercises = presetExercises,
        )
    }

    private fun ExerciseState.asPreset(): ExerciseState = copy(
        iterations = iterations
            .map { it.asPreset() }
            .toPersistentList(),
    )

    private fun IterationState.asPreset(): IterationState = copy(
        externalWeight = VolumeFormatState.Empty(),
        extraWeight = VolumeFormatState.Empty(),
        assistWeight = VolumeFormatState.Empty(),
        bodyWeight = WeightFormatState.Empty(),
    )

    override fun onSelect(key: String) {
        val option = state.value.options.firstOrNull { it.key == key } ?: return

        when (option) {
            StartTrainingOption.Empty -> navigateTo(StartTrainingDirection.StartEmpty)
            is StartTrainingOption.Preset -> emitExercises(option.exercises)
            is StartTrainingOption.Recent -> emitExercises(option.exercises)
        }
    }

    override fun onBack() {
        navigateTo(StartTrainingDirection.Back)
    }

    private fun emitExercises(source: ImmutableList<ExerciseState>) {
        val cloned = source.map { it.cloned() }.toPersistentList()
        navigateTo(StartTrainingDirection.UseExercises(cloned))
    }

    private fun ExerciseState.cloned(): ExerciseState = copy(
        id = Uuid.random().toString(),
        iterations = iterations
            .map { it.cloned() }
            .toPersistentList(),
    )

    private fun IterationState.cloned(): IterationState = copy(
        id = Uuid.random().toString(),
    )

    private companion object {
        private const val RECENT_TRAININGS_WINDOW_DAYS = 14
        private const val MAX_RECENT_TRAININGS = 10
    }
}
