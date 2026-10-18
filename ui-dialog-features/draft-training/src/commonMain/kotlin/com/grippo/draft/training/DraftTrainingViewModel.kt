package com.grippo.draft.training

import com.grippo.analytics.draft.training.DraftTrainingAnalytics
import com.grippo.analytics.draft.training.models.DraftTrainingEvent
import com.grippo.analytics.draft.training.models.DraftTrainingOperation
import com.grippo.core.foundation.BaseViewModel
import com.grippo.core.state.formatters.DurationFormatState
import com.grippo.core.state.stage.StageState
import com.grippo.data.features.api.training.TrainingFeature
import com.grippo.data.features.api.training.models.DraftTraining
import com.grippo.domain.state.training.toState
import kotlin.uuid.Uuid
import kotlinx.collections.immutable.toPersistentList
import kotlinx.coroutines.flow.first

public class DraftTrainingViewModel(
    private val analytics: DraftTrainingAnalytics,
    private val trainingFeature: TrainingFeature
) : BaseViewModel<DraftTrainingState, DraftTrainingDirection, DraftTrainingLoader>(
    DraftTrainingState()
), DraftTrainingContract {

    init {
        safeLaunch(loader = DraftTrainingLoader.Draft) {
            val draft = trainingFeature.getDraftTraining().first()
            provideDraftTraining(draft)
        }
    }

    private fun provideDraftTraining(value: DraftTraining?) {
        if (value == null) {
            navigateTo(DraftTrainingDirection.Back)
            return
        }

        val exercises = value.exercises.map { exercise ->
            val iterations = exercise.iterations.map { iteration ->
                iteration.toState(id = Uuid.random().toString())
            }.toPersistentList()

            exercise.toState(
                id = Uuid.random().toString(),
                iterations = iterations,
            )
        }.toPersistentList()

        val duration = DurationFormatState.of(value.duration)

        val stage = when (val trainingId = value.trainingId) {
            null -> StageState.Add
            else -> StageState.Edit(trainingId)
        }

        update { it.copy(exercises = exercises, duration = duration, stage = stage) }
    }

    override fun onContinue() {
        if (loaders.value.isNotEmpty()) {
            return
        }

        analytics.trackEvent(DraftTrainingEvent.ContinueClicked)
        navigateTo(DraftTrainingDirection.Continue)
    }

    override fun onStartNew() {
        if (loaders.value.isNotEmpty()) {
            return
        }

        analytics.trackEvent(DraftTrainingEvent.StartNewClicked)
        safeLaunch(loader = DraftTrainingLoader.DeleteDraft) {
            analytics.trackOperation(DraftTrainingOperation.DeleteDraft) {
                trainingFeature.deleteDraftTraining().getOrThrow()
            }
            navigateTo(DraftTrainingDirection.StartNew)
        }
    }

    override fun onBack() {
        analytics.trackEvent(DraftTrainingEvent.DismissRequested)
        navigateTo(DraftTrainingDirection.Back)
    }
}
