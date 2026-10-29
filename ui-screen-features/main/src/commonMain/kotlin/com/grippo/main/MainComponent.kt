package com.grippo.main

import androidx.compose.runtime.Composable
import com.arkivanov.decompose.ComponentContext
import com.arkivanov.essenty.backhandler.BackCallback
import com.arkivanov.essenty.instancekeeper.retainedInstance
import com.grippo.core.foundation.BaseComponent
import com.grippo.core.foundation.platform.collectAsStateMultiplatform
import com.grippo.core.state.stage.StageState
import com.grippo.screen.api.MainRouter
import com.grippo.screen.api.TrainingsRouter

public class MainComponent(
    componentContext: ComponentContext,
    private val createHome: (
        context: ComponentContext,
        toTrainings: (TrainingsRouter) -> Unit,
        onBack: () -> Unit,
    ) -> BaseComponent<*>,
    private val createCalendar: (
        context: ComponentContext,
        initial: TrainingsRouter,
        onBack: () -> Unit,
    ) -> BaseComponent<*>,
    private val createLibrary: (context: ComponentContext, onBack: () -> Unit) -> BaseComponent<*>,
    private val createProfile: (context: ComponentContext, onBack: () -> Unit) -> BaseComponent<*>,
    private val toTraining: (StageState) -> Unit,
    private val toGoal: () -> Unit,
    private val close: () -> Unit,
) : BaseComponent<MainDirection>(componentContext) {
    override val viewModel: MainViewModel = componentContext.retainedInstance {
        MainViewModel(
            analytics = getKoin().get(),
            trainingFeature = getKoin().get(),
            goalSetupSuggestionUseCase = getKoin().get(),
            dialogController = getKoin().get(),
        )
    }
    private val backCallback = BackCallback(onBack = viewModel::onBack)

    init {
        backHandler.register(backCallback)
    }

    private val navigation = MainTabNavigation(componentContext, ::createChild)
    internal val childStack get() = navigation.childStack

    override suspend fun eventListener(direction: MainDirection) {
        when (direction) {
            is MainDirection.SelectTab -> navigation.select(direction.tab)
            is MainDirection.Trainings -> navigation.open(MainRouter.Calendar(value = direction.route))
            is MainDirection.Training -> toTraining(direction.stage)
            MainDirection.Goal -> toGoal()
            MainDirection.Back -> if (!navigation.back()) close()
        }
    }

    private fun createChild(tab: MainRouter, context: ComponentContext): BaseComponent<*> = when (tab) {
        MainRouter.Home -> createHome(context, viewModel::toTrainings, viewModel::onBack)
        is MainRouter.Calendar -> createCalendar(context, tab.value, viewModel::onBack)
        MainRouter.Library -> createLibrary(context, viewModel::onBack)
        MainRouter.Profile -> createProfile(context, viewModel::onBack)
    }

    @Composable
    override fun Render() {
        val state = viewModel.state.collectAsStateMultiplatform()
        val loaders = viewModel.loaders.collectAsStateMultiplatform()
        MainScreen(this, state.value, loaders.value, viewModel)
    }
}
