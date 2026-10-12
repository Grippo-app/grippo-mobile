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
import com.grippo.analytics.authorization.models.AuthTransition
import com.grippo.analytics.authorization.models.ExitReason
import com.grippo.authorization.analytics.AuthAnalyticsProvider
import com.grippo.authorization.login.LoginComponent
import com.grippo.authorization.registration.credential.CredentialComponent
import com.grippo.core.foundation.BaseComponent
import com.grippo.core.foundation.platform.collectAsStateMultiplatform
import com.grippo.screen.api.AuthProcessRouter
import com.grippo.screen.api.AuthRouter

internal class AuthProcessComponent(
    componentContext: ComponentContext,
    route: AuthRouter.AuthProcess,
    private val toHome: () -> Unit,
    private val toProfileCreation: () -> Unit,
    private val close: () -> Unit
) : BaseComponent<AuthProcessDirection>(componentContext) {

    private val analytics = getKoin().get<AuthAnalyticsProvider>()
        .provideAuthorization(componentContext, route)

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
                analytics.trackTransition(AuthTransition.Exit(ExitReason.UserBack))
                close.invoke()
            }

            is AuthProcessDirection.Registration -> {
                analytics.trackTransition(AuthTransition.OpenRegistration)
                navigation.push(AuthProcessRouter.Registration(direction.email))
            }

            AuthProcessDirection.Home -> {
                analytics.trackTransition(AuthTransition.Exit(ExitReason.Authenticated))
                toHome.invoke()
            }

            AuthProcessDirection.ProfileCreation -> {
                analytics.trackTransition(AuthTransition.Exit(ExitReason.ProfileRequired))
                toProfileCreation.invoke()
            }

            AuthProcessDirection.Back -> {
                analytics.trackTransition(AuthTransition.BackToLogin)
                navigation.pop()
            }
        }
    }

    private val navigation = StackNavigation<AuthProcessRouter>()

    internal val childStack: Value<ChildStack<AuthProcessRouter, Child>> = childStack(
        source = navigation,
        serializer = AuthProcessRouter.serializer(),
        initialConfiguration = AuthProcessRouter.Login,
        key = "AuthProcessComponent",
        handleBackButton = false,
        childFactory = ::createChild,
    )

    private fun createChild(router: AuthProcessRouter, context: ComponentContext): Child {
        return when (router) {
            AuthProcessRouter.Login -> Child.Login(
                LoginComponent(
                    componentContext = context,
                    analytics = analytics,
                    toRegistration = viewModel::toRegistration,
                    toHome = viewModel::toHome,
                    toCreateProfile = viewModel::toProfileCreation,
                    back = viewModel::onClose
                )
            )

            is AuthProcessRouter.Registration -> Child.Registration(
                CredentialComponent(
                    componentContext = context,
                    analytics = analytics,
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
