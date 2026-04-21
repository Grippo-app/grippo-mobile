package com.grippo.training.profile.details

import androidx.compose.runtime.Composable
import com.arkivanov.decompose.ComponentContext
import com.arkivanov.essenty.backhandler.BackCallback
import com.arkivanov.essenty.instancekeeper.retainedInstance
import com.grippo.core.foundation.BaseComponent
import com.grippo.core.foundation.platform.collectAsStateMultiplatform
import com.grippo.toolkit.date.utils.DateRange

public class TrainingProfileDetailsComponent(
    componentContext: ComponentContext,
    range: DateRange,
    private val back: () -> Unit,
) : BaseComponent<TrainingProfileDetailsDirection>(componentContext) {

    override val viewModel: TrainingProfileDetailsViewModel = componentContext.retainedInstance {
        TrainingProfileDetailsViewModel(
            range = range,
            trainingFeature = getKoin().get(),
            trainingLoadProfileUseCase = getKoin().get(),
        )
    }

    private val backCallback = BackCallback(onBack = viewModel::onBack)

    init {
        backHandler.register(backCallback)
    }

    override suspend fun eventListener(direction: TrainingProfileDetailsDirection) {
        when (direction) {
            TrainingProfileDetailsDirection.Back -> back.invoke()
        }
    }

    @Composable
    override fun Render() {
        val state = viewModel.state.collectAsStateMultiplatform()
        val loaders = viewModel.loaders.collectAsStateMultiplatform()
        TrainingProfileDetailsScreen(state.value, loaders.value, viewModel)
    }
}
