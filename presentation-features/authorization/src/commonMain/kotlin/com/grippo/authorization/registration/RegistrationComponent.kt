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
import com.grippo.core.collectAsStateMultiplatform
import com.grippo.presentation.api.auth.RegistrationRouter

internal class RegistrationComponent(
    componentContext: ComponentContext,
    private val toHome: () -> Unit,
    private val back: () -> Unit,
) : BaseComponent<RegistrationDirection>(componentContext) {

    override val viewModel = componentContext.retainedInstance {
        RegistrationViewModel()
    }

    override suspend fun eventListener(direction: RegistrationDirection) {
        when (direction) {
            RegistrationDirection.Back -> navigation::pop
        }
    }

    private val navigation = StackNavigation<RegistrationRouter>()

    private val backCallback = BackCallback(onBack = viewModel::onBack)

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
                    toName = { e, p ->
                        viewModel.saveCredentials(e, p)
                        navigation.push(RegistrationRouter.Name)
                    },
                    back = back
                ),
            )

            RegistrationRouter.Name -> Child.Name(
                NameComponent(
                    componentContext = context,
                    toBody = { n ->
                        viewModel.saveName(n)
                        navigation.push(RegistrationRouter.Body)
                    },
                    back = navigation::pop
                ),
            )

            RegistrationRouter.Body -> Child.Body(
                BodyComponent(
                    componentContext = context,
                    toExperience = { w, h ->
                        viewModel.saveWeightHeight(w, h)
                        navigation.push(RegistrationRouter.Experience)
                    },
                    back = navigation::pop
                ),
            )

            RegistrationRouter.Experience -> Child.Experience(
                ExperienceComponent(
                    componentContext = context,
                    toExcludedMuscles = { e ->
                        viewModel.saveExperience(e)
                        navigation.push(RegistrationRouter.ExcludedMuscles)
                    },
                    back = navigation::pop
                ),
            )

            RegistrationRouter.ExcludedMuscles -> Child.ExcludedMuscles(
                ExcludedMusclesComponent(
                    componentContext = context,
                    toMissingEquipment = { ids ->
                        viewModel.saveExcludedMuscleIds(ids)
                        navigation.push(RegistrationRouter.MissingEquipments)
                    },
                    back = navigation::pop
                ),
            )

            RegistrationRouter.MissingEquipments -> Child.MussingEquipments(
                MissingEquipmentsComponent(
                    componentContext = context,
                    toCompleted = { ids ->
                        viewModel.saveMissingEquipmentIds(ids)
                        navigation.push(RegistrationRouter.Completed)
                    },
                    back = navigation::pop
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
                    toHome = toHome,
                    back = toHome
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