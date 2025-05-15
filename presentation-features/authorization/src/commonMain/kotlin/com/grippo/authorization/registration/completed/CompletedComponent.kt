package com.grippo.authorization.registration.completed

import androidx.compose.runtime.Composable
import com.arkivanov.decompose.ComponentContext
import com.arkivanov.essenty.instancekeeper.retainedInstance
import com.grippo.core.BaseComponent
import com.grippo.core.collectAsStateMultiplatform
import com.grippo.presentation.api.user.models.ExperienceEnumState
import kotlinx.collections.immutable.ImmutableList

internal class CompletedComponent(
    componentContext: ComponentContext,
    val email: String,
    val password: String,
    val name: String,
    val weight: Float,
    val height: Int,
    val experience: ExperienceEnumState?,
    val excludedMuscleIds: ImmutableList<String>,
    val missingEquipmentIds: ImmutableList<String>,
    private val toHome: () -> Unit
) : BaseComponent<CompletedDirection>(componentContext) {

    override val viewModel = componentContext.retainedInstance {
        CompletedViewModel(
            email = email,
            password = password,
            name = name,
            weight = weight,
            height = height,
            experience = experience,
            excludedMuscleIds = excludedMuscleIds,
            missingEquipmentIds = missingEquipmentIds,
            registerUseCase = getKoin().get()
        )
    }

    override suspend fun eventListener(direction: CompletedDirection) {
        when (direction) {
            CompletedDirection.Home -> toHome.invoke()
        }
    }

    @Composable
    override fun Render() {
        val state = viewModel.state.collectAsStateMultiplatform()
        val loaders = viewModel.loaders.collectAsStateMultiplatform()
        CompletedScreen(state.value, loaders.value, viewModel)
    }
}