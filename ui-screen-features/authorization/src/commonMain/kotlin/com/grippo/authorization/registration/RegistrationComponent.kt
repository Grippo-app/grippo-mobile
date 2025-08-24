package com.grippo.authorization.registration

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
import com.grippo.authorization.registration.body.BodyComponent
import com.grippo.authorization.registration.completed.CompletedComponent
import com.grippo.authorization.registration.credential.CredentialComponent
import com.grippo.authorization.registration.excluded.muscles.ExcludedMusclesComponent
import com.grippo.authorization.registration.experience.ExperienceComponent
import com.grippo.authorization.registration.missing.equipments.MissingEquipmentsComponent
import com.grippo.authorization.registration.name.NameComponent
import com.grippo.core.BaseComponent
import com.grippo.core.platform.collectAsStateMultiplatform
import com.grippo.screen.api.RegistrationRouter

internal class RegistrationComponent(
    componentContext: ComponentContext,
    private val toHome: () -> Unit,
    private val close: () -> Unit,
) : BaseComponent<RegistrationDirection>(componentContext) {

    override val viewModel = componentContext.retainedInstance {
        RegistrationViewModel()
    }

    override suspend fun eventListener(direction: RegistrationDirection) {
        when (direction) {
            is RegistrationDirection.ToNameWithCredentials -> navigation.push(
                RegistrationRouter.Name
            )

            is RegistrationDirection.ToBodyWithName -> navigation.push(
                RegistrationRouter.Body
            )

            is RegistrationDirection.ToExperienceWithBody -> navigation.push(
                RegistrationRouter.Experience
            )

            is RegistrationDirection.ToExcludedMusclesWithExperience -> navigation.push(
                RegistrationRouter.ExcludedMuscles
            )

            is RegistrationDirection.ToMissingEquipmentWithMuscles -> navigation.push(
                RegistrationRouter.MissingEquipments
            )

            is RegistrationDirection.ToCompletedWithEquipment -> navigation.push(
                RegistrationRouter.Completed
            )

            RegistrationDirection.ToHome -> toHome.invoke()
            RegistrationDirection.Close -> close.invoke()
            RegistrationDirection.Back -> navigation.pop()
        }
    }

    private val navigation = StackNavigation<RegistrationRouter>()

    private val backCallback = BackCallback(onBack = viewModel::onClose)

    init {
        backHandler.register(backCallback)
    }

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
            RegistrationRouter.Credentials -> Child.Credential(
                CredentialComponent(
                    componentContext = context,
                    toName = viewModel::toNameWithCredentials,
                    back = viewModel::onClose
                ),
            )

            RegistrationRouter.Name -> Child.Name(
                NameComponent(
                    componentContext = context,
                    toBody = viewModel::toBodyWithName,
                    back = viewModel::onBack
                ),
            )

            RegistrationRouter.Body -> Child.Body(
                BodyComponent(
                    componentContext = context,
                    toExperience = viewModel::toExperienceWithBody,
                    back = viewModel::onBack
                ),
            )

            RegistrationRouter.Experience -> Child.Experience(
                ExperienceComponent(
                    componentContext = context,
                    toExcludedMuscles = viewModel::toExcludedMusclesWithExperience,
                    back = viewModel::onBack
                ),
            )

            RegistrationRouter.ExcludedMuscles -> Child.ExcludedMuscles(
                ExcludedMusclesComponent(
                    componentContext = context,
                    toMissingEquipment = viewModel::toMissingEquipmentWithMuscles,
                    back = viewModel::onBack
                ),
            )

            RegistrationRouter.MissingEquipments -> Child.MussingEquipments(
                MissingEquipmentsComponent(
                    componentContext = context,
                    toCompleted = viewModel::toCompletedWithEquipment,
                    back = viewModel::onBack
                ),
            )

            RegistrationRouter.Completed -> Child.Completed(
                CompletedComponent(
                    componentContext = context,
                    email = viewModel.state.value.email,
                    password = viewModel.state.value.password,
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
        RegistrationScreen(this, state.value, loaders.value, viewModel)
    }

    internal sealed class Child(open val component: BaseComponent<*>) {
        data class Credential(override val component: CredentialComponent) :
            Child(component)

        data class Name(override val component: NameComponent) :
            Child(component)

        data class Body(override val component: BodyComponent) :
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