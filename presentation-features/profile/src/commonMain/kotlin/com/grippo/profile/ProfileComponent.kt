package com.grippo.profile

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
import com.grippo.presentation.api.profile.ProfileRouter
import com.grippo.profile.ProfileComponent.Child.Equipments
import com.grippo.profile.ProfileComponent.Child.Muscles
import com.grippo.profile.ProfileComponent.Child.WeightHistory
import com.grippo.profile.equipments.ProfileEquipmentsComponent
import com.grippo.profile.muscles.ProfileMusclesComponent
import com.grippo.profile.weight.history.WeightHistoryComponent

public class ProfileComponent(
    initial: ProfileRouter,
    componentContext: ComponentContext,
    private val back: () -> Unit
) : BaseComponent<ProfileDirection>(componentContext) {

    internal sealed class Child(open val component: BaseComponent<*>) {
        data class Muscles(override val component: ProfileMusclesComponent) : Child(component)
        data class Equipments(override val component: ProfileEquipmentsComponent) : Child(component)
        data class WeightHistory(override val component: WeightHistoryComponent) : Child(component)
    }

    override val viewModel: ProfileViewModel = componentContext.retainedInstance {
        ProfileViewModel()
    }

    private val backCallback = BackCallback(onBack = viewModel::back)

    init {
        backHandler.register(backCallback)
    }

    override suspend fun eventListener(direction: ProfileDirection) {
        when (direction) {
            ProfileDirection.Back -> back.invoke()
        }
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
            ProfileRouter.Muscles -> Muscles(
                ProfileMusclesComponent(
                    componentContext = context,
                    back = back
                ),
            )

            is ProfileRouter.Equipments -> Equipments(
                ProfileEquipmentsComponent(
                    componentContext = context,
                    back = back
                ),
            )

            ProfileRouter.WeightHistory -> WeightHistory(
                WeightHistoryComponent(
                    componentContext = context,
                    back = back
                )
            )
        }
    }

    @Composable
    override fun Render() {
        val state = viewModel.state.collectAsStateMultiplatform()
        val loaders = viewModel.loaders.collectAsStateMultiplatform()
        ProfileScreen(this, state.value, loaders.value, viewModel)
    }
}