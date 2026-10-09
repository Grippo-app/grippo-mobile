package com.grippo.main

import androidx.compose.runtime.Composable
import com.arkivanov.decompose.ComponentContext
import com.arkivanov.essenty.backhandler.BackCallback
import com.arkivanov.essenty.instancekeeper.retainedInstance
import com.grippo.core.foundation.BaseComponent
import com.grippo.core.foundation.platform.collectAsStateMultiplatform
import com.grippo.screen.api.MainRouter

public class MainComponent(
    componentContext: ComponentContext,
    private val createHome: (context: ComponentContext, onBack: () -> Unit) -> BaseComponent<*>,
    private val createCalendar: (context: ComponentContext, onBack: () -> Unit) -> BaseComponent<*>,
    private val createProfile: (context: ComponentContext, onBack: () -> Unit) -> BaseComponent<*>,
    private val close: () -> Unit,
) : BaseComponent<MainDirection>(componentContext) {
    override val viewModel: MainViewModel = componentContext.retainedInstance { MainViewModel() }
    private val backCallback = BackCallback(onBack = viewModel::onBack)

    init {
        backHandler.register(backCallback)
    }

    private val navigation = MainTabNavigation(componentContext, ::createChild)
    internal val childStack get() = navigation.childStack

    override suspend fun eventListener(direction: MainDirection) {
        when (direction) {
            is MainDirection.SelectTab -> navigation.select(direction.tab)
            MainDirection.Back -> if (!navigation.back()) close()
        }
    }

    private fun createChild(tab: MainRouter, context: ComponentContext): BaseComponent<*> = when (tab) {
        MainRouter.Home -> createHome(context, viewModel::onBack)
        MainRouter.Calendar -> createCalendar(context, viewModel::onBack)
        MainRouter.Profile -> createProfile(context, viewModel::onBack)
    }

    @Composable
    override fun Render() {
        val state = viewModel.state.collectAsStateMultiplatform()
        val loaders = viewModel.loaders.collectAsStateMultiplatform()
        MainScreen(this, state.value, loaders.value, viewModel)
    }
}
