package com.grippo.goal.setup.suggestion

import androidx.compose.runtime.Composable
import com.arkivanov.decompose.ComponentContext
import com.arkivanov.essenty.backhandler.BackCallback
import com.arkivanov.essenty.instancekeeper.retainedInstance
import com.grippo.core.foundation.BaseComponent
import com.grippo.core.foundation.platform.collectAsStateMultiplatform

public class GoalSetupSuggestionComponent(
    componentContext: ComponentContext,
    private val onConfigure: () -> Unit,
    private val onLater: () -> Unit,
    private val back: () -> Unit,
) : BaseComponent<GoalSetupSuggestionDirection>(componentContext) {

    override val viewModel: GoalSetupSuggestionViewModel = componentContext.retainedInstance {
        GoalSetupSuggestionViewModel()
    }

    private val backCallback = BackCallback(onBack = viewModel::onBack)

    init {
        backHandler.register(backCallback)
    }

    override suspend fun eventListener(direction: GoalSetupSuggestionDirection) {
        when (direction) {
            GoalSetupSuggestionDirection.Configure -> onConfigure.invoke()
            GoalSetupSuggestionDirection.Later -> onLater.invoke()
            GoalSetupSuggestionDirection.Back -> back.invoke()
        }
    }

    @Composable
    override fun Render() {
        val state = viewModel.state.collectAsStateMultiplatform()
        val loaders = viewModel.loaders.collectAsStateMultiplatform()
        GoalSetupSuggestionScreen(state.value, loaders.value, viewModel)
    }
}
