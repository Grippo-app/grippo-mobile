package com.grippo.authorization.registration.experience

import androidx.compose.runtime.Composable
import com.arkivanov.decompose.ComponentContext
import com.arkivanov.essenty.instancekeeper.retainedInstance
import com.grippo.core.BaseComponent
import com.grippo.core.collectAsStateMultiplatform

internal class ExperienceComponent(
    componentContext: ComponentContext,
    private val toExcludedMuscles: () -> Unit
) : BaseComponent<ExperienceDirection>(componentContext) {

    override val viewModel = componentContext.retainedInstance {
        ExperienceViewModel()
    }

    override suspend fun eventListener(direction: ExperienceDirection) {
        when (direction) {
            ExperienceDirection.ExcludedMuscles -> toExcludedMuscles.invoke()
        }
    }

    @Composable
    override fun Render() {
        val state = viewModel.state.collectAsStateMultiplatform()
        val loaders = viewModel.loaders.collectAsStateMultiplatform()
        ExperienceScreen(state.value, loaders.value, viewModel)
    }
}