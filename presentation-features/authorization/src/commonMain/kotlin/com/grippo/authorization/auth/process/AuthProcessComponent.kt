package com.grippo.authorization.auth.process

import androidx.compose.runtime.Composable
import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.router.stack.ChildStack
import com.arkivanov.decompose.router.stack.StackNavigation
import com.arkivanov.decompose.router.stack.childStack
import com.arkivanov.decompose.router.stack.pop
import com.arkivanov.decompose.router.stack.push
import com.arkivanov.decompose.value.Value
import com.arkivanov.essenty.instancekeeper.retainedInstance
import com.grippo.authorization.login.LoginComponent
import com.grippo.authorization.registration.RegistrationComponent
import com.grippo.core.BaseComponent
import com.grippo.core.collectAsStateMultiplatform
import com.grippo.presentation.api.auth.AuthProcessRouter

internal class AuthProcessComponent(
    componentContext: ComponentContext,
    private val toHome: () -> Unit
) : BaseComponent<AuthProcessDirection>(componentContext) {

    internal sealed class Child(open val component: BaseComponent<*>) {
        data class Login(override val component: LoginComponent) : Child(component)
        data class Registration(override val component: RegistrationComponent) : Child(component)
    }

    override val viewModel = componentContext.retainedInstance {
        AuthProcessViewModel()
    }

    override suspend fun eventListener(direction: AuthProcessDirection) {
        when (direction) {
            AuthProcessDirection.Back -> navigation::pop
        }
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

    private fun createChild(router: AuthProcessRouter, context: ComponentContext): Child {
        return when (router) {
            AuthProcessRouter.Login -> Child.Login(
                LoginComponent(
                    componentContext = context,
                    toRegistration = { navigation.push(AuthProcessRouter.Registration) },
                    toHome = toHome
                )
            )

            AuthProcessRouter.Registration -> Child.Registration(
                RegistrationComponent(
                    componentContext = context,
                    toHome = toHome
                )
            )
        }
    }

    @Composable
    override fun Render() {
        val state = viewModel.state.collectAsStateMultiplatform()
        val loaders = viewModel.loaders.collectAsStateMultiplatform()
        AuthProcessScreen(this, state.value, loaders.value, viewModel)
    }
}