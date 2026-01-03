package com.grippo.authorization.profile.creation.excluded.muscles

import androidx.compose.runtime.Composable
import com.arkivanov.decompose.ComponentContext
import com.arkivanov.essenty.backhandler.BackCallback
import com.arkivanov.essenty.instancekeeper.retainedInstance
import com.grippo.core.foundation.BaseComponent
import com.grippo.core.foundation.platform.collectAsStateMultiplatform

internal class ExcludedMusclesComponent(
    componentContext: ComponentContext,
    private val toMissingEquipment: (excludedMuscleIds: List<String>) -> Unit,
    private val back: () -> Unit,
) : BaseComponent<ExcludedMusclesDirection>(componentContext) {

    override val viewModel = componentContext.retainedInstance {
        ExcludedMusclesViewModel(
            muscleFeature = getKoin().get(),
            colorProvider = getKoin().get(),
        )
    }

    private val backCallback = BackCallback(onBack = viewModel::onBack)

    init {
        backHandler.register(backCallback)
    }

    override suspend fun eventListener(direction: ExcludedMusclesDirection) {
        when (direction) {
            is ExcludedMusclesDirection.MissingEquipment -> toMissingEquipment.invoke(direction.excludedMuscleIds)
            ExcludedMusclesDirection.Back -> back.invoke()
        }
    }

    @Composable
    override fun Render() {
        val state = viewModel.state.collectAsStateMultiplatform()
        val loaders = viewModel.loaders.collectAsStateMultiplatform()
        ExcludedMusclesScreen(state.value, loaders.value, viewModel)
    }
}
