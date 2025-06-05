package com.grippo.profile

import androidx.compose.runtime.Composable
import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.router.stack.ChildStack
import com.arkivanov.decompose.router.stack.StackNavigation
import com.arkivanov.decompose.router.stack.childStack
import com.arkivanov.decompose.router.stack.pop
import com.arkivanov.decompose.value.Value
import com.arkivanov.essenty.instancekeeper.retainedInstance
import com.grippo.core.BaseComponent
import com.grippo.core.collectAsStateMultiplatform
import com.grippo.presentation.api.profile.ProfileRouter
import com.grippo.profile.equipment.ProfileEquipmentComponent
import com.grippo.profile.muscles.ProfileMusclesComponent

public class ProfileComponent(
    initial: ProfileRouter,
    componentContext: ComponentContext,
) : BaseComponent<ProfileDirection>(componentContext) {

    internal sealed class Child(open val component: BaseComponent<*>) {
        data class Muscles(override val component: ProfileMusclesComponent) : Child(component)
        data class Equipments(override val component: ProfileEquipmentComponent) : Child(component)
    }

    override val viewModel: ProfileViewModel = componentContext.retainedInstance {
        ProfileViewModel()
    }

    override suspend fun eventListener(direction: ProfileDirection) {
    }

    private val navigation = StackNavigation<ProfileRouter>()

    internal val childStack: Value<ChildStack<ProfileRouter, Child>> = childStack(
        source = navigation,
        serializer = ProfileRouter.serializer(),
        initialStack = { listOf(initial) },
        key = "AuthComponent",
        handleBackButton = true,
        childFactory = ::createChild,
    )

    private fun createChild(router: ProfileRouter, context: ComponentContext): Child {
        return when (router) {
            ProfileRouter.Muscles -> Child.Muscles(
                ProfileMusclesComponent(
                    componentContext = context,
                    onBack = navigation::pop
                ),
            )

            is ProfileRouter.Equipments -> Child.Equipments(
                ProfileEquipmentComponent(
                    componentContext = context,
                    onBack = navigation::pop
                ),
            )
        }
    }

    @Composable
    override fun Render() {
        val state = viewModel.state.collectAsStateMultiplatform()
        val loaders = viewModel.loaders.collectAsStateMultiplatform()
        ProfileScreen(childStack, state.value, loaders.value, viewModel)
    }
}