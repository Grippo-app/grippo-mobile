package com.grippo.authorization

import androidx.compose.runtime.Composable
import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.router.stack.ChildStack
import com.arkivanov.decompose.router.stack.StackNavigation
import com.arkivanov.decompose.router.stack.childStack
import com.arkivanov.decompose.value.Value
import com.arkivanov.essenty.instancekeeper.retainedInstance
import com.grippo.authorization.splash.SplashComponent
import com.grippo.core.BaseComponent
import com.grippo.presentation.api.AuthRouter

internal class AuthComponent(
    componentContext: ComponentContext,
) : BaseComponent<AuthDirection>(componentContext) {

    internal sealed class AuthChild {
        data class Splash(val component: SplashComponent) : AuthChild()
    }

    override val viewModel = componentContext.retainedInstance {
        AuthViewModel()
    }

    override suspend fun eventListener(rout: AuthDirection) {

    }

    private val navigation = StackNavigation<AuthRouter>()

    internal val childStack: Value<ChildStack<AuthRouter, AuthChild>> = childStack(
        source = navigation,
        serializer = AuthRouter.serializer(),
        initialStack = { listOf(AuthRouter.Splash) },
        key = "AuthComponent",
        handleBackButton = true,
        childFactory = ::createChild,
    )

    private fun createChild(router: AuthRouter, context: ComponentContext): AuthChild {
        return when (router) {
            AuthRouter.Splash -> AuthChild.Splash(
                SplashComponent(
                    componentContext = context,
                ),
            )

            is AuthRouter.AuthProcess -> {
                TODO()
            }
        }
    }

    @Composable
    override fun Render() {
    }
}
