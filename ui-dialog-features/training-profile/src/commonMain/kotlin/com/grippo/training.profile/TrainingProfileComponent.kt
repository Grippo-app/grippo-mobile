package com.grippo.training.profile

import androidx.compose.runtime.Composable
import com.arkivanov.decompose.ComponentContext
import com.arkivanov.essenty.backhandler.BackCallback
import com.arkivanov.essenty.instancekeeper.retainedInstance
import com.grippo.core.foundation.BaseComponent
import com.grippo.core.foundation.platform.collectAsStateMultiplatform
import com.grippo.toolkit.date.utils.DateRange

public class TrainingProfileComponent(
    componentContext: ComponentContext,
    range: DateRange,
    private val back: () -> Unit,
) : BaseComponent<TrainingProfileDirection>(componentContext) {

    override val viewModel: TrainingProfileViewModel = componentContext.retainedInstance {
        TrainingProfileViewModel(
            range = range,
            trainingFeature = getKoin().get(),
            trainingLoadProfileUseCase = getKoin().get(),
        )
    }

    private val backCallback = BackCallback(onBack = viewModel::onBack)

    init {
        backHandler.register(backCallback)
    }

    override suspend fun eventListener(direction: TrainingProfileDirection) {
        when (direction) {
            TrainingProfileDirection.Back -> back.invoke()
        }
    }

    @Composable
    override fun Render() {
        val state = viewModel.state.collectAsStateMultiplatform()
        val loaders = viewModel.loaders.collectAsStateMultiplatform()
        TrainingProfileScreen(state.value, loaders.value, viewModel)
    }
}
