package com.grippo.training.preferences

import androidx.compose.runtime.Composable
import com.arkivanov.decompose.ComponentContext
import com.arkivanov.essenty.backhandler.BackCallback
import com.arkivanov.essenty.instancekeeper.retainedInstance
import com.grippo.core.BaseComponent
import com.grippo.core.platform.collectAsStateMultiplatform

internal class TrainingPreferencesComponent(
    componentContext: ComponentContext,
    private val toRecording: (selectedMuscleIds: List<String>) -> Unit,
    private val back: () -> Unit,
) : BaseComponent<TrainingPreferencesDirection>(componentContext) {

    override val viewModel: TrainingPreferencesViewModel = componentContext.retainedInstance {
        TrainingPreferencesViewModel(
            muscleFeature = getKoin().get(),
        )
    }

    private val backCallback = BackCallback(onBack = viewModel::onBack)

    init {
        backHandler.register(backCallback)
    }

    override suspend fun eventListener(direction: TrainingPreferencesDirection) {
        when (direction) {
            is TrainingPreferencesDirection.ToRecording -> toRecording.invoke(direction.selectedMuscleIds)
            TrainingPreferencesDirection.Back -> back.invoke()
        }
    }

    @Composable
    override fun Render() {
        val state = viewModel.state.collectAsStateMultiplatform()
        val loaders = viewModel.loaders.collectAsStateMultiplatform()
        TrainingPreferencesScreen(state.value, loaders.value, viewModel)
    }
}
