package com.grippo.settings

import androidx.compose.runtime.Composable
import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.router.stack.ChildStack
import com.arkivanov.decompose.router.stack.StackNavigation
import com.arkivanov.decompose.router.stack.childStack
import com.arkivanov.decompose.value.Value
import com.arkivanov.essenty.backhandler.BackCallback
import com.arkivanov.essenty.instancekeeper.retainedInstance
import com.grippo.core.BaseComponent
import com.grippo.core.collectAsStateMultiplatform
import com.grippo.presentation.api.settings.SettingsRouter
import com.grippo.settings.system.SystemComponent

public class SettingsComponent(
    initial: SettingsRouter,
    componentContext: ComponentContext,
    private val back: () -> Unit
) : BaseComponent<SettingsDirection>(componentContext) {

    internal sealed class Child(open val component: BaseComponent<*>) {
        data class System(override val component: SystemComponent) : Child(component)
    }

    override val viewModel: SettingsViewModel = componentContext.retainedInstance {
        SettingsViewModel()
    }

    private val backCallback = BackCallback(onBack = viewModel::back)

    init {
        backHandler.register(backCallback)
    }

    override suspend fun eventListener(direction: SettingsDirection) {
        when (direction) {
            SettingsDirection.Back -> back.invoke()
        }
    }

    private val navigation = StackNavigation<SettingsRouter>()

    internal val childStack: Value<ChildStack<SettingsRouter, Child>> =
        childStack(
            source = navigation,
            serializer = SettingsRouter.serializer(),
            initialStack = { listOf(initial) },
            key = "AuthComponent",
            handleBackButton = true,
            childFactory = ::createChild,
        )

    private fun createChild(
        router: SettingsRouter,
        context: ComponentContext
    ): Child {
        return when (router) {
            SettingsRouter.System -> Child.System(
                SystemComponent(
                    componentContext = context,
                    back = back
                ),
            )
        }
    }

    @Composable
    override fun Render() {
        val state = viewModel.state.collectAsStateMultiplatform()
        val loaders = viewModel.loaders.collectAsStateMultiplatform()
        SettingsScreen(this, state.value, loaders.value, viewModel)
    }
}