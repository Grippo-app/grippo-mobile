package com.grippo.main

import com.grippo.core.foundation.BaseViewModel
import com.grippo.core.state.formatters.UiText
import com.grippo.core.state.stage.StageState
import com.grippo.data.features.api.goal.GoalSetupSuggestionUseCase
import com.grippo.data.features.api.training.TrainingFeature
import com.grippo.design.resources.provider.Res
import com.grippo.design.resources.provider.dialog_saved_workout
import com.grippo.design.resources.provider.goal_setup_suggestion_title
import com.grippo.dialog.api.DialogConfig
import com.grippo.dialog.api.DialogController
import com.grippo.screen.api.MainRouter
import kotlinx.coroutines.flow.first

public class MainViewModel(
    private val trainingFeature: TrainingFeature,
    private val goalSetupSuggestionUseCase: GoalSetupSuggestionUseCase,
    private val dialogController: DialogController,
) : BaseViewModel<MainState, MainDirection, MainLoader>(MainState), MainContract {

    override fun onSelectTab(tab: MainRouter) {
        navigateTo(MainDirection.SelectTab(tab))
    }

    override fun onStartTraining() {
        safeLaunch(loader = MainLoader.StartTraining) {
            if (trainingFeature.getDraftTraining().first() != null) {
                dialogController.open(
                    DialogConfig.DraftTraining(
                        title = UiText.Res(Res.string.dialog_saved_workout),
                        onContinue = { navigateTo(MainDirection.Training(StageState.Draft)) },
                        onStartNew = ::onStartTraining,
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
