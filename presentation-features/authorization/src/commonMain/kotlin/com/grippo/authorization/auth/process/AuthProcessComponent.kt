package com.grippo.authorization.auth.process

import androidx.compose.runtime.Composable
import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.router.stack.ChildStack
import com.arkivanov.decompose.router.stack.StackNavigation
import com.arkivanov.decompose.router.stack.childStack
import com.arkivanov.decompose.value.Value
import com.arkivanov.essenty.instancekeeper.retainedInstance
import com.grippo.authorization.login.LoginComponent
import com.grippo.core.BaseComponent
import com.grippo.presentation.api.AuthProcessRouter

internal class AuthProcessComponent(
    componentContext: ComponentContext,
) : BaseComponent<AuthProcessDirection>(componentContext) {

    internal sealed class Child(open val component: BaseComponent<*>) {
        data class Login(override val component: LoginComponent) : Child(component)
    }

    override val viewModel = componentContext.retainedInstance {
        AuthProcessViewModel()
    }

    override suspend fun eventListener(rout: AuthProcessDirection) {
    }

    private val navigation = StackNavigation<AuthProcessRouter>()

    internal val childStack: Value<ChildStack<AuthProcessRouter, Child>> = childStack(
        source = navigation,
        serializer = AuthProcessRouter.serializer(),
        initialStack = { listOf(AuthProcessRouter.Login) },
        key = "AuthProcessComponent",
        handleBackButton = true,
        childFactory = ::createChild,
    )

    private fun createChild(
        router: AuthProcessRouter,
        context: ComponentContext
    ): Child {
        return when (router) {
            AuthProcessRouter.Login -> Child.Login(
                LoginComponent(
                    componentContext = context
                )
            )

            AuthProcessRouter.Registration -> TODO()
        }
    }

    @Composable
    override fun Render() {
        AuthProcessScreen(this)
    }
}