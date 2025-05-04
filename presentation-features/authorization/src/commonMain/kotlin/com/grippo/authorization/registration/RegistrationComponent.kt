package com.grippo.authorization.registration

import androidx.compose.runtime.Composable
import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.router.stack.ChildStack
import com.arkivanov.decompose.router.stack.StackNavigation
import com.arkivanov.decompose.router.stack.childStack
import com.arkivanov.decompose.router.stack.push
import com.arkivanov.decompose.value.Value
import com.arkivanov.essenty.instancekeeper.retainedInstance
import com.grippo.authorization.registration.body.BodyComponent
import com.grippo.authorization.registration.completed.CompletedComponent
import com.grippo.authorization.registration.credential.CredentialComponent
import com.grippo.authorization.registration.excluded.muscles.ExcludedMusclesComponent
import com.grippo.authorization.registration.experience.ExperienceComponent
import com.grippo.authorization.registration.missing.equipment.MissingEquipmentComponent
import com.grippo.authorization.registration.name.NameComponent
import com.grippo.core.BaseComponent
import com.grippo.core.collectAsStateMultiplatform
import com.grippo.presentation.api.auth.RegistrationRouter

internal class RegistrationComponent(
    componentContext: ComponentContext,
) : BaseComponent<RegistrationDirection>(componentContext) {

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

        data class MussingEquipment(override val component: MissingEquipmentComponent) :
            Child(component)

        data class Completed(override val component: CompletedComponent) :
            Child(component)
    }

    override val viewModel = componentContext.retainedInstance {
        RegistrationViewModel()
    }

    override suspend fun eventListener(direction: RegistrationDirection) {
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
            RegistrationRouter.Credentials -> Child.Credential(
                CredentialComponent(
                    componentContext = context,
                    toName = { e, p ->
                        navigation.push(RegistrationRouter.Name)
                        viewModel.saveCredentials(e, p)
                    }
                ),
            )

            RegistrationRouter.Name -> Child.Name(
                NameComponent(
                    componentContext = context,
                    toBody = { n ->
                        navigation.push(RegistrationRouter.Body)
                        viewModel.saveName(n)
                    }
                ),
            )

            RegistrationRouter.Body -> Child.Body(
                BodyComponent(
                    componentContext = context,
                    toExperience = { navigation.push(RegistrationRouter.Experience) }
                ),
            )

            RegistrationRouter.Experience -> Child.Experience(
                ExperienceComponent(
                    componentContext = context,
                    toExcludedMuscles = { navigation.push(RegistrationRouter.ExcludedMuscles) }
                ),
            )

            RegistrationRouter.ExcludedMuscles -> Child.ExcludedMuscles(
                ExcludedMusclesComponent(
                    componentContext = context,
                    toMissingEquipment = { navigation.push(RegistrationRouter.MissingEquipment) }
                ),
            )

            RegistrationRouter.MissingEquipment -> Child.MussingEquipment(
                MissingEquipmentComponent(
                    componentContext = context,
                    toCompleted = { navigation.push(RegistrationRouter.Completed) }
                ),
            )

            RegistrationRouter.Completed -> Child.Completed(
                CompletedComponent(
                    componentContext = context,
                    toHome = TODO()
                ),
            )
        }
    }

    @Composable
    override fun Render() {
        val state = viewModel.state.collectAsStateMultiplatform()
        val loaders = viewModel.loaders.collectAsStateMultiplatform()
        RegistrationScreen(childStack, state.value, loaders.value, viewModel)
    }
}