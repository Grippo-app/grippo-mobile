package com.grippo.home.home

import com.grippo.core.foundation.BaseViewModel
import com.grippo.core.state.profile.ProfileMenu
import com.grippo.core.state.profile.SettingsMenu
import com.grippo.data.features.api.exercise.example.ExerciseExampleFeature
import com.grippo.data.features.api.exercise.example.models.ExerciseExample
import com.grippo.data.features.api.training.TrainingFeature
import com.grippo.data.features.api.training.models.Training
import com.grippo.dialog.api.DialogConfig
import com.grippo.dialog.api.DialogController
import com.grippo.domain.state.exercise.example.toState
import com.grippo.domain.state.training.toState
import com.grippo.domain.state.training.transformation.toHighlight
import com.grippo.domain.state.training.transformation.toMonthlyDigestState
import com.grippo.domain.state.training.transformation.toWeeklyDigestState
import com.grippo.toolkit.date.utils.DateRange
import com.grippo.toolkit.date.utils.DateTimeUtils
import kotlinx.coroutines.flow.distinctUntilChangedBy
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach

internal class HomeViewModel(
    private val trainingFeature: TrainingFeature,
    private val dialogController: DialogController,
    private val exerciseExampleFeature: ExerciseExampleFeature,
) : BaseViewModel<HomeState, HomeDirection, HomeLoader>(
    HomeState()
), HomeContract {

    private val monthlyRange: DateRange = DateTimeUtils.trailingMonth()

    init {
        trainingFeature
            .observeLastTraining()
            .onEach(::provideLastTraining)
            .safeLaunch()

        trainingFeature
            .observeTrainings(start = monthlyRange.from, end = monthlyRange.to)
            .onEach(::provideDigests)
            .safeLaunch()

        safeLaunch {
            trainingFeature.getTrainings(
                start = monthlyRange.from,
                end = monthlyRange.to
            ).getOrThrow()
        }

        state
            .map { it.highlightContext }
            .distinctUntilChangedBy { context ->
                context?.let { ctx -> ctx.exampleIds to ctx.trainings }
            }
            .flatMapLatest { context ->
                when {
                    context == null -> flowOf<HighlightPayload?>(null)
                    context.exampleIds.isEmpty() -> flowOf(HighlightPayload(context, emptyList()))
                    else -> exerciseExampleFeature
                        .observeExerciseExamples(context.exampleIds.toList())
                        .map { examples -> HighlightPayload(context, examples) }
                }
            }
            .onEach { payload ->
                val data = payload ?: return@onEach
                provideHighlightExamples(data.context, data.examples)
            }
            .safeLaunch()
    }

    private fun provideLastTraining(value: Training?) {
        val training = value?.toState() ?: return
        update { it.copy(lastTraining = training) }
    }

    private fun provideDigests(trainings: List<Training>) {
        if (trainings.isEmpty()) {
            update {
                it.copy(
                    weeklyDigestState = null,
                    monthlyDigestState = null,
                    highlight = null,
                    highlightContext = null
                )
            }
            return
        }

        val trainingsState = trainings.toState()
        val weekly = trainingsState.toWeeklyDigestState(range = DateTimeUtils.trailingWeek())
        val monthly = trainingsState.toMonthlyDigestState(range = monthlyRange)
        val highlight = trainingsState.toHighlight(exerciseExamples = emptyList())
        val ids = trainings
            .flatMap { it.exercises }
            .map { it.exerciseExample.id }
            .toSet()

        update {
            it.copy(
                weeklyDigestState = weekly,
                monthlyDigestState = monthly,
                highlight = highlight,
                highlightContext = HighlightContext(
                    trainings = trainingsState,
                    exampleIds = ids,
                    examples = emptyList()
                )
            )
        }
    }

    override fun onStartTraining() {
        navigateTo(HomeDirection.AddTraining)
    }

    override fun onOpenProfile() {
        val dialog = DialogConfig.Profile(
            onProfileResult = {
                when (it) {
                    ProfileMenu.Muscles -> navigateTo(HomeDirection.ExcludedMuscles)
                    ProfileMenu.Equipment -> navigateTo(HomeDirection.MissingEquipment)
                }
            },
            onSettingsResult = {
                when (it) {
                    SettingsMenu.Debug -> navigateTo(HomeDirection.Debug)
                }
            }
        )

        dialogController.show(dialog)
    }

    override fun onOpenTrainings() {
        navigateTo(HomeDirection.Trainings)
    }

    override fun onOpenExample() {
        val exampleId = state.value.highlight?.focusExercise?.exampleId ?: return

        val dialog = DialogConfig.ExerciseExample(
            id = exampleId
        )

        dialogController.show(dialog)
    }

    override fun onBack() {
        navigateTo(HomeDirection.Back)
    }

    private fun provideHighlightExamples(
        context: HighlightContext,
        list: List<ExerciseExample>,
    ) {
        val examples = list.toState()
        val highlight = context.trainings.toHighlight(exerciseExamples = examples)

        update {
            it.copy(
                highlight = highlight,
                highlightContext = context.copy(examples = examples)
            )
        }
    }

    private data class HighlightPayload(
        val context: HighlightContext,
        val examples: List<ExerciseExample>,
    )
}
