package com.grippo.authorization.registration

import androidx.compose.runtime.Composable
import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.router.stack.ChildStack
import com.arkivanov.decompose.router.stack.StackNavigation
import com.arkivanov.decompose.router.stack.childStack
import com.arkivanov.decompose.value.Value
import com.arkivanov.essenty.instancekeeper.retainedInstance
import com.grippo.authorization.splash.SplashComponent
import com.grippo.core.BaseComponent
import com.grippo.core.collectAsStateMultiplatform
import com.grippo.presentation.api.auth.RegistrationRouter

internal class RegistrationComponent(
    componentContext: ComponentContext,
) : BaseComponent<RegistrationDirection>(componentContext) {

    internal sealed class Child(open val component: BaseComponent<*>) {
        data class Credential(override val component: SplashComponent) : Child(component)
    }

    override val viewModel = componentContext.retainedInstance {
        RegistrationViewModel()
    }

    override suspend fun eventListener(rout: RegistrationDirection) {
    }

    private val navigation = StackNavigation<RegistrationRouter>()

    internal val childStack: Value<ChildStack<RegistrationRouter, Child>> = childStack(
        source = navigation,
        serializer = RegistrationRouter.serializer(),
        initialStack = { listOf(RegistrationRouter.Credentials) },
        key = "RegistrationComponent",
        handleBackButton = true,
        childFactory = ::createChild,
    )

    private fun createChild(router: RegistrationRouter, context: ComponentContext): Child {
        return when (router) {
            RegistrationRouter.Body -> TODO()
            RegistrationRouter.Completed -> TODO()
            RegistrationRouter.Credentials -> TODO()
            RegistrationRouter.ExcludedMuscles -> TODO()
            RegistrationRouter.Experience -> TODO()
            RegistrationRouter.MissingEquipment -> TODO()
            RegistrationRouter.Name -> TODO()
        }
    }

    @Composable
    override fun Render() {
        val state = viewModel.state.collectAsStateMultiplatform()
        val loaders = viewModel.loaders.collectAsStateMultiplatform()
        RegistrationScreen(state.value, loaders.value, viewModel)
    }
}