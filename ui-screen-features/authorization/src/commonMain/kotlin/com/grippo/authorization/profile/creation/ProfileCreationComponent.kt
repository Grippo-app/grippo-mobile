package com.grippo.authorization.profile.creation

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
import com.grippo.authorization.profile.creation.completed.CompletedComponent
import com.grippo.authorization.profile.creation.excluded.muscles.ExcludedMusclesComponent
import com.grippo.authorization.profile.creation.experience.ExperienceComponent
import com.grippo.authorization.profile.creation.missing.equipments.MissingEquipmentsComponent
import com.grippo.authorization.profile.creation.user.UserComponent
import com.grippo.core.foundation.BaseComponent
import com.grippo.core.foundation.platform.collectAsStateMultiplatform
import com.grippo.screen.api.ProfileCreationRouter

internal class ProfileCreationComponent(
    componentContext: ComponentContext,
    private val toHome: () -> Unit,
    private val toLogin: () -> Unit,
    private val close: () -> Unit,
) : BaseComponent<ProfileCreationDirection>(componentContext) {

    override val viewModel = componentContext.retainedInstance {
        ProfileCreationViewModel()
    }

    override suspend fun eventListener(direction: ProfileCreationDirection) {
        when (direction) {
            is ProfileCreationDirection.ToBodyWithName -> navigation.push(
                ProfileCreationRouter.User
            )

            is ProfileCreationDirection.ToExperienceWithBody -> navigation.push(
                ProfileCreationRouter.Experience
            )

            is ProfileCreationDirection.ToExcludedMusclesWithExperience -> navigation.push(
                ProfileCreationRouter.ExcludedMuscles
            )

            is ProfileCreationDirection.ToMissingEquipmentWithMuscles -> navigation.push(
                ProfileCreationRouter.MissingEquipments
            )

            is ProfileCreationDirection.ToCompletedWithEquipment -> navigation.push(
                ProfileCreationRouter.Completed
            )

            ProfileCreationDirection.ToHome -> toHome.invoke()
            ProfileCreationDirection.Close -> close.invoke()
            ProfileCreationDirection.Back -> navigation.pop()
            ProfileCreationDirection.ToLogin -> toLogin.invoke()
        }
    }

    private val navigation = StackNavigation<ProfileCreationRouter>()

    private val backCallback = BackCallback(onBack = viewModel::onClose)

    init {
        backHandler.register(backCallback)
    }

    internal val childStack: Value<ChildStack<ProfileCreationRouter, Child>> = childStack(
        source = navigation,
        serializer = ProfileCreationRouter.serializer(),
        initialStack = { listOf(ProfileCreationRouter.User) },
        key = "ProfileCreationComponent",
        handleBackButton = true,
        childFactory = ::createChild,
    )

    private fun createChild(router: ProfileCreationRouter, context: ComponentContext): Child {
        return when (router) {
            ProfileCreationRouter.User -> Child.User(
                UserComponent(
                    componentContext = context,
                    toExperience = viewModel::toExperienceWithUser,
                    back = viewModel::toLogin
                ),
            )

            ProfileCreationRouter.Experience -> Child.Experience(
                ExperienceComponent(
                    componentContext = context,
                    toExcludedMuscles = viewModel::toExcludedMusclesWithExperience,
                    back = viewModel::onBack
                ),
            )

            ProfileCreationRouter.ExcludedMuscles -> Child.ExcludedMuscles(
                ExcludedMusclesComponent(
                    componentContext = context,
                    toMissingEquipment = viewModel::toMissingEquipmentWithMuscles,
                    back = viewModel::onBack
                ),
            )

            ProfileCreationRouter.MissingEquipments -> Child.MussingEquipments(
                MissingEquipmentsComponent(
                    componentContext = context,
                    toCompleted = viewModel::toCompletedWithEquipment,
                    back = viewModel::onBack
                ),
            )

            ProfileCreationRouter.Completed -> Child.Completed(
                CompletedComponent(
                    componentContext = context,
                    name = viewModel.state.value.name,
                    experience = viewModel.state.value.experience,
                    height = viewModel.state.value.height,
                    weight = viewModel.state.value.weight,
                    excludedMuscleIds = viewModel.state.value.excludedMuscleIds,
                    missingEquipmentIds = viewModel.state.value.missingEquipmentIds,
                    toHome = viewModel::toHome,
                    back = viewModel::toHome
                ),
            )
        }
    }

    @Composable
    override fun Render() {
        val state = viewModel.state.collectAsStateMultiplatform()
        val loaders = viewModel.loaders.collectAsStateMultiplatform()
        ProfileCreationScreen(this, state.value, loaders.value, viewModel)
    }

    internal sealed class Child(open val component: BaseComponent<*>) {
        data class User(override val component: UserComponent) :
            Child(component)

        data class Experience(override val component: ExperienceComponent) :
            Child(component)

        data class ExcludedMuscles(override val component: ExcludedMusclesComponent) :
            Child(component)

        data class MussingEquipments(override val component: MissingEquipmentsComponent) :
            Child(component)

        data class Completed(override val component: CompletedComponent) :
            Child(component)
    }
}
