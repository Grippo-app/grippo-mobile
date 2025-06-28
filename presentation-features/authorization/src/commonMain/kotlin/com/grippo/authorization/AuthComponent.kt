package com.grippo.authorization

import androidx.compose.runtime.Composable
import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.router.stack.ChildStack
import com.arkivanov.decompose.router.stack.StackNavigation
import com.arkivanov.decompose.router.stack.childStack
import com.arkivanov.decompose.router.stack.push
import com.arkivanov.decompose.router.stack.replaceAll
import com.arkivanov.decompose.value.Value
import com.arkivanov.essenty.backhandler.BackCallback
import com.arkivanov.essenty.instancekeeper.retainedInstance
import com.grippo.authorization.AuthComponent.Child.AuthProcess
import com.grippo.authorization.AuthComponent.Child.Splash
import com.grippo.authorization.auth.process.AuthProcessComponent
import com.grippo.authorization.splash.SplashComponent
import com.grippo.core.BaseComponent
import com.grippo.core.collectAsStateMultiplatform
import com.grippo.presentation.api.auth.AuthRouter

public class AuthComponent(
    componentContext: ComponentContext,
    initial: AuthRouter,
    private val toHome: () -> Unit,
    private val back: () -> Unit,
) : BaseComponent<AuthDirection>(componentContext) {

    internal sealed class Child(open val component: BaseComponent<*>) {
        data class Splash(override val component: SplashComponent) : Child(component)
        data class AuthProcess(override val component: AuthProcessComponent) : Child(component)
    }

    override val viewModel: AuthViewModel = componentContext.retainedInstance {
        AuthViewModel()
    }

    private val backCallback = BackCallback(onBack = viewModel::back)

    init {
        backHandler.register(backCallback)
    }

    override suspend fun eventListener(direction: AuthDirection) {
        when (direction) {
            AuthDirection.AuthProcess -> navigation.push(AuthRouter.AuthProcess)
            AuthDirection.Back -> back.invoke()
        }
    }

    private val navigation = StackNavigation<AuthRouter>()

    internal val childStack: Value<ChildStack<AuthRouter, Child>> = childStack(
        source = navigation,
        serializer = AuthRouter.serializer(),
        initialStack = { listOf(initial) },
        key = "AuthComponent",
        handleBackButton = true,
        childFactory = ::createChild,
    )

    private fun createChild(router: AuthRouter, context: ComponentContext): Child {
        return when (router) {
            AuthRouter.Splash -> Splash(
                SplashComponent(
                    componentContext = context,
                    toAuthProcess = { navigation.replaceAll(AuthRouter.AuthProcess) },
                    toHome = toHome,
                    back = back
                ),
            )

            is AuthRouter.AuthProcess -> AuthProcess(
                AuthProcessComponent(
                    componentContext = context,
                    toHome = toHome,
                    back = back
                ),
            )
        }
    }

    @Composable
    override fun Render() {
        val state = viewModel.state.collectAsStateMultiplatform()
        val loaders = viewModel.loaders.collectAsStateMultiplatform()
        AuthScreen(this, state.value, loaders.value, viewModel)
    }
}
