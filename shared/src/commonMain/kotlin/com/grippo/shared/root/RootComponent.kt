package com.grippo.shared.root

import androidx.compose.runtime.Composable
import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.router.stack.ChildStack
import com.arkivanov.decompose.router.stack.StackNavigation
import com.arkivanov.decompose.router.stack.childStack
import com.arkivanov.decompose.value.Value
import com.arkivanov.essenty.instancekeeper.retainedInstance
import com.grippo.authorization.AuthComponent
import com.grippo.core.BaseComponent
import com.grippo.design.core.AppTheme
import com.grippo.presentation.api.RootRouter

public class RootComponent(
    componentContext: ComponentContext,
) : BaseComponent<RootDirection>(componentContext) {

    internal sealed class Child(open val component: BaseComponent<*>) {
        data class Authorization(override val component: AuthComponent) : Child(component)
    }

    override val viewModel: RootViewModel = componentContext.retainedInstance {
        RootViewModel()
    }

    override suspend fun eventListener(rout: RootDirection) {

    }

    private val navigation = StackNavigation<RootRouter>()

    internal val childStack: Value<ChildStack<RootRouter, Child>> = childStack(
        source = navigation,
        serializer = RootRouter.serializer(),
        initialConfiguration = RootRouter.Auth,
        handleBackButton = true,
        key = "RootComponent",
        childFactory = ::createChild,
    )

    private fun createChild(
        router: RootRouter,
        context: ComponentContext
    ): Child {
        return when (router) {
            RootRouter.Auth -> Child.Authorization(
                AuthComponent(
                    componentContext = context,
                ),
            )
        }
    }

    @Composable
    override fun Render() {
        AppTheme { RootScreen(this) }
    }
}