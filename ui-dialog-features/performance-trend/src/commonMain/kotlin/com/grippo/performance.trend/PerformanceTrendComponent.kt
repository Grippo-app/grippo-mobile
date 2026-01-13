package com.grippo.performance.trend

import androidx.compose.runtime.Composable
import com.arkivanov.decompose.ComponentContext
import com.arkivanov.essenty.backhandler.BackCallback
import com.arkivanov.essenty.instancekeeper.retainedInstance
import com.grippo.core.foundation.BaseComponent
import com.grippo.core.foundation.platform.collectAsStateMultiplatform
import com.grippo.core.state.metrics.PerformanceMetricTypeState
import com.grippo.toolkit.date.utils.DateRange

public class PerformanceTrendComponent(
    componentContext: ComponentContext,
    range: DateRange,
    metricType: PerformanceMetricTypeState,
    private val back: () -> Unit,
) : BaseComponent<PerformanceTrendDirection>(componentContext) {

    override val viewModel: PerformanceTrendViewModel = componentContext.retainedInstance {
        PerformanceTrendViewModel(
            range = range,
            metricType = metricType,
            trainingFeature = getKoin().get(),
            performanceTrendUseCase = getKoin().get()
        )
    }

    private val backCallback = BackCallback(onBack = viewModel::onBack)

    init {
        backHandler.register(backCallback)
    }

    override suspend fun eventListener(direction: PerformanceTrendDirection) {
        when (direction) {
            PerformanceTrendDirection.Back -> back.invoke()
        }
    }

    @Composable
    override fun Render() {
        val state = viewModel.state.collectAsStateMultiplatform()
        val loaders = viewModel.loaders.collectAsStateMultiplatform()
        PerformanceTrendScreen(state.value, loaders.value, viewModel)
    }
}
