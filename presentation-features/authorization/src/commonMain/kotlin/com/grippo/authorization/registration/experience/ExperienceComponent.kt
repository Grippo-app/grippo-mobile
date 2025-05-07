package com.grippo.authorization.registration.experience

import androidx.compose.runtime.Composable
import com.arkivanov.decompose.ComponentContext
import com.arkivanov.essenty.instancekeeper.retainedInstance
import com.grippo.core.BaseComponent
import com.grippo.core.collectAsStateMultiplatform
import com.grippo.presentation.api.auth.models.Experience

internal class ExperienceComponent(
    componentContext: ComponentContext,
    private val toExcludedMuscles: (experience: Experience) -> Unit
) : BaseComponent<ExperienceDirection>(componentContext) {

    override val viewModel = componentContext.retainedInstance {
        ExperienceViewModel()
    }

    override suspend fun eventListener(direction: ExperienceDirection) {
        when (direction) {
            is ExperienceDirection.ExcludedMuscles -> toExcludedMuscles.invoke(direction.experience)
        }
    }

    @Composable
    override fun Render() {
        val state = viewModel.state.collectAsStateMultiplatform()
        val loaders = viewModel.loaders.collectAsStateMultiplatform()
        ExperienceScreen(state.value, loaders.value, viewModel)
    }
}