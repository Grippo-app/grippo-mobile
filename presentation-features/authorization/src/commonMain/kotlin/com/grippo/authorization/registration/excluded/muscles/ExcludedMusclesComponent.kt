package com.grippo.authorization.registration.excluded.muscles

import androidx.compose.runtime.Composable
import com.arkivanov.decompose.ComponentContext
import com.arkivanov.essenty.instancekeeper.retainedInstance
import com.grippo.core.BaseComponent
import com.grippo.core.collectAsStateMultiplatform

internal class ExcludedMusclesComponent(
    componentContext: ComponentContext,
    private val toMissingEquipment: () -> Unit
) : BaseComponent<ExcludedMusclesDirection>(componentContext) {

    override val viewModel = componentContext.retainedInstance {
        ExcludedMusclesViewModel()
    }

    override suspend fun eventListener(direction: ExcludedMusclesDirection) {
        when (direction) {
            ExcludedMusclesDirection.MissingEquipment -> toMissingEquipment.invoke()
        }
    }

    @Composable
    override fun Render() {
        val state = viewModel.state.collectAsStateMultiplatform()
        val loaders = viewModel.loaders.collectAsStateMultiplatform()
        ExcludedMusclesScreen(state.value, loaders.value, viewModel)
    }
}