package com.grippo.muscle.loading

import androidx.compose.runtime.Composable
import com.arkivanov.decompose.ComponentContext
import com.arkivanov.essenty.backhandler.BackCallback
import com.arkivanov.essenty.instancekeeper.retainedInstance
import com.grippo.core.foundation.BaseComponent
import com.grippo.core.foundation.platform.collectAsStateMultiplatform
import com.grippo.toolkit.date.utils.DateRange

public class MuscleLoadingComponent(
    componentContext: ComponentContext,
    range: DateRange,
    private val back: () -> Unit,
) : BaseComponent<MuscleLoadingDirection>(componentContext) {

    override val viewModel: MuscleLoadingViewModel = componentContext.retainedInstance {
        MuscleLoadingViewModel(
            range = range,
            trainingFeature = getKoin().get(),
            muscleLoadingSummaryUseCase = getKoin().get()
        )
    }

    private val backCallback = BackCallback(onBack = viewModel::onBack)

    init {
        backHandler.register(backCallback)
    }

    override suspend fun eventListener(direction: MuscleLoadingDirection) {
        when (direction) {
            MuscleLoadingDirection.Back -> back.invoke()
        }
    }

    @Composable
    override fun Render() {
        val state = viewModel.state.collectAsStateMultiplatform()
        val loaders = viewModel.loaders.collectAsStateMultiplatform()
        MuscleLoadingScreen(state.value, loaders.value, viewModel)
    }
}
