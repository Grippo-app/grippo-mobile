package com.grippo.authorization.auth.process

import androidx.compose.runtime.Composable
import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.router.stack.ChildStack
import com.arkivanov.decompose.router.stack.StackNavigation
import com.arkivanov.decompose.router.stack.childStack
import com.arkivanov.decompose.router.stack.pop
import com.arkivanov.decompose.router.stack.push
import com.arkivanov.decompose.value.Value
import com.arkivanov.essenty.backhandler.BackCallback
import com.arkivanov.essenty.instancekeeper.retainedInstance
import com.grippo.authorization.login.LoginComponent
import com.grippo.authorization.registration.credential.CredentialComponent
import com.grippo.core.foundation.BaseComponent
import com.grippo.core.foundation.platform.collectAsStateMultiplatform
import com.grippo.screen.api.AuthProcessRouter

internal class AuthProcessComponent(
    componentContext: ComponentContext,
    private val toHome: () -> Unit,
    private val toProfileCreation: () -> Unit,
    private val close: () -> Unit
) : BaseComponent<AuthProcessDirection>(componentContext) {

    override val viewModel = componentContext.retainedInstance {
        AuthProcessViewModel()
    }

    private val backCallback = BackCallback(onBack = viewModel::onClose)

    init {
        backHandler.register(backCallback)
    }

    override suspend fun eventListener(direction: AuthProcessDirection) {
        when (direction) {
            AuthProcessDirection.Close -> {
                close.invoke()
            }

            is AuthProcessDirection.Registration -> {
                navigation.push(AuthProcessRouter.Registration(direction.email))
            }

            AuthProcessDirection.Home -> {
                toHome.invoke()
            }

            AuthProcessDirection.ProfileCreation -> {
                toProfileCreation.invoke()
            }

            AuthProcessDirection.Back -> {
                navigation.pop()
            }
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
                    toRegistration = viewModel::toRegistration,
                    toHome = viewModel::toHome,
                    toCreateProfile = viewModel::toProfileCreation,
                    back = viewModel::onClose
                )
            )

            is AuthProcessRouter.Registration -> Child.Registration(
                CredentialComponent(
                    componentContext = context,
                    email = router.email,
                    toCreateProfile = viewModel::toProfileCreation,
                    toHome = viewModel::toHome,
                    back = viewModel::onBack
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

    internal sealed class Child(open val component: BaseComponent<*>) {
        data class Login(override val component: LoginComponent) : Child(component)
        data class Registration(override val component: CredentialComponent) : Child(component)
    }
}
