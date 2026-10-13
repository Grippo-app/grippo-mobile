package com.grippo.draft.training

import com.grippo.analytics.draft.training.DraftTrainingAnalytics
import com.grippo.analytics.draft.training.models.DraftTrainingEvent
import com.grippo.analytics.draft.training.models.DraftTrainingOperation
import com.grippo.core.foundation.BaseViewModel
import com.grippo.core.state.stage.StageState
import com.grippo.data.features.api.training.TrainingFeature
import com.grippo.data.features.api.training.models.DraftTraining
import com.grippo.domain.state.training.toState
import kotlin.uuid.Uuid
import kotlinx.collections.immutable.toPersistentList
import kotlinx.coroutines.flow.onEach

public class DraftTrainingViewModel(
    private val analytics: DraftTrainingAnalytics,
    private val trainingFeature: TrainingFeature
) : BaseViewModel<DraftTrainingState, DraftTrainingDirection, DraftTrainingLoader>(
    DraftTrainingState()
), DraftTrainingContract {

    init {
        trainingFeature.getDraftTraining()
            .onEach(::provideDraftTraining)
            .safeLaunch()
    }

    private fun provideDraftTraining(value: DraftTraining?) {
        value ?: return
        update {
            it.copy(
                exercises = value.exercises.map { exercise ->
                    exercise.toState(
                        id = Uuid.random().toString(),
                        iterations = exercise.iterations.map { iteration ->
                            iteration.toState(id = Uuid.random().toString())
                        }.toPersistentList(),
                    )
                }.toPersistentList(),
                stage = when (val trainingId = value.trainingId) {
                    null -> StageState.Add
                    else -> StageState.Edit(trainingId)
                }
            )
        }
    }

    override fun onContinue() {
        analytics.trackEvent(DraftTrainingEvent.ContinueClicked)
        navigateTo(DraftTrainingDirection.Continue)
    }

    override fun onStartNew() {
        analytics.trackEvent(DraftTrainingEvent.StartNewClicked)
        safeLaunch {
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
