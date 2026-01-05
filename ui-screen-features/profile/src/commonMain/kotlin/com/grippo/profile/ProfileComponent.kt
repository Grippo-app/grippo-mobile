package com.grippo.profile

import androidx.compose.runtime.Composable
import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.router.stack.ChildStack
import com.arkivanov.decompose.router.stack.StackNavigation
import com.arkivanov.decompose.router.stack.childStack
import com.arkivanov.decompose.value.Value
import com.arkivanov.essenty.backhandler.BackCallback
import com.arkivanov.essenty.instancekeeper.retainedInstance
import com.grippo.core.foundation.BaseComponent
import com.grippo.core.foundation.platform.collectAsStateMultiplatform
import com.grippo.profile.ProfileComponent.Child.Equipments
import com.grippo.profile.ProfileComponent.Child.Experience
import com.grippo.profile.ProfileComponent.Child.Muscles
import com.grippo.profile.ProfileComponent.Child.WeightHistory
import com.grippo.profile.equipments.ProfileEquipmentsComponent
import com.grippo.profile.experience.ProfileExperienceComponent
import com.grippo.profile.muscles.ProfileMusclesComponent
import com.grippo.profile.weight.history.WeightHistoryComponent
import com.grippo.screen.api.ProfileRouter

public class ProfileComponent(
    initial: ProfileRouter,
    componentContext: ComponentContext,
    private val close: () -> Unit
) : BaseComponent<ProfileDirection>(componentContext) {

    override val viewModel: ProfileViewModel = componentContext.retainedInstance {
        ProfileViewModel()
    }

    private val backCallback = BackCallback(onBack = viewModel::onBack)

    init {
        backHandler.register(backCallback)
    }

    override suspend fun eventListener(direction: ProfileDirection) {
        when (direction) {
            ProfileDirection.Back -> close.invoke()
        }
    }

    private val navigation = StackNavigation<ProfileRouter>()

    internal val childStack: Value<ChildStack<ProfileRouter, Child>> = childStack(
        source = navigation,
        serializer = ProfileRouter.serializer(),
        initialStack = { listOf(initial) },
        key = "ProfileComponent",
        handleBackButton = true,
        childFactory = ::createChild,
    )

    private fun createChild(router: ProfileRouter, context: ComponentContext): Child {
        return when (router) {
            ProfileRouter.Muscles -> Muscles(
                ProfileMusclesComponent(
                    componentContext = context,
                    back = viewModel::onBack
                ),
            )

            is ProfileRouter.Equipments -> Equipments(
                ProfileEquipmentsComponent(
                    componentContext = context,
                    back = viewModel::onBack
                ),
            )

            ProfileRouter.WeightHistory -> WeightHistory(
                WeightHistoryComponent(
                    componentContext = context,
                    back = viewModel::onBack
                )
            )

            ProfileRouter.Experience -> Experience(
                ProfileExperienceComponent(
                    componentContext = context,
                    back = viewModel::onBack
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

    internal sealed class Child(open val component: BaseComponent<*>) {
        data class Muscles(override val component: ProfileMusclesComponent) : Child(component)
        data class Equipments(override val component: ProfileEquipmentsComponent) : Child(component)
        data class WeightHistory(override val component: WeightHistoryComponent) : Child(component)
        data class Experience(override val component: ProfileExperienceComponent) : Child(component)
    }
}
