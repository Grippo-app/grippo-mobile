package com.grippo.main

import com.grippo.analytics.main.MainAnalytics
import com.grippo.analytics.main.models.MainEvent
import com.grippo.core.foundation.BaseViewModel
import com.grippo.core.foundation.models.ErrorRecovery
import com.grippo.core.state.formatters.UiText
import com.grippo.core.state.stage.StageState
import com.grippo.data.features.api.goal.GoalSetupSuggestionUseCase
import com.grippo.data.features.api.training.TrainingFeature
import com.grippo.design.resources.provider.Res
import com.grippo.design.resources.provider.dialog_saved_workout
import com.grippo.design.resources.provider.goal_setup_suggestion_title
import com.grippo.dialog.api.DialogConfig
import com.grippo.dialog.api.DialogController
import com.grippo.main.analytics.toAnalytics
import com.grippo.screen.api.MainRouter
import kotlinx.coroutines.flow.first

public class MainViewModel(
    private val analytics: MainAnalytics,
    private val trainingFeature: TrainingFeature,
    private val goalSetupSuggestionUseCase: GoalSetupSuggestionUseCase,
    private val dialogController: DialogController,
) : BaseViewModel<MainState, MainDirection, MainLoader>(MainState), MainContract {

    override fun onSelectTab(tab: MainRouter) {
        analytics.trackEvent(MainEvent.TabSelected(tab.toAnalytics()))
        navigateTo(MainDirection.SelectTab(tab))
    }

    override fun onStartTraining() {
        analytics.trackEvent(MainEvent.StartTrainingClicked)
        resumeOrStartTraining()
    }

    private fun resumeOrStartTraining() {
        safeLaunch(loader = MainLoader.StartTraining, recovery = ErrorRecovery.Retry) {
            if (trainingFeature.getDraftTraining().first() != null) {
                dialogController.open(
                    DialogConfig.DraftTraining(
                        title = UiText.Res(Res.string.dialog_saved_workout),
                        onContinue = { navigateTo(MainDirection.Training(StageState.Draft)) },
                        onStartNew = ::resumeOrStartTraining,
                    )
                )
            } else {
                startNewTraining()
            }
        }
    }

    private suspend fun startNewTraining() {
        if (goalSetupSuggestionUseCase.shouldSuggest()) {
            goalSetupSuggestionUseCase.markShown()
            dialogController.open(
                DialogConfig.GoalSetupSuggestion(
                    title = UiText.Res(Res.string.goal_setup_suggestion_title),
                    onConfigure = { navigateTo(MainDirection.Goal) },
                    onLater = { navigateTo(MainDirection.Training(StageState.Add)) },
                )
            )
        } else {
            navigateTo(MainDirection.Training(StageState.Add))
        }
    }

    override fun onBack() {
        navigateTo(MainDirection.Back)
    }
}
