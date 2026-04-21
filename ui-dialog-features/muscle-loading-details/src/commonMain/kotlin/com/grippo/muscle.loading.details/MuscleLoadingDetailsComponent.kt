package com.grippo.muscle.loading.details

import androidx.compose.runtime.Composable
import com.arkivanov.decompose.ComponentContext
import com.arkivanov.essenty.backhandler.BackCallback
import com.arkivanov.essenty.instancekeeper.retainedInstance
import com.grippo.core.foundation.BaseComponent
import com.grippo.core.foundation.platform.collectAsStateMultiplatform
import com.grippo.toolkit.date.utils.DateRange

public class MuscleLoadingDetailsComponent(
    componentContext: ComponentContext,
    range: DateRange,
    private val back: () -> Unit,
) : BaseComponent<MuscleLoadingDetailsDirection>(componentContext) {

    override val viewModel: MuscleLoadingDetailsViewModel = componentContext.retainedInstance {
        MuscleLoadingDetailsViewModel(
            range = range,
            trainingFeature = getKoin().get(),
            muscleLoadingSummaryUseCase = getKoin().get()
        )
    }

    private val backCallback = BackCallback(onBack = viewModel::onBack)

    init {
        backHandler.register(backCallback)
    }

    override suspend fun eventListener(direction: MuscleLoadingDetailsDirection) {
        when (direction) {
            MuscleLoadingDetailsDirection.Back -> back.invoke()
        }
    }

    @Composable
    override fun Render() {
        val state = viewModel.state.collectAsStateMultiplatform()
        val loaders = viewModel.loaders.collectAsStateMultiplatform()
        MuscleLoadingDetailsScreen(state.value, loaders.value, viewModel)
    }
}
