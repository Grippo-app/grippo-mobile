package com.grippo.start.training

import androidx.compose.runtime.Composable
import com.arkivanov.decompose.ComponentContext
import com.arkivanov.essenty.backhandler.BackCallback
import com.arkivanov.essenty.instancekeeper.retainedInstance
import com.grippo.core.foundation.BaseComponent
import com.grippo.core.foundation.platform.collectAsStateMultiplatform
import com.grippo.core.state.trainings.ExerciseState
import kotlinx.collections.immutable.ImmutableList

public class StartTrainingComponent(
    componentContext: ComponentContext,
    private val onStartEmpty: () -> Unit,
    private val onUseExercises: (exercises: ImmutableList<ExerciseState>) -> Unit,
    private val back: () -> Unit,
) : BaseComponent<StartTrainingDirection>(componentContext) {

    override val viewModel: StartTrainingViewModel = componentContext.retainedInstance {
        StartTrainingViewModel(
            trainingFeature = getKoin().get(),
            generatePresetTrainingUseCase = getKoin().get(),
        )
    }

    private val backCallback = BackCallback(onBack = viewModel::onBack)

    init {
        backHandler.register(backCallback)
    }

    override suspend fun eventListener(direction: StartTrainingDirection) {
        when (direction) {
            StartTrainingDirection.StartEmpty -> onStartEmpty.invoke()
            is StartTrainingDirection.UseExercises -> onUseExercises.invoke(direction.exercises)
            StartTrainingDirection.Back -> back.invoke()
        }
    }

    @Composable
    override fun Render() {
        val state = viewModel.state.collectAsStateMultiplatform()
        val loaders = viewModel.loaders.collectAsStateMultiplatform()
        StartTrainingScreen(state.value, loaders.value, viewModel)
    }
}
